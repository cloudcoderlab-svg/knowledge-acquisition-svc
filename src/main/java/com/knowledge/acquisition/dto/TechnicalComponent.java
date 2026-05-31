package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TechnicalComponent {
  private String componentName;
  private String componentType;
  private String responsibility;
  private String technology;
  private String lifecycle;
  private Double confidence;
}
