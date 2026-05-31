package com.knowledge.acquisition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionMetrics {
  private long bytesProcessed;
  private long tokensUsed;

  public static IngestionMetrics empty() {
    return IngestionMetrics.builder().bytesProcessed(0L).tokensUsed(0L).build();
  }

  public void add(IngestionMetrics other) {
    if (other != null) {
      this.bytesProcessed += other.bytesProcessed;
      this.tokensUsed += other.tokensUsed;
    }
  }

  public void addBytes(long bytes) {
    this.bytesProcessed += bytes;
  }

  public void addTokens(long tokens) {
    this.tokensUsed += tokens;
  }
}
