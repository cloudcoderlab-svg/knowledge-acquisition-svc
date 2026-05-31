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

  @Value("${knowledge-engine.project-pipeline.poll-ms:1000}")
  private long pollMs;

  public ProjectPipelineService(
      ProjectService projectService,
      ProcessingService processingService,
      ProcessTrackingRepository processRepository,
      @Qualifier("projectPipelineExecutor") Executor projectPipelineExecutor) {
    this.projectService = projectService;
    this.processingService = processingService;
    this.processRepository = processRepository;
    this.projectPipelineExecutor = projectPipelineExecutor;
  }

  public ProcessResponse startProjectPipeline(UUID projectId) {
    ProjectEntity project = projectService.find(projectId);
    log.info("Starting project pipeline for project {} ({})", project.getProjectName(), projectId);
    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("PROJECT_PIPELINE")
                .status("RUNNING")
                .totalFiles(4)
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
          waitForTerminal(processingService.startIngestion(projectId).processId());
      if (!isUsable(ingestion)) {
        failPipeline(processId, "ingestion", ingestion, completedStages, failedStages + 1);
        return;
      }
      completedStages++;
      failedStages += "PARTIAL_SUCCESS".equals(ingestion.getStatus()) ? 1 : 0;
      updateProgress(processId, completedStages, failedStages, "consolidation");

      ProcessResponse consolidation = processingService.startConsolidation(projectId);
      if (!isUsable(consolidation)) {
        failPipeline(processId, "consolidation", consolidation, completedStages, failedStages + 1);
        return;
      }
      completedStages++;
      updateProgress(processId, completedStages, failedStages, "planning");

      ProcessResponse planning = processingService.startPlanning(projectId);
      if (!isUsable(planning)) {
        failPipeline(processId, "planning", planning, completedStages, failedStages + 1);
        return;
      }
      completedStages++;
      updateProgress(processId, completedStages, failedStages, "project-summary");

      ProcessResponse projectSummary = processingService.startProjectSummary(projectId);
      if (!isUsable(projectSummary)) {
        failPipeline(
            processId, "project-summary", projectSummary, completedStages, failedStages + 1);
        return;
      }
      completedStages++;

      int finalCompletedStages = completedStages;
      int finalFailedStages = failedStages;
      updateProcess(
          processId,
          process -> {
            process.setProcessedFiles(finalCompletedStages);
            process.setFailedFiles(finalFailedStages);
            process.setCurrentFile(null);
            process.setStatus(finalFailedStages == 0 ? "COMPLETED" : "PARTIAL_SUCCESS");
            process.setCompletedAt(OffsetDateTime.now());
          });
    } catch (Exception e) {
      log.error("Project pipeline failed for project {}", projectId, e);
      int finalCompletedStages = completedStages;
      int finalFailedStages = Math.max(1, failedStages);
      updateProcess(
          processId,
          process -> {
            process.setProcessedFiles(finalCompletedStages);
            process.setFailedFiles(finalFailedStages);
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
