package com.knowledge.acquisition.ingestion.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowledge.acquisition.dto.AIResponse;
import com.knowledge.acquisition.dto.ClassificationResult;
import com.knowledge.acquisition.ingestion.util.JsonResponseUtils;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticClassificationService {

  private final VertexAIService vertexAIService;
  private final PromptLoaderUtils promptLoaderUtils;
  private final ObjectMapper mapper;

  public ClassificationResult classify(String content) throws Exception {

    String template = promptLoaderUtils.load("prompt/classification-prompt.txt");

    String prompt = template.replace("{{CONTENT}}", content);

    AIResponse aiResponse = vertexAIService.generate(prompt);

    ObjectNode result =
        (ObjectNode) mapper.readTree(JsonResponseUtils.object(aiResponse.getContent()));
    normalizeTextField(result, "businessCapability");
    normalizeTextField(result, "technicalCapability");

    ClassificationResult classificationResult =
        mapper.treeToValue(result, ClassificationResult.class);
    classificationResult.setTokensConsumed(aiResponse.getTotalTokens());

    return classificationResult;
  }

  private void normalizeTextField(ObjectNode result, String fieldName) {
    if (result.get(fieldName) instanceof ArrayNode values) {
      result.put(fieldName, String.join(", ", mapper.convertValue(values, List.class)));
    }
  }
}
