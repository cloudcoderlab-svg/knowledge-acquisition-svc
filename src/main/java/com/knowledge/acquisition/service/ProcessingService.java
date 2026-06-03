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

/**
 * Service for orchestrating knowledge acquisition processing workflows.
 *
 * <p>This service manages the complete processing pipeline for knowledge ingestion projects,
 * including document ingestion, cross-document consolidation, and planning artifact generation.
 * Each process is tracked with detailed status, progress metrics, and failure information.
 *
 * <h3>Processing Workflow Types</h3>
 *
 * <ol>
 *   <li><b>PROJECT_INGESTION:</b> Extracts knowledge from source documents in GCS
 *       <ul>
 *         <li>Parses documents (PDF, DOCX, HTML, etc.)
 *         <li>Extracts structured entities (workflows, APIs, data models, business rules)
 *         <li>Generates document chunks with embeddings
 *         <li>Tracks progress per file with token/byte metrics
 *       </ul>
 *   <li><b>CROSS_DOCUMENT_CONSOLIDATION:</b> Analyzes relationships across ingested documents
 *       <ul>
 *         <li>Infers relationships between entities from different documents
 *         <li>Generates consolidated knowledge chunks
 *         <li>Updates cross-references and dependencies
 *       </ul>
 *   <li><b>PLANNING_GENERATION:</b> Generates planning artifacts from ingested knowledge
 *       <ul>
 *         <li>Creates domain-based EPICs, FEATUREs, USER_STORYs
 *         <li>Generates acceptance criteria
 *         <li>Structures planning hierarchy
 *       </ul>
 *   <li><b>PROJECT_SUMMARY:</b> Generates high-level project summary
 *       <ul>
 *         <li>Summarizes ingested knowledge
 *         <li>Generates summary embedding for semantic search
 *       </ul>
 * </ol>
 *
 * <h3>Process Tracking</h3>
 *
 * Each process is tracked with:
 *
 * <ul>
 *   <li>Status: RUNNING, COMPLETED, PARTIAL_SUCCESS, FAILED
 *   <li>Progress: total files, processed files, failed files
 *   <li>Metrics: bytes processed, tokens consumed
 *   <li>Timing: start time, completion time
 *   <li>Error details: failure cause, current file on failure
 * </ul>
 *
 * <h3>Asynchronous Execution</h3>
 *
 * All processes run asynchronously and return immediately with a process tracking ID. Clients can
 * poll the process status using the tracking ID or monitor via the live monitoring endpoint.
 *
 * @see ProcessTrackingEntity
 * @see ProcessResponse
 * @see ProcessingSummaryResponse
 * @see IngestionProcessRunner
 */
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

    // Get processable files (exclude directories and definition.md)
    List<String> files =
        fileService.listFiles(projectId).stream()
            .filter(file -> !file.endsWith("/"))
            .filter(file -> !file.endsWith("definition.md"))
            .toList();

    // Validate that there are processable files
    if (files.isEmpty()) {
      ProcessTrackingEntity failedProcess =
          processRepository.save(
              ProcessTrackingEntity.builder()
                  .projectId(projectId)
                  .processType("PROJECT_INGESTION")
                  .status("FAILED")
                  .totalFiles(0)
                  .processedFiles(0)
                  .failedFiles(0)
                  .startedAt(OffsetDateTime.now())
                  .completedAt(OffsetDateTime.now())
                  .failureCause(
                      "No processable files found in project. Only definition.md or empty folder detected. "
                          + "Please upload documents (PDF, DOCX, XML, etc.) to start ingestion.")
                  .build());
      return toResponse(failedProcess);
    }

    ProcessTrackingEntity process =
        processRepository.save(
            ProcessTrackingEntity.builder()
                .projectId(projectId)
                .processType("PROJECT_INGESTION")
                .status("RUNNING")
                .totalFiles(files.size())
                .processedFiles(0)
                .failedFiles(0)
                .startedAt(OffsetDateTime.now())
                .build());
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
