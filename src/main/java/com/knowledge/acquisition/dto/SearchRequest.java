package com.knowledge.acquisition.dto;

import jakarta.validation.constraints.Positive;

public record SearchRequest(
    String query, String embedding, String factType, @Positive Integer limit) {
  public boolean hasQuery() {
    return query != null && !query.isBlank();
  }

  public boolean hasEmbedding() {
    return embedding != null && !embedding.isBlank();
  }

  public boolean hasFactType() {
    return factType != null && !factType.isBlank();
  }
}
