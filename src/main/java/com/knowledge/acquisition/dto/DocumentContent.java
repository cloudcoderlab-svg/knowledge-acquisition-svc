package com.knowledge.acquisition.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentContent {
  private String textContent;
  private List<DiagramContent> diagrams;
  private List<TableContent> tables;
  private Map<String, Object> metadata;
}
