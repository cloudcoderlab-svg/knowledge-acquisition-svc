package com.knowledge.acquisition.ingestion.util;

import java.util.List;

/**
 * Utility class for embedding-related operations.
 *
 * <p>Provides helper methods for converting embeddings to pgvector format.
 */
public final class EmbeddingUtils {

  private EmbeddingUtils() {}

  /**
   * Converts a List&lt;Double&gt; embedding to pgvector string format: "[0.1, 0.2, 0.3]".
   *
   * @param embedding the embedding as a list of doubles
   * @return the pgvector-formatted string, or null if embedding is null
   */
  public static String embeddingToString(List<Double> embedding) {
    if (embedding == null || embedding.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < embedding.size(); i++) {
      if (i > 0) sb.append(",");
      sb.append(embedding.get(i));
    }
    sb.append("]");
    return sb.toString();
  }
}
