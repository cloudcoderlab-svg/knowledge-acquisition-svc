package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKnowledge {
  private String overallArchitecture;
  private String systemSummary;
  private List<String> keyPatterns;
  private List<String> technologies;
  private String domain;
  private String subdomain;
  private List<String> identifiedComponents;
  private List<String> identifiedAPIs;
  private List<String> identifiedWorkflows;
  private List<String> identifiedCapabilities;
  private List<String> identifiedRoles;
  private List<String> identifiedTerms;
  private List<String> identifiedPolicies;
  private List<String> identifiedDecisions;
  private Map<String, Object> additionalMetadata;

  // Platform detection for routing to specialized prompts
  private String detectedPlatform; // e.g., "TIBCO_MDM", "PEGA_BPM", "GENERIC_XML", etc.
}
