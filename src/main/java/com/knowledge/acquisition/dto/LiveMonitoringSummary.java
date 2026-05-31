package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Live monitoring summary for all active processes in the system.
 *
 * <p>Provides a dashboard view of all running, pending, and recently completed processes with
 * aggregated statistics and efficiency metrics.
 */
public record LiveMonitoringSummary(
    OffsetDateTime timestamp,
    SystemStats systemStats,
    List<ProcessMonitoringResponse> runningProcesses,
    List<ProcessMonitoringResponse> recentlyCompleted) {

  /**
   * System-wide statistics for all processes.
   *
   * @param totalRunningProcesses count of currently running processes
   * @param totalPendingProcesses count of pending processes
   * @param totalFilesProcessing total files being processed across all running processes
   * @param totalTokensConsumed total tokens consumed across all processes
   * @param totalBytesProcessed total bytes processed across all processes
   * @param totalCostToday estimated total cost for today's processing (USD)
   */
  public record SystemStats(
      Integer totalRunningProcesses,
      Integer totalPendingProcesses,
      Integer totalFilesProcessing,
      Long totalTokensConsumed,
      Long totalBytesProcessed,
      Double totalCostToday) {}
}
