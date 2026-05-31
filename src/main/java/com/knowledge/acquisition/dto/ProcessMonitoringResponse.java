package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Real-time monitoring response for a running process with efficiency metrics and cost analysis.
 *
 * <p>This comprehensive DTO provides all necessary data for building live monitoring dashboards,
 * tracking process execution, analyzing resource consumption, and estimating costs.
 *
 * <p><b>Process Types:</b>
 *
 * <ul>
 *   <li>PROJECT_INGESTION - File ingestion and knowledge extraction
 *   <li>PROJECT_PIPELINE - Full 4-stage pipeline execution
 *   <li>CROSS_DOCUMENT_CONSOLIDATION - Relationship mapping
 *   <li>PLANNING_GENERATION - Migration plan generation
 *   <li>PROJECT_SUMMARY - Summary and embedding generation
 * </ul>
 *
 * <p><b>Status Values:</b>
 *
 * <ul>
 *   <li>PENDING - Queued but not yet started
 *   <li>RUNNING - Currently executing
 *   <li>COMPLETED - Successfully completed
 *   <li>PARTIAL_SUCCESS - Completed with some failures
 *   <li>FAILED - Failed to complete
 * </ul>
 *
 * @param processId unique identifier for the process
 * @param projectId UUID of the project being processed
 * @param projectName human-readable project name
 * @param processType type of process (e.g., PROJECT_INGESTION, PROJECT_PIPELINE)
 * @param status current status (PENDING, RUNNING, COMPLETED, etc.)
 * @param totalFiles total number of files to process
 * @param processedFiles number of files successfully processed
 * @param failedFiles number of files that failed processing
 * @param progressPercent calculated progress percentage (0-100)
 * @param currentFile name of file currently being processed
 * @param totalTokensProcessed cumulative tokens consumed by AI models
 * @param totalBytesProcessed cumulative bytes of data processed
 * @param efficiencyMetrics efficiency ratios (tokens/byte, tokens/KB, etc.) or null if insufficient
 *     data
 * @param costAnalysis cost estimates and projections or null if no token data
 * @param startedAt timestamp when process began execution
 * @param updatedAt timestamp of last progress update
 * @param elapsedSeconds total seconds elapsed since process started
 */
public record ProcessMonitoringResponse(
    UUID processId,
    UUID projectId,
    String projectName,
    String processType,
    String status,
    Integer totalFiles,
    Integer processedFiles,
    Integer failedFiles,
    Integer progressPercent,
    String currentFile,
    Long totalTokensProcessed,
    Long totalBytesProcessed,
    EfficiencyMetrics efficiencyMetrics,
    CostAnalysis costAnalysis,
    OffsetDateTime startedAt,
    OffsetDateTime updatedAt,
    Long elapsedSeconds) {

  /**
   * Efficiency metrics for token usage analysis.
   *
   * @param tokensPerByte tokens consumed per byte of input data
   * @param tokensPerKB tokens consumed per kilobyte of input data
   * @param tokensPerFile average tokens consumed per file
   * @param bytesPerToken bytes processed per token consumed
   */
  public record EfficiencyMetrics(
      Double tokensPerByte, Double tokensPerKB, Double tokensPerFile, Double bytesPerToken) {}

  /**
   * Cost analysis using Gemini 2.5 Flash pricing.
   *
   * @param estimatedInputTokens estimated input tokens (60% of total)
   * @param estimatedOutputTokens estimated output tokens (40% of total)
   * @param currentCost cost for tokens consumed so far (USD)
   * @param projectedTotalCost projected total cost for all files (USD)
   */
  public record CostAnalysis(
      Long estimatedInputTokens,
      Long estimatedOutputTokens,
      Double currentCost,
      Double projectedTotalCost) {}
}
