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
public class KnowledgeIntegration {
  private UUID integrationId;
  private UUID sourceDocumentId;
  private UUID componentId;
  private UUID projectId;
  private String integrationName;
  private String integrationType;
  private String sourceSystem;
  private String targetSystem;
  private String protocol;
  private String dataExchanged;
  private String description;
  private Double confidence;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
