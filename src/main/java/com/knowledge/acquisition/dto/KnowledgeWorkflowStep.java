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
public class KnowledgeWorkflowStep {
  private UUID stepId;
  private UUID workflowId;
  private Integer sequenceNumber;
  private String stepName;
  private String actor;
  private String actionText;
  private String inputData;
  private String outputData;
  private String nextStep;
  private List<Double> embedding;
  private String technicalDetails;
  private String inputParameters;
  private String outputParameters;
  private OffsetDateTime createdAt;
}
