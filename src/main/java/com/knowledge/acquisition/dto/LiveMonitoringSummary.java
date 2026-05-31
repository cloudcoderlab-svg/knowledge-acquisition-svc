package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Live monitoring summary for all active processes in the system.
 *
 * <p>Provides a comprehensive dashboard view of all running, pending, and recently completed
 * processes with aggregated system-wide statistics. Designed for real-time monitoring UI polling
 * (recommended interval: 5-30 seconds).
 *
 * <p><b>Dashboard Features:</b>
 *
 * <ul>
 *   <li>System-wide resource consumption (tokens, bytes, cost)
 *   <li>Active process tracking with progress percentages
 *   <li>Recently completed process history (last hour, max 10 entries)
 *   <li>Daily cost tracking for budget monitoring
 *   <li>Efficiency metrics for optimization insights
 * </ul>
 *
 * <p><b>Polling Strategy:</b>
 *
 * <ul>
 *   <li>High frequency (5s): For active monitoring during ingestion
 *   <li>Medium frequency (15s): For normal operational monitoring
 *   <li>Low frequency (30s): For background status checks
 * </ul>
 *
 * <p><b>Example Response:</b>
 *
 * <pre>
 * {
 *   "timestamp": "2025-01-31T10:30:00Z",
 *   "systemStats": {
 *     "totalRunningProcesses": 2,
 *     "totalPendingProcesses": 0,
 *     "totalFilesProcessing": 26,
 *     "totalTokensConsumed": 450000,
 *     "totalBytesProcessed": 800000,
 *     "totalCostToday": 0.25
 *   },
 *   "runningProcesses": [...],
 *   "recentlyCompleted": [...]
 * }
 * </pre>
 *
 * @param timestamp current server timestamp when summary was generated
 * @param systemStats aggregated statistics across all processes
 * @param runningProcesses list of currently running or pending processes
 * @param recentlyCompleted list of recently completed processes (last hour, max 10)
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
