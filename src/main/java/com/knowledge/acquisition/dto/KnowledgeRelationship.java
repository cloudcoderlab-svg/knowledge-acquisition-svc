package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class KnowledgeRelationship {
  @JsonAlias("sourceEntityName")
  private String sourceName;

  @JsonAlias("sourceEntityType")
  private String sourceType;

  @JsonAlias("targetEntityName")
  private String targetName;

  @JsonAlias("targetEntityType")
  private String targetType;

  private String relationshipType;

  @JsonAlias("description")
  private String context;

  private Double confidence;
}
