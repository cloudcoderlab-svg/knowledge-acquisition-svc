package com.knowledge.acquisition.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiagramContent {
  private String description;
  private byte[] imageData;
  private int pageNumber;
  private Double confidence;
}
