package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TechnicalComponent {
  private String componentName;
  private String componentLayer;
  private String componentType;
  private String capability;
  private String responsibility;
  private String description;
  private String technology;
  private String owner;
  private String lifecycle;
  private List<String> interfaces;
  private List<String> dependencies;
  private Double confidence;
}
