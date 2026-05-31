package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessRule {
  private String ruleName;
  private String ruleType;
  private String condition;
  private String outcome;
  private String sourceBusinessComponentName;
  private String priority;
  private Double confidence;
  private String technicalImplementation;
  private String validationCriteria;
}
