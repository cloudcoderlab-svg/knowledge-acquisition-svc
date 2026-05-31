package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentResource {
  private String resourceName;
  private String resourceType;
  private String provider;
  private String hostingModel;
  private String environment;
  private String region;
  private String criticality;
  private String lifecycle;
  private List<ResourceConfig> configs;
  private Double confidence;
}
