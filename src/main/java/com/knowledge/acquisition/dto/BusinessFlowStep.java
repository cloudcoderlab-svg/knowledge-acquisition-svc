package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BusinessFlowStep {
  private Integer sequence;
  private String stepName;
  private String actor;
  private String action;
  private String input;
  private String output;
  private String nextStep;
  private String technicalDetails;
  private String inputParameters;
  private String outputParameters;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public BusinessFlowStep(String stepName) {
    this.stepName = stepName;
  }

  public BusinessFlowStep() {}
}
