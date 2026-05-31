package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class KnowledgeRelationship {
  private String sourceName;
  private String sourceType;
  private String targetName;
  private String targetType;
  private String relationshipType;
  private String context;
  private Double confidence;
}
