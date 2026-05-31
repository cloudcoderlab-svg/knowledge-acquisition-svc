package com.knowledge.acquisition.ingestion.service.ai;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates vector embeddings with a fail-open quota circuit breaker.
 *
 * <p>Embedding generation improves search quality but should not block ingestion. When Vertex AI
 * reports quota exhaustion, this service activates a shared cooldown and returns {@code null} for
 * embedding requests until the cooldown expires. Non-quota failures still propagate so
 * configuration, authentication, and connectivity problems remain visible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

  private final VertexAIService vertexAIService;

  @Value("${knowledge-engine.embedding.quota-cooldown-ms:600000}")
  private long quotaCooldownMs;

  private volatile long quotaCooldownUntilEpochMs;

  /**
   * Generates an embedding for text, or returns {@code null} while quota cooldown is active.
   *
   * @param text text to embed
   * @return vector embedding converted to doubles, or {@code null} when quota is exhausted
   */
  public List<Double> embedding(String text) {
    if (isQuotaCooldownActive()) {
      log.debug("Skipping embedding call because quota cooldown is active");
      return null;
    }

    try {
      List<Float> result = vertexAIService.embedding(text);
      // Handle null from circuit breaker fallback
      if (result == null) {
        return null;
      }
      return result.stream().map(Float::doubleValue).toList();
    } catch (Exception e) {
      if (isQuotaExhausted(e)) {
        activateQuotaCooldown(e);
        return null;
      }
      throw new RuntimeException("Failed to generate embedding", e);
    }
  }

  /** Returns whether this service is currently skipping Vertex embedding calls due to quota. */
  boolean isQuotaCooldownActive() {
    return System.currentTimeMillis() < quotaCooldownUntilEpochMs;
  }

  /** Starts the cooldown window after a quota exhaustion response. */
  private void activateQuotaCooldown(Exception exception) {
    long cooldown = Math.max(0, quotaCooldownMs);
    quotaCooldownUntilEpochMs = System.currentTimeMillis() + cooldown;
    log.warn(
        "Embedding quota exhausted. Skipping embedding generation for {} ms. Cause: {}",
        cooldown,
        exception.getMessage());
  }

  /** Walks an exception cause chain looking for Vertex quota exhaustion signals. */
  private boolean isQuotaExhausted(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      String message = current.toString();
      if (message.contains("RESOURCE_EXHAUSTED") || message.contains("Quota exceeded")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
