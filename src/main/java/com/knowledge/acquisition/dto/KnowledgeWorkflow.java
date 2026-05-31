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
public class KnowledgeWorkflow {
  private UUID workflowId;
  private UUID sourceDocumentId;
  private UUID projectId;
  private UUID domainId;
  private String workflowName;
  private String triggerText;
  private String outcomeText;
  private String owner;
  private Double confidence;
  private List<Double> embedding;
  private List<KnowledgeWorkflowStep> steps;
  private OffsetDateTime createdAt;
}
