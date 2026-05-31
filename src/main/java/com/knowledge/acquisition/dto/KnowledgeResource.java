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
public class KnowledgeResource {
  private UUID resourceId;
  private UUID sourceDocumentId;
  private UUID componentId;
  private UUID projectId;
  private String resourceName;
  private String resourceType;
  private String provider;
  private String hostingModel;
  private String environment;
  private String region;
  private String criticality;
  private String lifecycle;
  private Map<String, Object> configs;
  private Double confidence;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
