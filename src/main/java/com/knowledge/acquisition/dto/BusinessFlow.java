package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BusinessFlow {
  private String flowName;
  private String trigger;
  private String outcome;
  private String owner;
  private List<BusinessFlowStep> steps;
  private Double confidence;
}
