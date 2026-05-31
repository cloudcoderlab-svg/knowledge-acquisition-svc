package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificationResult {

  @JsonAlias("primaryDomain")
  private String domain;

  private String subdomain;
  private String businessCapability;
  private String technicalCapability;
  private List<String> entities;
  private List<String> integrationTypes;
  private Double confidence;
  private String summary;
  private UUID sourceDocumentId; // Added for tracking ingested documents
  private Long tokensConsumed; // AI model tokens consumed for this classification
}
