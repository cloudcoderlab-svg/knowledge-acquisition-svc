package com.knowledge.acquisition.controller;

import com.knowledge.acquisition.dto.LiveMonitoringSummary;
import com.knowledge.acquisition.dto.LiveMonitoringSummary.SystemStats;
import com.knowledge.acquisition.dto.ProcessMonitoringResponse;
import com.knowledge.acquisition.dto.ProcessMonitoringResponse.CostAnalysis;
import com.knowledge.acquisition.dto.ProcessMonitoringResponse.EfficiencyMetrics;
import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import com.knowledge.acquisition.entity.ProjectEntity;
import com.knowledge.acquisition.repository.ProcessTrackingRepository;
import com.knowledge.acquisition.repository.ProjectRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for real-time process monitoring and live dashboard support.
 *
 * <p>This controller provides comprehensive monitoring capabilities for all knowledge extraction
 * processes, enabling real-time visibility into pipeline execution, resource consumption, and cost
 * analysis. Designed specifically for building live monitoring dashboards and operational
 * intelligence tools.
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li>Real-time progress tracking for active ingestion, consolidation, planning, and summary
 *       processes
 *   <li>Token consumption metrics with efficiency ratios (tokens/byte, tokens/KB, tokens/file)
 *   <li>Cost analysis using Gemini 2.5 Flash pricing ($0.15/1M input, $0.60/1M output tokens)
 *   <li>System-wide aggregated statistics across all running processes
 *   <li>Recently completed process history (last hour)
 *   <li>Projected cost estimation for in-progress processes
 * </ul>
 *
 * <p><b>Monitoring Metrics:</b>
 *
 * <ul>
 *   <li><b>Progress</b> - File counts, percentages, elapsed time
 *   <li><b>Efficiency</b> - Tokens per byte/KB/file, bytes per token
 *   <li><b>Cost</b> - Current cost, projected total cost, daily cost aggregation
 *   <li><b>Status</b> - RUNNING, PENDING, COMPLETED, PARTIAL_SUCCESS, FAILED
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>
 * GET /api/v1/monitoring/live
 * </pre>
 *
 * <p><b>Refresh Strategy:</b> Poll this endpoint every 5-30 seconds for live dashboard updates.
 *
 * @author Knowledge Acquisition Service
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequiredArgsConstructor
public class ProcessMonitoringController {

  /** Repository for querying process tracking entities. */
  private final ProcessTrackingRepository processRepository;

  /** Repository for resolving project names and details. */
  private final ProjectRepository projectRepository;

  /**
   * Gemini 2.5 Flash input token cost (USD per million tokens).
   *
   * <p>As of 2025, Gemini 2.5 Flash charges $0.15 per million input tokens.
   */
  private static final double INPUT_TOKEN_COST = 0.15;

  /**
   * Gemini 2.5 Flash output token cost (USD per million tokens).
   *
   * <p>As of 2025, Gemini 2.5 Flash charges $0.60 per million output tokens.
   */
  private static final double OUTPUT_TOKEN_COST = 0.60;

  /**
   * Gets live monitoring summary for all processes.
   *
   * <p>Returns comprehensive monitoring data including all running processes, recently completed
   * processes, and system-wide statistics. Perfect for building real-time dashboards.
   *
   * <p>Example response includes:
   *
   * <ul>
   *   <li>Progress percentages and file counts
   *   <li>Token consumption metrics
   *   <li>Efficiency ratios (tokens per byte, tokens per KB, etc.)
   *   <li>Cost estimates and projections
   * </ul>
   *
   * @return live monitoring summary with all active and recent processes
   */
  @GetMapping("/api/v1/monitoring/live")
  public LiveMonitoringSummary getLiveMonitoring() {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);

    // Get running processes
    List<ProcessTrackingEntity> runningProcesses =
        processRepository.findByStatusIn(List.of("RUNNING", "PENDING"));

    // Get recently completed (last hour)
    List<ProcessTrackingEntity> recentlyCompleted =
        processRepository.findByStatusInAndCompletedAtAfter(
            List.of("COMPLETED", "PARTIAL_SUCCESS", "FAILED"), oneHourAgo);

    // Convert to monitoring responses
    List<ProcessMonitoringResponse> runningResponses =
        runningProcesses.stream().map(this::toMonitoringResponse).toList();

    List<ProcessMonitoringResponse> completedResponses =
        recentlyCompleted.stream().map(this::toMonitoringResponse).limit(10).toList();

    // Calculate system stats
    SystemStats systemStats = calculateSystemStats(runningProcesses, now);

