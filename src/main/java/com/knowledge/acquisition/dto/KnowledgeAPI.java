package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

  @JsonAlias("inputParameters")
  private Map<String, Object> requestSchema;

  @JsonAlias("outputParameters")
  private Map<String, Object> responseSchema;

  private String authentication;
  private String sourceComponentName;
  private String businessCapability;
  private Double confidence;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
