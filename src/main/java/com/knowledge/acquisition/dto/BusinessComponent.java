package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BusinessComponent {
  private String componentName;
  private String componentType;
  private String capability;
  private String owner;
  private String lifecycle;
  private Double confidence;
}
