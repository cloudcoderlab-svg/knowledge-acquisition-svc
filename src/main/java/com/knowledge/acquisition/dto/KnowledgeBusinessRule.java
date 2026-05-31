package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBusinessRule {
  private UUID ruleId;
  private UUID sourceDocumentId;
  private UUID componentId;
  private UUID projectId;
  private UUID domainId;
  private String ruleName;
  private String ruleType;
  private String conditionText;
  private String outcomeText;
  private String priority;
  private Double confidence;
  private List<Double> embedding;
  private String technicalImplementation;
  private String validationCriteria;
  private OffsetDateTime createdAt;
}
