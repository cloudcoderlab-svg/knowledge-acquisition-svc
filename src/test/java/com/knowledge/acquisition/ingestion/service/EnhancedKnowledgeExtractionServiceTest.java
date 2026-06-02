package com.knowledge.acquisition.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.AIResponse;
import com.knowledge.acquisition.dto.DocumentKnowledge;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EnhancedKnowledgeExtractionServiceTest {

  @Test
  void normalizesCommonLlmJsonShapeMismatches() throws Exception {
    VertexAIService vertexAIService = mock(VertexAIService.class);
    when(vertexAIService.generate(anyString()))
        .thenReturn(
            AIResponse.withoutTokens(
                """
            {
              "dataModels": [
                {
                  "modelName": "Affiliation",
                  "modelType": {"name": "Repository"},
                  "fields": [
                    {"fieldName": "Status", "fieldType": ["String", "Code"]}
                  ]
                }
              ],
              "businessFlows": [
                {
                  "flowName": "Approval",
                  "steps": ["Review", {"stepName": "Approve", "actor": {"role": "AM"}}]
                }
              ]
            }
            """));

    EnhancedKnowledgeExtractionService service =
        new EnhancedKnowledgeExtractionService(
            vertexAIService,
            new PromptLoaderUtils(),
            new XMLPlatformDetector(),
            new ObjectMapper());
    ReflectionTestUtils.setField(service, "enableEnhancedExtraction", true);

    DocumentKnowledge context = new DocumentKnowledge();
    context.setDetectedPlatform("TIBCO_MDM");

    var result = service.extract("<Rulebase/>", context);

    assertThat(result.getDataModels()).hasSize(1);
    assertThat(result.getDataModels().getFirst().getModelType()).contains("Repository");
    assertThat(result.getDataModels().getFirst().getFields().getFirst().getFieldType())
        .isEqualTo("String; Code");
    assertThat(result.getBusinessFlows().getFirst().getSteps())
        .extracting("stepName")
        .containsExactly("Review", "Approve");
    assertThat(result.getBusinessFlows().getFirst().getSteps().get(1).getActor()).contains("AM");
  }
}
