package com.knowledge.acquisition.actuator;

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
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/**
 * Custom actuator endpoint for real-time process monitoring.
 *
 * <p>Exposes live processing metrics at /actuator/process-monitoring for building dashboards and
 * monitoring tools.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>All currently running processes with detailed metrics
 *   <li>Recently completed processes (last hour)
 *   <li>System-wide statistics and cost analysis
 *   <li>Efficiency metrics (tokens per byte, cost per file, etc.)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>
 * curl http://localhost:8087/actuator/process-monitoring
 * </pre>
 */
@Component
@Endpoint(id = "process-monitoring")
@RequiredArgsConstructor
public class ProcessMonitoringEndpoint {

  private final ProcessTrackingRepository processRepository;
  private final ProjectRepository projectRepository;

  // Gemini 2.5 Flash pricing (per million tokens)
  private static final double INPUT_TOKEN_COST = 0.15;
  private static final double OUTPUT_TOKEN_COST = 0.60;

  /**
   * Retrieves live monitoring summary for all processes.
   *
   * @return comprehensive monitoring data including running and completed processes
   */
  @ReadOperation
  public LiveMonitoringSummary monitor() {
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
   * Converts a ProcessTrackingEntity to a ProcessMonitoringResponse with metrics.
   *
   * @param process the process entity
   * @return monitoring response with efficiency and cost metrics
   */
  private ProcessMonitoringResponse toMonitoringResponse(ProcessTrackingEntity process) {
    // Calculate progress percentage
    int progressPercent = 0;
    if (process.getTotalFiles() != null && process.getTotalFiles() > 0) {
      progressPercent = (int) ((process.getProcessedFiles() * 100.0) / process.getTotalFiles());
    }

    // Calculate elapsed time
    long elapsedSeconds = 0;
    if (process.getStartedAt() != null) {
      Duration elapsed = Duration.between(process.getStartedAt(), OffsetDateTime.now());
      elapsedSeconds = elapsed.getSeconds();
    }

    // Get project name
    String projectName = null;
    if (process.getProjectId() != null) {
      projectName =
          projectRepository
              .findById(process.getProjectId())
              .map(ProjectEntity::getProjectName)
              .orElse("Unknown");
    }

    // Calculate efficiency metrics
    EfficiencyMetrics efficiencyMetrics = calculateEfficiencyMetrics(process);

    // Calculate cost analysis
    CostAnalysis costAnalysis = calculateCostAnalysis(process);

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
   * Calculates efficiency metrics for a process.
   *
   * @param process the process entity
   * @return efficiency metrics or null if insufficient data
   */
  private EfficiencyMetrics calculateEfficiencyMetrics(ProcessTrackingEntity process) {
    Long tokens = process.getTotalTokensProcessed();
    Long bytes = process.getTotalBytesProcessed();
    Integer files = process.getProcessedFiles();

    if (tokens == null
        || bytes == null
        || files == null
        || tokens == 0
        || bytes == 0
        || files == 0) {
      return null;
    }

    double tokensPerByte = (double) tokens / bytes;
    double tokensPerKB = (double) tokens / (bytes / 1024.0);
    double tokensPerFile = (double) tokens / files;
    double bytesPerToken = (double) bytes / tokens;

    return new EfficiencyMetrics(tokensPerByte, tokensPerKB, tokensPerFile, bytesPerToken);
  }

  /**
   * Calculates cost analysis for a process using Gemini 2.5 Flash pricing.
   *
   * @param process the process entity
   * @return cost analysis or null if no token data available
   */
  private CostAnalysis calculateCostAnalysis(ProcessTrackingEntity process) {
    Long tokens = process.getTotalTokensProcessed();
    Integer totalFiles = process.getTotalFiles();
    Integer processedFiles = process.getProcessedFiles();

    if (tokens == null || tokens == 0) {
      return null;
    }

    // Estimate input/output split (typically 60/40)
    long estimatedInputTokens = (long) (tokens * 0.6);
    long estimatedOutputTokens = (long) (tokens * 0.4);

    // Calculate current cost
    double inputCost = (estimatedInputTokens / 1_000_000.0) * INPUT_TOKEN_COST;
    double outputCost = (estimatedOutputTokens / 1_000_000.0) * OUTPUT_TOKEN_COST;
    double currentCost = inputCost + outputCost;

    // Project total cost
    double projectedTotalCost = currentCost;
    if (totalFiles != null
        && processedFiles != null
        && processedFiles > 0
        && totalFiles > processedFiles) {
      projectedTotalCost = currentCost * ((double) totalFiles / processedFiles);
    }

    return new CostAnalysis(
        estimatedInputTokens, estimatedOutputTokens, currentCost, projectedTotalCost);
  }

  /**
   * Calculates system-wide statistics across all running processes.
   *
   * @param runningProcesses list of currently running processes
   * @param now current timestamp
   * @return aggregated system statistics
   */
  private SystemStats calculateSystemStats(
      List<ProcessTrackingEntity> runningProcesses, OffsetDateTime now) {
    int totalRunning =
        (int) runningProcesses.stream().filter(p -> "RUNNING".equals(p.getStatus())).count();
    int totalPending =
        (int) runningProcesses.stream().filter(p -> "PENDING".equals(p.getStatus())).count();

    int totalFiles =
        runningProcesses.stream()
            .map(ProcessTrackingEntity::getTotalFiles)
            .filter(f -> f != null)
            .mapToInt(Integer::intValue)
            .sum();

    long totalTokens =
        runningProcesses.stream()
            .map(ProcessTrackingEntity::getTotalTokensProcessed)
            .filter(t -> t != null)
            .mapToLong(Long::longValue)
            .sum();

    long totalBytes =
        runningProcesses.stream()
            .map(ProcessTrackingEntity::getTotalBytesProcessed)
            .filter(b -> b != null)
            .mapToLong(Long::longValue)
            .sum();

    // Calculate total cost for today
    OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
    List<ProcessTrackingEntity> todayProcesses = processRepository.findByStartedAtAfter(startOfDay);

    double totalCostToday =
        todayProcesses.stream()
            .map(ProcessTrackingEntity::getTotalTokensProcessed)
            .filter(t -> t != null && t > 0)
            .mapToDouble(
                t -> {
                  long inputTokens = (long) (t * 0.6);
                  long outputTokens = (long) (t * 0.4);
                  return ((inputTokens / 1_000_000.0) * INPUT_TOKEN_COST)
                      + ((outputTokens / 1_000_000.0) * OUTPUT_TOKEN_COST);
                })
            .sum();

    return new SystemStats(
        totalRunning, totalPending, totalFiles, totalTokens, totalBytes, totalCostToday);
  }
}
