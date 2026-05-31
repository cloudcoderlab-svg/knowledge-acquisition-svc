package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableContent {
  private List<String> headers;
  private List<List<String>> rows;
  private String caption;
  private int pageNumber;
}
