package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProcessingSummaryResponse(
    UUID projectId,
    String projectName,
    Integer version,
    Long totalBytesProcessed,
    Long totalTokensProcessed,
    Integer totalProcesses,
    Integer completedProcesses,
    Integer failedProcesses,
    Integer runningProcesses,
    List<ProcessResponse> recentProcesses,
    OffsetDateTime lastProcessedAt) {}
