package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ResourceCostEstimate {
  private String resourceName;
  private String environment;
  private String provider;
  private String billingModel;
  private Double quantity;
  private String unit;
  private Double unitCost;
  private Double estimatedMonthlyCost;
  private String currency;
  private String pricingSource;
  private String pricingDate;
  private String assumptions;
  private Double confidence;
}
