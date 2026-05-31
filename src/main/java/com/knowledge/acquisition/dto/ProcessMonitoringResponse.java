package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Real-time monitoring response for a running process with efficiency metrics and cost analysis.
 *
 * <p>This response includes detailed metrics for live dashboards and monitoring tools.
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
