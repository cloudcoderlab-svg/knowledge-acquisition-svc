package com.knowledge.acquisition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProjectDiscoveryRequest(
    @NotBlank String task, String embedding, @Positive Integer limit, Double minScore) {
  public boolean hasEmbedding() {
    return embedding != null && !embedding.isBlank();
  }
}
