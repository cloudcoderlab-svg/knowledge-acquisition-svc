package com.knowledge.acquisition.service;

import com.knowledge.acquisition.dto.ProcessResponse;
import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import com.knowledge.acquisition.entity.ProjectEntity;
import com.knowledge.acquisition.repository.ProcessTrackingRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectPipelineService {
  private static final Set<String> TERMINAL_STATUSES =
      Set.of("COMPLETED", "PARTIAL_SUCCESS", "FAILED");

  private final ProjectService projectService;
  private final ProcessingService processingService;
  private final ProcessTrackingRepository processRepository;
  private final Executor projectPipelineExecutor;
  private final GcsProjectFileService gcsProjectFileService;

  @Value("${knowledge-engine.project-pipeline.poll-ms:1000}")
  private long pollMs;

  public ProjectPipelineService(
      ProjectService projectService,
      ProcessingService processingService,
      ProcessTrackingRepository processRepository,
      @Qualifier("projectPipelineExecutor") Executor projectPipelineExecutor,
      GcsProjectFileService gcsProjectFileService) {
    this.projectService = projectService;
    this.processingService = processingService;
    this.processRepository = processRepository;
    this.projectPipelineExecutor = projectPipelineExecutor;
    this.gcsProjectFileService = gcsProjectFileService;
  }

  public ProcessResponse startProjectPipeline(UUID projectId) {
    ProjectEntity project = projectService.find(projectId);
    log.info("Starting project pipeline for project {} ({})", project.getProjectName(), projectId);

    // Get processable files (exclude directories and definition.md)
    List<String> allFiles = gcsProjectFileService.listFiles(projectId);
    long processableFileCount =
        allFiles.stream()
            .filter(file -> !file.endsWith("/"))
            .filter(file -> !file.endsWith("definition.md"))
            .count();

    log.info(
        "Found {} total files, {} processable files in project {}",
        allFiles.size(),
        processableFileCount,
        projectId);

    // Validate that there are processable files before starting pipeline
    if (processableFileCount == 0) {
      log.warn(
          "No processable files found for project {} ({}). Only definition.md or empty folder detected.",
          project.getProjectName(),
          projectId);
      ProcessTrackingEntity failedProcess =
          processRepository.save(
              ProcessTrackingEntity.builder()
                  .projectId(projectId)
                  .processType("PROJECT_PIPELINE")
                  .status("FAILED")
                  .totalFiles(0)
                  .processedFiles(0)
                  .failedFiles(0)
                  .currentFile(null)
                  .startedAt(OffsetDateTime.now())
                  .completedAt(OffsetDateTime.now())
                  .failureCause(
                      "No processable files found in project. Only definition.md or empty folder detected. "
                          + "Please upload documents (PDF, DOCX, XML, etc.) to start the pipeline.")
                  .build());
      return toResponse(failedProcess);
    }

    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("PROJECT_PIPELINE")
                .status("RUNNING")
                .totalFiles((int) processableFileCount)
                .processedFiles(0)
                .failedFiles(0)
                .currentFile("queued")
                .startedAt(OffsetDateTime.now())
                .build());

    projectPipelineExecutor.execute(() -> runPipeline(process.getProcessId(), projectId));
    return toResponse(process);
  }

  public List<ProcessResponse> startProjectPipelines(List<UUID> projectIds) {
    return new LinkedHashSet<>(projectIds).stream().map(this::startProjectPipeline).toList();
  }

  private void runPipeline(UUID processId, UUID projectId) {
    int completedStages = 0;
    int failedStages = 0;
    try {
      updateProcess(processId, process -> process.setCurrentFile("ingestion"));
      ProcessTrackingEntity ingestion =
          waitForTerminalAndSyncProgress(
              processId, processingService.startIngestion(projectId).processId());
      if (!isUsable(ingestion)) {
        failPipeline(
            processId, "ingestion", ingestion, ingestion.getProcessedFiles(), failedStages + 1);
        return;
      }
      completedStages++;
      failedStages += "PARTIAL_SUCCESS".equals(ingestion.getStatus()) ? 1 : 0;
      updateProgress(processId, ingestion.getProcessedFiles(), failedStages, "consolidation");

      ProcessResponse consolidation = processingService.startConsolidation(projectId);
      if (!isUsable(consolidation)) {
        failPipeline(
            processId,
            "consolidation",
            consolidation,
            ingestion.getProcessedFiles(),
            failedStages + 1);
        return;
      }
      completedStages++;
      updateProgress(processId, ingestion.getProcessedFiles(), failedStages, "planning");

      ProcessResponse planning = processingService.startPlanning(projectId);
      if (!isUsable(planning)) {
        failPipeline(
            processId, "planning", planning, ingestion.getProcessedFiles(), failedStages + 1);
        return;
      }
      completedStages++;
      updateProgress(processId, ingestion.getProcessedFiles(), failedStages, "project-summary");

      ProcessResponse projectSummary = processingService.startProjectSummary(projectId);
      if (!isUsable(projectSummary)) {
        failPipeline(
            processId,
            "project-summary",
            projectSummary,
            ingestion.getProcessedFiles(),
            failedStages + 1);
        return;
      }
      completedStages++;

      int finalProcessedFiles = ingestion.getProcessedFiles();
      int finalFailedFiles = ingestion.getFailedFiles() + failedStages;
      String finalStatus = finalFailedFiles == 0 ? "COMPLETED" : "PARTIAL_SUCCESS";
      updateProcess(
          processId,
          process -> {
            process.setProcessedFiles(finalProcessedFiles);
            process.setFailedFiles(finalFailedFiles);
            process.setCurrentFile(null);
            process.setStatus(finalStatus);
            process.setCompletedAt(OffsetDateTime.now());
          });

      // Mark project as ACTIVE after ALL pipeline phases complete successfully
      // This includes: ingestion → consolidation → planning → project-summary
      try {
        projectService.onPipelineSuccess(projectId, finalStatus);
      } catch (Exception e) {
        log.error("Failed to activate project after pipeline completion: {}", projectId, e);
      }
    } catch (Exception e) {
      log.error("Project pipeline failed for project {}", projectId, e);
      int finalFailedStages = Math.max(1, failedStages);
      updateProcess(
          processId,
          process -> {
            process.setFailedFiles(process.getFailedFiles() + finalFailedStages);
            process.setStatus("FAILED");
            process.setFailureCause(shortMessage(e));
            process.setCompletedAt(OffsetDateTime.now());
          });
    }
  }

  private ProcessTrackingEntity waitForTerminal(UUID childProcessId) throws InterruptedException {
    while (true) {
      ProcessTrackingEntity process =
          processRepository
              .findById(childProcessId)
              .orElseThrow(() -> new IllegalStateException("Process not found: " + childProcessId));
      if (TERMINAL_STATUSES.contains(process.getStatus())) {
        return process;
      }
      Thread.sleep(Math.max(100, pollMs));
    }
  }

  private ProcessTrackingEntity waitForTerminalAndSyncProgress(
      UUID pipelineProcessId, UUID childProcessId) throws InterruptedException {
    while (true) {
      ProcessTrackingEntity childProcess =
          processRepository
              .findById(childProcessId)
              .orElseThrow(() -> new IllegalStateException("Process not found: " + childProcessId));

      // Sync progress from child process to pipeline
      updateProcess(
          pipelineProcessId,
          pipelineProcess -> {
            pipelineProcess.setProcessedFiles(childProcess.getProcessedFiles());
            pipelineProcess.setFailedFiles(childProcess.getFailedFiles());
          });

      if (TERMINAL_STATUSES.contains(childProcess.getStatus())) {
        return childProcess;
      }
      Thread.sleep(Math.max(100, pollMs));
    }
  }

  private void updateProgress(UUID processId, int completedStages, int failedStages, String stage) {
    updateProcess(
        processId,
        process -> {
          process.setProcessedFiles(completedStages);
          process.setFailedFiles(failedStages);
          process.setCurrentFile(stage);
        });
  }

  private void failPipeline(
      UUID processId,
      String stage,
      ProcessTrackingEntity childProcess,
      int completedStages,
      int failedStages) {
    failPipeline(
        processId,
        stage,
        childProcess.getProcessId(),
        childProcess.getStatus(),
        childProcess.getFailureCause(),
        completedStages,
        failedStages);
  }

  private void failPipeline(
      UUID processId,
      String stage,
      ProcessResponse childProcess,
      int completedStages,
      int failedStages) {
    failPipeline(
        processId,
        stage,
        childProcess.processId(),
        childProcess.status(),
        childProcess.failureCause(),
        completedStages,
        failedStages);
  }

  private void failPipeline(
      UUID processId,
      String stage,
      UUID childProcessId,
      String childStatus,
      String childFailureCause,
      int completedStages,
      int failedStages) {
    updateProcess(
        processId,
        process -> {
          process.setProcessedFiles(completedStages);
          process.setFailedFiles(failedStages);
          process.setCurrentFile(stage);
          process.setStatus("FAILED");
          process.setFailureCause(
              stage
                  + " process "
                  + childProcessId
                  + " ended with "
                  + childStatus
                  + failureSuffix(childFailureCause));
          process.setCompletedAt(OffsetDateTime.now());
        });
  }

  private boolean isUsable(ProcessTrackingEntity process) {
    return "COMPLETED".equals(process.getStatus()) || "PARTIAL_SUCCESS".equals(process.getStatus());
  }

  private boolean isUsable(ProcessResponse process) {
    return "COMPLETED".equals(process.status()) || "PARTIAL_SUCCESS".equals(process.status());
  }

  private synchronized void updateProcess(UUID processId, Consumer<ProcessTrackingEntity> update) {
    processRepository
        .findById(processId)
        .ifPresent(
            process -> {
              update.accept(process);
              processRepository.save(process);
            });
  }

  private ProcessResponse toResponse(ProcessTrackingEntity process) {
    return new ProcessResponse(
        process.getProcessId(),
        process.getProjectId(),
        process.getProcessType(),
        process.getStatus(),
        process.getTotalFiles(),
        process.getProcessedFiles(),
        process.getFailedFiles(),
        process.getCurrentFile(),
        process.getFailureCause(),
        process.getTotalTokensProcessed(),
        process.getTotalBytesProcessed(),
        process.getStartedAt(),
        process.getCompletedAt(),
        process.getCreatedAt(),
        process.getUpdatedAt());
  }

  private String failureSuffix(String failureCause) {
    return failureCause == null || failureCause.isBlank() ? "" : ": " + failureCause;
  }

  private String shortMessage(Exception e) {
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      message = e.getClass().getSimpleName();
    }
    return message.length() <= 2000 ? message : message.substring(0, 2000);
  }
}
