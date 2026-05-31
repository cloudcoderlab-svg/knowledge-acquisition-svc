package com.knowledge.acquisition.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for AI responses with token usage metadata.
 *
 * <p>Captures both the generated content and token consumption metrics from AI model invocations.
 * This enables tracking of API costs and resource usage across the knowledge ingestion pipeline.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
  /** The generated text content from the AI model. */
  private String content;

  /** Total number of tokens consumed (input + output). */
  private long totalTokens;

  /** Number of tokens in the prompt/input. */
  private long promptTokens;

  /** Number of tokens in the generated response/output. */
  private long responseTokens;

  /**
   * Creates an AIResponse with only content (for cases where token tracking is unavailable).
   *
   * @param content the generated content
   * @return AIResponse with zero token counts
   */
  public static AIResponse withoutTokens(String content) {
    return new AIResponse(content, 0, 0, 0);
  }

  /**
   * Creates an AIResponse with content and total token count.
   *
   * @param content the generated content
   * @param totalTokens total tokens consumed
   * @return AIResponse with token count
   */
  public static AIResponse withTokens(String content, long totalTokens) {
    return new AIResponse(content, totalTokens, 0, 0);
  }

  /**
   * Creates a complete AIResponse with all token metadata.
   *
   * @param content the generated content
   * @param totalTokens total tokens consumed
   * @param promptTokens input tokens
   * @param responseTokens output tokens
   * @return complete AIResponse
   */
  public static AIResponse of(
      String content, long totalTokens, long promptTokens, long responseTokens) {
    return new AIResponse(content, totalTokens, promptTokens, responseTokens);
  }
}
