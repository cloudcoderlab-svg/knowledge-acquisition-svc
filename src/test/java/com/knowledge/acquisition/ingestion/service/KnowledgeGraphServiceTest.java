package com.knowledge.acquisition.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.acquisition.dto.KnowledgeDataField;
import com.knowledge.acquisition.dto.KnowledgeDataModel;
import com.knowledge.acquisition.dto.KnowledgeExtractionResult;
import com.knowledge.acquisition.dto.SourceDocumentMetadata;
import com.knowledge.acquisition.ingestion.service.ai.KnowledgeEntityEmbeddingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class KnowledgeGraphServiceTest {

  @Test
  void suppliesRequiredDataFieldValuesBeforePersistence() {
    KnowledgeEntityEmbeddingService embeddingService = mock(KnowledgeEntityEmbeddingService.class);
    PostgresStorageService storageService = mock(PostgresStorageService.class);
    UUID dataModelId = UUID.randomUUID();
    when(storageService.saveKnowledgeDataModel(any())).thenReturn(dataModelId);

    KnowledgeGraphService service = new KnowledgeGraphService(embeddingService, storageService);
    ReflectionTestUtils.setField(service, "enablePersistence", true);

    KnowledgeDataField field =
        KnowledgeDataField.builder()
            .description("Unique identifier for a product associated with an affiliation.")
            .build();
    KnowledgeDataModel dataModel =
        KnowledgeDataModel.builder().modelName("Affiliation").fields(List.of(field)).build();
    KnowledgeExtractionResult result = new KnowledgeExtractionResult();
    result.setDataModels(List.of(dataModel));

    SourceDocumentMetadata source =
        new SourceDocumentMetadata(
            UUID.randomUUID(),
            "bucket",
            "projects/demo/affiliation-rules.csv",
            1L,
            null,
            "hash",
            "document",
            "csv",
            "affiliation-rules.csv");

    service.buildKnowledgeGraph(UUID.randomUUID(), source, null, List.of(result));

    ArgumentCaptor<KnowledgeDataField> captor = ArgumentCaptor.forClass(KnowledgeDataField.class);
    verify(storageService).saveKnowledgeDataField(captor.capture());
    assertThat(captor.getValue().getDataModelId()).isEqualTo(dataModelId);
    assertThat(captor.getValue().getFieldName())
        .isEqualTo("Unique identifier for a product associated with an affiliation.");
    assertThat(captor.getValue().getFieldType()).isEqualTo("unknown");
  }
}
