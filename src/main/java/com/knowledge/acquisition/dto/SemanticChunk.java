package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemanticChunk {
  private int chunkIndex;
  private int totalChunks;
  private int charStart;
  private int charEnd;
  private String content;
  private String contentHash;
  private ClassificationResult classification;
  private List<Double> embedding;
  private KnowledgeExtractionResult knowledgeExtraction;
}
