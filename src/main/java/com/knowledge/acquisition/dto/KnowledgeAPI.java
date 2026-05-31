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
public class KnowledgeAPI {
  private UUID apiId;
  private UUID componentId;
  private UUID sourceDocumentId;
  private UUID projectId;
  private String apiName;
  private String apiType;
  private String httpMethod;
  private String endpointPath;
  private String description;
  private Map<String, Object> requestSchema;
  private Map<String, Object> responseSchema;
  private String authentication;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
