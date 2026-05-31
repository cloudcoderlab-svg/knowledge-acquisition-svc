package com.knowledge.acquisition.ingestion.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmbeddingServiceTest {

  @Test
  void returnsNullAndActivatesCooldownWhenQuotaIsExhausted() throws Exception {
    VertexAIService vertexAIService = mock(VertexAIService.class);
    when(vertexAIService.embedding("content"))
        .thenThrow(new RuntimeException("RESOURCE_EXHAUSTED: Quota exceeded"));
    EmbeddingService service = new EmbeddingService(vertexAIService);
    ReflectionTestUtils.setField(service, "quotaCooldownMs", 600000L);

    assertThat(service.embedding("content")).isNull();
    assertThat(service.embedding("content")).isNull();

    verify(vertexAIService, times(1)).embedding("content");
    assertThat(service.isQuotaCooldownActive()).isTrue();
  }

  @Test
  void stillThrowsNonQuotaFailures() throws Exception {
    VertexAIService vertexAIService = mock(VertexAIService.class);
    when(vertexAIService.embedding("content")).thenThrow(new RuntimeException("permission denied"));
    EmbeddingService service = new EmbeddingService(vertexAIService);

    assertThatThrownBy(() -> service.embedding("content"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to generate embedding");
  }

  @Test
  void mapsFloatEmbeddingsToDoublesWhenQuotaIsAvailable() throws Exception {
    VertexAIService vertexAIService = mock(VertexAIService.class);
    when(vertexAIService.embedding("content")).thenReturn(List.of(0.25f, 0.5f));
    EmbeddingService service = new EmbeddingService(vertexAIService);

    assertThat(service.embedding("content")).containsExactly(0.25d, 0.5d);
  }
}
