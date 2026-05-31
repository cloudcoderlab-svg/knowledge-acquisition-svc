package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlanningResponse(UUID projectId, List<PlanningFact> facts) {

  public record PlanningFact(
      UUID factId,
      UUID parentFactId,
      UUID rootFactId,
      String factType,
      String factKey,
      String title,
      String summary,
      String content,
      String priority,
      String sourceEntityType,
      UUID sourceEntityId,
      UUID sourceRuleId,
      String attributes,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      List<PlanningFact> children) {}
}
