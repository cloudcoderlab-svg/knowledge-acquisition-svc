package com.knowledge.acquisition.controller;

import com.knowledge.acquisition.dto.ProcessResponse;
import com.knowledge.acquisition.dto.ProcessingSummaryResponse;
import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import com.knowledge.acquisition.exception.NotFoundException;
import com.knowledge.acquisition.repository.ProcessTrackingRepository;
import com.knowledge.acquisition.service.ProcessingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing knowledge ingestion and processing workflows.
 *
 * <p>This controller provides endpoints for:
 *
 * <ul>
 *   <li>Starting and monitoring ingestion, consolidation, and planning processes
 *   <li>Retrieving process details by project ID, project name/version, or process ID
 *   <li>Getting processing summaries with statistics and recent process history
 * </ul>
 *
 * <p>Process Types:
 *
 * <ul>
 *   <li><b>INGESTION</b>: Extracts and stores knowledge from project documents
 *   <li><b>CONSOLIDATION</b>: Analyzes cross-document relationships and generates knowledge chunks
 *   <li><b>PLANNING</b>: Generates planning artifacts from ingested knowledge
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ProcessingController {
  private final ProcessingService processingService;
  private final ProcessTrackingRepository processRepository;

  /**
   * Starts a knowledge ingestion process for a project.
   *
   * <p>Ingestion extracts knowledge from all source documents in the project's GCS bucket and
   * stores structured data including workflows, data fields, business rules, and document chunks.
   *
   * @param projectId the UUID of the project to ingest
   * @return process tracking details including status and progress
   * @throws NotFoundException if the project does not exist
   */
  @PostMapping("/api/v1/projects/{projectId}/processes/ingestion")
  public ProcessResponse startIngestion(@PathVariable UUID projectId) {
    return processingService.startIngestion(projectId);
  }

  /**
   * Starts a consolidation process for a project.
   *
   * <p>Consolidation analyzes relationships across ingested documents and generates consolidated
   * knowledge chunks for improved retrieval.
   *
   * @param projectId the UUID of the project to consolidate
   * @return process tracking details including status and progress
   * @throws NotFoundException if the project does not exist
   */
  @PostMapping("/api/v1/projects/{projectId}/processes/consolidation")
  public ProcessResponse startConsolidation(@PathVariable UUID projectId) {
    return processingService.startConsolidation(projectId);
  }

  /**
   * Starts a planning generation process for a project.
   *
   * <p>Planning generation creates planning artifacts from the ingested and consolidated knowledge
   * to support downstream use cases.
   *
   * @param projectId the UUID of the project to generate planning for
   * @return process tracking details including status and progress
   * @throws NotFoundException if the project does not exist
   */
  @PostMapping("/api/v1/projects/{projectId}/processes/planning")
  public ProcessResponse startPlanning(@PathVariable UUID projectId) {
    return processingService.startPlanning(projectId);
  }

  /**
   * Lists all processes for a specific project.
   *
   * @param projectId the UUID of the project
   * @return list of all processes ordered by creation date (most recent first)
   * @throws NotFoundException if the project does not exist
   */
  @GetMapping("/api/v1/projects/{projectId}/processes")
  public List<ProcessResponse> list(@PathVariable UUID projectId) {
    return processingService.list(projectId);
  }

  /**
   * Gets a processing summary for a specific project.
   *
   * <p>The summary includes:
   *
   * <ul>
   *   <li>Total bytes and tokens processed
   *   <li>Process counts by status (completed, failed, running)
   *   <li>Last 10 recent processes
   *   <li>Timestamp of most recent completed process
   * </ul>
   *
   * @param projectId the UUID of the project
   * @return processing summary with aggregated statistics
   * @throws NotFoundException if the project does not exist
   */
  @GetMapping("/api/v1/projects/{projectId}/processes/summary")
  public ProcessingSummaryResponse getProcessingSummary(@PathVariable UUID projectId) {
    return processingService.getProcessingSummary(projectId);
  }

  /**
   * Retrieves detailed information about a specific process by its ID.
   *
   * <p>This endpoint can be used to monitor process progress, check status, and retrieve error
   * information if the process failed.
   *
   * @param processId the UUID of the process
   * @return detailed process information
   * @throws NotFoundException if the process does not exist
   */
  @GetMapping("/api/v1/processes/{processId}")
  public ProcessResponse getProcessById(@PathVariable UUID processId) {
    ProcessTrackingEntity process =
        processRepository
            .findById(processId)
            .orElseThrow(() -> new NotFoundException("Process not found: " + processId));
    return toProcessResponse(process);
  }

  /**
   * Converts a ProcessTrackingEntity to a ProcessResponse DTO.
   *
   * @param process the entity to convert
   * @return the DTO representation
   */
  private ProcessResponse toProcessResponse(ProcessTrackingEntity process) {
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
}
