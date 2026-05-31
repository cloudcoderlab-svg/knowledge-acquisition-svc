package com.knowledge.acquisition.ingestion.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    String response = vertexAIService.generate(prompt);

    ObjectNode result = (ObjectNode) mapper.readTree(JsonResponseUtils.object(response));
    normalizeTextField(result, "businessCapability");
    normalizeTextField(result, "technicalCapability");

    return mapper.treeToValue(result, ClassificationResult.class);
  }

  private void normalizeTextField(ObjectNode result, String fieldName) {
    if (result.get(fieldName) instanceof ArrayNode values) {
      result.put(fieldName, String.join(", ", mapper.convertValue(values, List.class)));
    }
  }
}
