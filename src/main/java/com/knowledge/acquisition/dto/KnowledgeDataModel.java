package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDataModel {
  private UUID dataModelId;
  private UUID sourceDocumentId;
  private UUID projectId;
  private UUID domainId;
  private String modelName;
  private String modelType;
  private String description;
  private Map<String, Object> schemaDefinition;
  private List<Double> embedding;
  private List<KnowledgeDataField> fields;
  private OffsetDateTime createdAt;
}
