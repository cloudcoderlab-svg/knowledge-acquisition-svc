package com.knowledge.acquisition.service;

import com.knowledge.acquisition.dto.ProcessResponse;
import com.knowledge.acquisition.dto.ProcessingSummaryResponse;
import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import com.knowledge.acquisition.ingestion.service.CrossDocumentRelationshipService;
import com.knowledge.acquisition.ingestion.service.KnowledgeChunkGenerationService;
import com.knowledge.acquisition.ingestion.service.PlanningGenerationService;
import com.knowledge.acquisition.ingestion.service.ProjectKnowledgeResetService;
import com.knowledge.acquisition.repository.ProcessTrackingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessingService {
  private final ProjectService projectService;
  private final ProcessTrackingRepository processRepository;
  private final GcsProjectFileService fileService;
  private final IngestionProcessRunner ingestionProcessRunner;
  private final CrossDocumentRelationshipService crossDocumentRelationshipService;
  private final KnowledgeChunkGenerationService knowledgeChunkGenerationService;
  private final PlanningGenerationService planningGenerationService;
  private final ProjectKnowledgeResetService projectKnowledgeResetService;
  private final ProjectSummaryService projectSummaryService;

  @Value("${gcp.storage.bucket-name}")
  private String defaultBucket;

  public ProcessResponse startIngestion(UUID projectId) {
    var project = projectService.find(projectId);
    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("PROJECT_INGESTION")
                .status("RUNNING")
                .totalFiles(0)
                .processedFiles(0)
                .failedFiles(0)
                .startedAt(OffsetDateTime.now())
                .build());
    List<String> files =
        fileService.listFiles(projectId).stream()
            .filter(file -> !file.endsWith("/"))
            .filter(file -> !file.endsWith("definition.md"))
            .toList();
    process.setTotalFiles(files.size());
    process.setFileList(toJsonArray(files));
    processRepository.save(process);

    ingestionProcessRunner.runIngestion(
        process.getProcessId(),
        projectId,
        project.getSourceBucket() == null ? defaultBucket : project.getSourceBucket(),
        files);
    return toResponse(process);
  }

  public ProcessResponse startConsolidation(UUID projectId) {
    var project = projectService.find(projectId);
    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("CROSS_DOCUMENT_CONSOLIDATION")
                .status("RUNNING")
                .startedAt(OffsetDateTime.now())
                .build());
    try {
      crossDocumentRelationshipService.analyzeAndInferRelationships(
          projectId, project.getProjectName());
      int knowledgeChunks = knowledgeChunkGenerationService.refreshKnowledgeChunks(projectId);
      process.setProcessedFiles(knowledgeChunks);
      process.setStatus("COMPLETED");
    } catch (Exception e) {
      process.setStatus("FAILED");
      process.setFailureCause(e.getMessage());
    }
    process.setCompletedAt(OffsetDateTime.now());
    processRepository.save(process);
    return toResponse(process);
  }

  public ProcessResponse startPlanning(UUID projectId) {
    projectService.find(projectId);
    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("PLANNING_GENERATION")
                .status("RUNNING")
                .startedAt(OffsetDateTime.now())
                .build());
    try {
      projectKnowledgeResetService.resetPlanningData(projectId);
      int generated = planningGenerationService.generate(projectId);
      process.setProcessedFiles(generated);
      process.setStatus("COMPLETED");
    } catch (Exception e) {
      process.setStatus("FAILED");
      process.setFailureCause(e.getMessage());
    }
    process.setCompletedAt(OffsetDateTime.now());
    processRepository.save(process);
    return toResponse(process);
  }

  public ProcessResponse startProjectSummary(UUID projectId) {
    projectService.find(projectId);
    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("PROJECT_SUMMARY")
                .status("RUNNING")
                .startedAt(OffsetDateTime.now())
                .build());
    try {
      projectSummaryService.refreshSummary(projectId);
      process.setProcessedFiles(1);
      process.setStatus("COMPLETED");
    } catch (Exception e) {
      process.setStatus("FAILED");
      process.setFailureCause(e.getMessage());
    }
    process.setCompletedAt(OffsetDateTime.now());
    processRepository.save(process);
    return toResponse(process);
  }

  public List<ProcessResponse> list(UUID projectId) {
    projectService.find(projectId);
    return processRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
        .map(this::toResponse)
        .toList();
  }

  /**
   * Lists all processes for a specific project version identified by name and version.
   *
   * <p>The project name is normalized to kebab-case before lookup, allowing flexible name matching.
   *
   * @param projectName the project name (will be normalized)
   * @param version the version number
   * @return list of processes ordered by creation date (most recent first)
   * @throws NotFoundException if the project version does not exist
   */
  public List<ProcessResponse> listByProjectVersion(String projectName, Integer version) {
    var project = projectService.findByNameAndVersion(projectName, version);
    return processRepository.findByProjectIdOrderByCreatedAtDesc(project.getProjectId()).stream()
        .map(this::toResponse)
        .toList();
  }

  /**
   * Gets a processing summary for a specific project version identified by name and version.
   *
   * <p>This is a convenience method that looks up the project by name and version, then delegates
   * to {@link #getProcessingSummary(UUID)}.
   *
   * @param projectName the project name (will be normalized)
   * @param version the version number
   * @return processing summary with aggregated statistics
   * @throws NotFoundException if the project version does not exist
   */
  public ProcessingSummaryResponse getProcessingSummaryByVersion(
      String projectName, Integer version) {
    var project = projectService.findByNameAndVersion(projectName, version);
    return getProcessingSummary(project.getProjectId());
  }

  public ProcessingSummaryResponse getProcessingSummary(UUID projectId) {
    var project = projectService.find(projectId);
    List<ProcessTrackingEntity> allProcesses =
        processRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

    long totalBytes =
        allProcesses.stream()
            .filter(p -> p.getTotalBytesProcessed() != null)
            .mapToLong(ProcessTrackingEntity::getTotalBytesProcessed)
            .sum();

    long totalTokens =
        allProcesses.stream()
            .filter(p -> p.getTotalTokensProcessed() != null)
            .mapToLong(ProcessTrackingEntity::getTotalTokensProcessed)
            .sum();

    int completedCount =
        (int)
            allProcesses.stream()
                .filter(
                    p ->
                        "COMPLETED".equals(p.getStatus())
                            || "PARTIAL_SUCCESS".equals(p.getStatus()))
                .count();

    int failedCount =
        (int) allProcesses.stream().filter(p -> "FAILED".equals(p.getStatus())).count();

    int runningCount =
        (int) allProcesses.stream().filter(p -> "RUNNING".equals(p.getStatus())).count();

    OffsetDateTime lastProcessedAt =
        allProcesses.stream()
            .filter(p -> p.getCompletedAt() != null)
            .map(ProcessTrackingEntity::getCompletedAt)
            .max(OffsetDateTime::compareTo)
            .orElse(null);

    List<ProcessResponse> recentProcesses =
        allProcesses.stream().limit(10).map(this::toResponse).toList();

    return new ProcessingSummaryResponse(
        projectId,
        project.getProjectName(),
        project.getVersion(),
        totalBytes,
        totalTokens,
        allProcesses.size(),
        completedCount,
        failedCount,
        runningCount,
        recentProcesses,
        lastProcessedAt);
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

  private String toJsonArray(List<String> values) {
    return "[\""
        + String.join("\",\"", values.stream().map(v -> v.replace("\"", "\\\"")).toList())
        + "\"]";
  }
}