    return new LiveMonitoringSummary(now, systemStats, runningResponses, completedResponses);
  }

  /**
   * Converts a ProcessTrackingEntity to a ProcessMonitoringResponse with calculated metrics.
   *
   * <p>This method enriches the raw process entity data with:
   *
   * <ul>
   *   <li>Progress percentage calculation
   *   <li>Elapsed time in seconds
   *   <li>Project name resolution
   *   <li>Efficiency metrics (tokens/byte ratios)
   *   <li>Cost analysis with projections
   * </ul>
   *
   * @param process the process entity from database
   * @return comprehensive monitoring response with all calculated metrics
   */
  private ProcessMonitoringResponse toMonitoringResponse(ProcessTrackingEntity process) {
    // Calculate progress percentage (0-100)
    // Formula: (processedFiles / totalFiles) * 100
    int progressPercent = 0;
    if (process.getTotalFiles() != null && process.getTotalFiles() > 0) {
      progressPercent = (int) ((process.getProcessedFiles() * 100.0) / process.getTotalFiles());
    }

    // Calculate elapsed time since process started
    // Returns total seconds elapsed from start to now
    long elapsedSeconds = 0;
    if (process.getStartedAt() != null) {
      Duration elapsed = Duration.between(process.getStartedAt(), OffsetDateTime.now());
      elapsedSeconds = elapsed.getSeconds();
    }

    // Resolve project name from project ID
    // Falls back to "Unknown" if project not found
    String projectName = null;
    if (process.getProjectId() != null) {
      projectName =
          projectRepository
              .findById(process.getProjectId())
              .map(ProjectEntity::getProjectName)
              .orElse("Unknown");
    }

    // Calculate efficiency metrics (tokens per byte, tokens per KB, etc.)
    // Returns null if insufficient data available
    EfficiencyMetrics efficiencyMetrics = calculateEfficiencyMetrics(process);

    // Calculate cost analysis (current cost and projected total cost)
    // Returns null if no token data available
    CostAnalysis costAnalysis = calculateCostAnalysis(process);

    // Build comprehensive monitoring response
    return new ProcessMonitoringResponse(
        process.getProcessId(),
        process.getProjectId(),
        projectName,
        process.getProcessType(),
        process.getStatus(),
        process.getTotalFiles(),
        process.getProcessedFiles(),
        process.getFailedFiles(),
        progressPercent,
        process.getCurrentFile(),
        process.getTotalTokensProcessed(),
        process.getTotalBytesProcessed(),
        efficiencyMetrics,
        costAnalysis,
        process.getStartedAt(),
        process.getUpdatedAt(),
        elapsedSeconds);
  }

  /**
   * Calculates efficiency metrics for token and data consumption analysis.
   *
   * <p>Computes various efficiency ratios to understand the relationship between tokens consumed
   * and data processed. These metrics help identify:
   *
   * <ul>
   *   <li>Document complexity (higher tokens/byte indicates more complex content)
   *   <li>Processing efficiency across different document types
   *   <li>Cost patterns and optimization opportunities
   * </ul>
   *
   * <p><b>Calculated Metrics:</b>
   *
   * <ul>
   *   <li><b>tokensPerByte</b> - Average tokens consumed per byte of data
   *   <li><b>tokensPerKB</b> - Average tokens consumed per kilobyte (for readability)
   *   <li><b>tokensPerFile</b> - Average tokens consumed per file processed
   *   <li><b>bytesPerToken</b> - Inverse ratio for compression analysis
   * </ul>
   *
   * @param process the process entity with token and byte tracking data
   * @return efficiency metrics or null if insufficient data (zeros or nulls)
   */
  private EfficiencyMetrics calculateEfficiencyMetrics(ProcessTrackingEntity process) {
    Long tokens = process.getTotalTokensProcessed();
    Long bytes = process.getTotalBytesProcessed();
    Integer files = process.getProcessedFiles();

    // Return null if any required data is missing or zero
    // Cannot calculate meaningful ratios without complete data
    if (tokens == null
        || bytes == null
        || files == null
        || tokens == 0
        || bytes == 0
        || files == 0) {
      return null;
    }

    // Calculate tokens per byte (base metric)
    double tokensPerByte = (double) tokens / bytes;

    // Calculate tokens per KB (more readable scale)
    double tokensPerKB = (double) tokens / (bytes / 1024.0);

    // Calculate tokens per file (average complexity)
    double tokensPerFile = (double) tokens / files;

    // Calculate bytes per token (inverse for compression analysis)
    double bytesPerToken = (double) bytes / tokens;

    return new EfficiencyMetrics(tokensPerByte, tokensPerKB, tokensPerFile, bytesPerToken);
  }

  /**
   * Calculates comprehensive cost analysis using Gemini 2.5 Flash pricing model.
   *
   * <p>Estimates costs based on token consumption and provides projected total cost for in-progress
   * processes. Uses estimated input/output token split since Vertex AI tracks total tokens
   * consumed.
   *
   * <p><b>Pricing Model:</b>
   *
   * <ul>
   *   <li>Input tokens: $0.15 per million tokens
   *   <li>Output tokens: $0.60 per million tokens
   *   <li>Estimated split: 60% input, 40% output (based on typical workloads)
   * </ul>
   *
   * <p><b>Cost Projection:</b>
   *
   * <ul>
   *   <li>For completed processes: projectedTotalCost equals currentCost
   *   <li>For in-progress: extrapolates based on (totalFiles / processedFiles) ratio
   *   <li>Assumes linear cost scaling with file count
   * </ul>
   *
   * @param process the process entity with token consumption data
   * @return cost analysis with current and projected costs, or null if no token data available
   */
  private CostAnalysis calculateCostAnalysis(ProcessTrackingEntity process) {
    Long tokens = process.getTotalTokensProcessed();
    Integer totalFiles = process.getTotalFiles();
    Integer processedFiles = process.getProcessedFiles();

    // Return null if no token data available
    if (tokens == null || tokens == 0) {
      return null;
    }

    // Estimate input/output split (typically 60% input, 40% output)
    // This is an approximation since Vertex AI doesn't separate input/output in all cases
    long estimatedInputTokens = (long) (tokens * 0.6);
    long estimatedOutputTokens = (long) (tokens * 0.4);

    // Calculate current cost based on token consumption
    // Input cost: (tokens / 1M) * $0.15
    double inputCost = (estimatedInputTokens / 1_000_000.0) * INPUT_TOKEN_COST;
    // Output cost: (tokens / 1M) * $0.60
    double outputCost = (estimatedOutputTokens / 1_000_000.0) * OUTPUT_TOKEN_COST;
    double currentCost = inputCost + outputCost;

    // Project total cost for in-progress processes
    // Formula: currentCost * (totalFiles / processedFiles)
    // For completed processes, projectedTotalCost equals currentCost
    double projectedTotalCost = currentCost;
    if (totalFiles != null
        && processedFiles != null
        && processedFiles > 0
        && totalFiles > processedFiles) {
      // Extrapolate cost based on remaining files
      projectedTotalCost = currentCost * ((double) totalFiles / processedFiles);
    }

    return new CostAnalysis(
        estimatedInputTokens, estimatedOutputTokens, currentCost, projectedTotalCost);
  }

  /**
   * Calculates system-wide aggregated statistics across all running processes.
   *
   * <p>Provides a holistic view of system utilization and resource consumption by aggregating
   * metrics from all active processes. Also calculates daily cost totals for budget tracking.
   *
   * <p><b>Aggregated Metrics:</b>
   *
   * <ul>
   *   <li><b>totalRunning</b> - Count of processes in RUNNING status
   *   <li><b>totalPending</b> - Count of processes in PENDING status (queued)
   *   <li><b>totalFiles</b> - Sum of all files being processed across all processes
   *   <li><b>totalTokens</b> - Sum of all tokens consumed across all processes
   *   <li><b>totalBytes</b> - Sum of all bytes processed across all processes
   *   <li><b>totalCostToday</b> - Total cost incurred today (midnight to now)
   * </ul>
   *
   * <p><b>Cost Calculation:</b> Queries all processes started today and sums their estimated costs
   * using the same 60/40 input/output token split assumption.
   *
   * @param runningProcesses list of currently running or pending processes
   * @param now current timestamp for daily boundary calculation
   * @return aggregated system statistics including daily cost total
   */
  private SystemStats calculateSystemStats(
      List<ProcessTrackingEntity> runningProcesses, OffsetDateTime now) {
    // Count processes in RUNNING status
    int totalRunning =
        (int) runningProcesses.stream().filter(p -> "RUNNING".equals(p.getStatus())).count();

    // Count processes in PENDING status (queued but not started)
    int totalPending =
        (int) runningProcesses.stream().filter(p -> "PENDING".equals(p.getStatus())).count();

    // Sum total files across all running processes
    int totalFiles =
        runningProcesses.stream()
            .map(ProcessTrackingEntity::getTotalFiles)
            .filter(f -> f != null)
            .mapToInt(Integer::intValue)
            .sum();

    // Sum total tokens consumed across all running processes
    long totalTokens =
        runningProcesses.stream()
            .map(ProcessTrackingEntity::getTotalTokensProcessed)
            .filter(t -> t != null)
            .mapToLong(Long::longValue)
            .sum();

    // Sum total bytes processed across all running processes
    long totalBytes =
        runningProcesses.stream()
            .map(ProcessTrackingEntity::getTotalBytesProcessed)
            .filter(b -> b != null)
            .mapToLong(Long::longValue)
            .sum();

    // Calculate total cost for today (midnight to now)
    // Query all processes started today regardless of status
    OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
    List<ProcessTrackingEntity> todayProcesses = processRepository.findByStartedAtAfter(startOfDay);

    // Sum costs for all processes started today
    double totalCostToday =
        todayProcesses.stream()
            .map(ProcessTrackingEntity::getTotalTokensProcessed)
            .filter(t -> t != null && t > 0)
            .mapToDouble(
                t -> {
                  // Apply same 60/40 input/output split
                  long inputTokens = (long) (t * 0.6);
                  long outputTokens = (long) (t * 0.4);
                  // Calculate cost: (input tokens * $0.15/M) + (output tokens * $0.60/M)
                  return ((inputTokens / 1_000_000.0) * INPUT_TOKEN_COST)
                      + ((outputTokens / 1_000_000.0) * OUTPUT_TOKEN_COST);
                })
            .sum();

    return new SystemStats(
        totalRunning, totalPending, totalFiles, totalTokens, totalBytes, totalCostToday);
  }
}
