package com.knowledge.acquisition.dto;

import com.knowledge.acquisition.entity.ProjectStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID projectId,
    String projectName,
    Integer version,
    String title,
    String description,
    String definition,
    String summary,
    String sourceBucket,
    String gcsPrefix,
    ProjectStatus status,
    String metadata,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime summaryGeneratedAt) {}
