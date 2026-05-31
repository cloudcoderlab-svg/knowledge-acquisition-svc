package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.knowledge.acquisition.dto.DocumentKnowledge;
import com.knowledge.acquisition.dto.KnowledgeExtractionResult;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.JsonResponseUtils;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import com.knowledge.acquisition.ingestion.util.StringUtils;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Enhanced knowledge extraction service that uses document-level context to improve chunk-level
 * extraction accuracy and entity linking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedKnowledgeExtractionService {
  private static final Set<String> STRING_FIELDS =
      Set.of(
          "apiName",
          "apiType",
          "action",
          "actor",
          "authentication",
          "category",
          "componentName",
          "componentType",
          "conditionText",
          "condition",
          "description",
          "endpointPath",
          "fieldName",
          "fieldType",
          "flowName",
          "hostingModel",
          "httpMethod",
          "inputData",
          "input",
          "inputParameters",
          "integrationName",
          "integrationType",
          "lifecycle",
          "modelName",
          "modelType",
          "nextStep",
          "outcomeText",
          "outcome",
          "outputData",
          "output",
          "outputParameters",
          "owner",
          "provider",
          "resourceName",
          "resourceType",
          "responsibility",
          "ruleName",
          "ruleType",
          "sourceBusinessComponentName",
          "sourceSystem",
          "stepName",
          "targetSystem",
          "technicalDetails",
          "technology",
          "triggerText",
          "workflowName");

  private final VertexAIService vertexAIService;
  private final PromptLoaderUtils promptLoaderUtils;
  private final XMLPlatformDetector platformDetector;
  private final ObjectMapper mapper;

  @Value("${knowledge-engine.extraction.enable-document-level-analysis:true}")
  private boolean enableEnhancedExtraction;

  /**
   * Extracts knowledge from a chunk using document-level context. This provides better entity
   * linking and confidence scores compared to isolated chunk extraction.
   *
   * @param chunkContent the text content of the chunk
   * @param docContext document-level knowledge providing context
   * @return KnowledgeExtractionResult with enhanced entity linking
   */
  public KnowledgeExtractionResult extract(String chunkContent, DocumentKnowledge docContext) {
    if (!enableEnhancedExtraction || docContext == null) {
      log.debug("Enhanced extraction disabled or no document context. Using basic extraction.");
      return extractBasic(chunkContent);
    }

    try {
      // Get platform-specific or generic prompt based on detected platform
      String promptFile = getChunkExtractionPromptFile(docContext);
      String template = promptLoaderUtils.load(promptFile);

      log.debug(
          "Using chunk extraction prompt: {} for platform: {}",
          promptFile,
          docContext.getDetectedPlatform());

      // Populate document context placeholders
      String prompt = populatePromptWithContext(template, chunkContent, docContext);

      // Call Vertex AI
      log.debug("Calling Vertex AI for enhanced chunk extraction");
      com.knowledge.acquisition.dto.AIResponse aiResponse = vertexAIService.generate(prompt);

      // Parse JSON response
      String jsonResponse = JsonResponseUtils.object(aiResponse.getContent());
      KnowledgeExtractionResult result = parseKnowledgeExtractionResponse(jsonResponse);

      log.debug(
          "Enhanced extraction completed. Rules: {}, Components: {}, Relationships: {}",
          result.getBusinessRules() != null ? result.getBusinessRules().size() : 0,
          result.getTechnicalComponents() != null ? result.getTechnicalComponents().size() : 0,
          result.getRelationships() != null ? result.getRelationships().size() : 0);

      return result;

    } catch (Exception e) {
      log.error("Error during enhanced chunk extraction. Falling back to basic extraction.", e);
      return extractBasic(chunkContent);
    }
  }

  /** Populates the prompt template with document context and chunk content. */
  private String populatePromptWithContext(
      String template, String chunkContent, DocumentKnowledge docContext) {

    String prompt = template;

    // Replace document context placeholders
    prompt =
        prompt.replace(
            "{{OVERALL_ARCHITECTURE}}",
            StringUtils.safeString(docContext.getOverallArchitecture(), "Not specified"));
    prompt =
        prompt.replace("{{DOMAIN}}", StringUtils.safeString(docContext.getDomain(), "Unknown"));
    prompt =
        prompt.replace(
            "{{SUBDOMAIN}}", StringUtils.safeString(docContext.getSubdomain(), "Unknown"));
    prompt =
        prompt.replace(
            "{{KEY_PATTERNS}}",
            StringUtils.safeListToString(docContext.getKeyPatterns(), "None identified"));
    prompt =
        prompt.replace(
            "{{TECHNOLOGIES}}",
            StringUtils.safeListToString(docContext.getTechnologies(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_COMPONENTS}}",
            StringUtils.safeListToString(docContext.getIdentifiedComponents(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_APIS}}",
            StringUtils.safeListToString(docContext.getIdentifiedAPIs(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_WORKFLOWS}}",
            StringUtils.safeListToString(docContext.getIdentifiedWorkflows(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_CAPABILITIES}}",
            StringUtils.safeListToString(
                docContext.getIdentifiedCapabilities(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_ROLES}}",
            StringUtils.safeListToString(docContext.getIdentifiedRoles(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_TERMS}}",
            StringUtils.safeListToString(docContext.getIdentifiedTerms(), "None identified"));
    prompt =
        prompt.replace(
            "{{DETECTED_PLATFORM}}",
            StringUtils.safeString(docContext.getDetectedPlatform(), "GENERIC_XML"));

    // Replace chunk content
    prompt = prompt.replace("{{CHUNK_CONTENT}}", chunkContent);

    return prompt;
  }

  /**
   * Fallback to basic extraction without document context. Uses the original
   * knowledge-extraction-prompt.txt
   */
  private KnowledgeExtractionResult extractBasic(String content) {
    try {
      String template = promptLoaderUtils.load("prompt/knowledge-extraction-prompt.txt");
      String prompt = template.replace("{{CONTENT}}", content);
      com.knowledge.acquisition.dto.AIResponse aiResponse = vertexAIService.generate(prompt);
      return parseKnowledgeExtractionResponse(JsonResponseUtils.object(aiResponse.getContent()));
    } catch (Exception e) {
      log.error("Error in basic extraction fallback", e);
      return new KnowledgeExtractionResult();
    }
  }

  /**
   * Gets the appropriate chunk extraction prompt file based on detected platform. Falls back to
   * generic enhanced extraction prompt if platform-specific prompt doesn't exist.
   */
  private String getChunkExtractionPromptFile(DocumentKnowledge docContext) {
    // If no platform detected, use generic enhanced extraction
    if (docContext.getDetectedPlatform() == null || docContext.getDetectedPlatform().isEmpty()) {
      return "prompt/enhanced-chunk-extraction-prompt.txt";
    }

    try {
      // Try to map platform to a specific prompt file
      XMLPlatformDetector.XMLPlatform platform =
          XMLPlatformDetector.XMLPlatform.valueOf(docContext.getDetectedPlatform());

      // Get platform-specific prompt file name
      String platformPromptFile = getPlatformSpecificPromptFile(platform);

      // Check if platform-specific prompt exists by attempting to load it
      // If it exists, use it; otherwise fall back to generic
      try {
        promptLoaderUtils.load(platformPromptFile);
        log.debug("Found platform-specific chunk extraction prompt: {}", platformPromptFile);
        return platformPromptFile;
      } catch (Exception e) {
        log.debug(
            "Platform-specific prompt {} not found, falling back to generic enhanced extraction",
            platformPromptFile);
        return "prompt/enhanced-chunk-extraction-prompt.txt";
      }

    } catch (IllegalArgumentException e) {
      log.warn(
          "Unknown platform type: {}, using generic extraction", docContext.getDetectedPlatform());
      return "prompt/enhanced-chunk-extraction-prompt.txt";
    }
  }

  /**
   * Maps platform enum to the chunk-level extraction prompt file name. These prompts are designed
   * for extracting knowledge from individual chunks with document-level context.
   */
  private String getPlatformSpecificPromptFile(XMLPlatformDetector.XMLPlatform platform) {
    return switch (platform) {
      case TIBCO_MDM -> "prompt/tibco-mdm-extraction-prompt.txt";
      case TIBCO_BPM -> "prompt/tibco-bpm-extraction-prompt.txt";
      case TIBCO_BUSINESSWORKS -> "prompt/tibco-bw-extraction-prompt.txt";
      case INFORMATICA_MDM, SAP_MDM, ORACLE_MDM, IBM_INFOSPHERE_MDM, RELTIO_MDM, SEMARCHY_MDM ->
          "prompt/mdm-xml-extraction-prompt.txt";
      case PEGA_BPM,
              CAMUNDA_BPMN,
              ACTIVITI_BPMN,
              JBPM,
              FLOWABLE_BPMN,
              BPMN_20_GENERIC,
              ORACLE_BPEL,
              IBM_BPM,
              APPIAN,
              SAP_WORKFLOW ->
          "prompt/workflow-xml-extraction-prompt.txt";
      case GENERIC_XML -> "prompt/generic-xml-extraction-prompt.txt";
      case CSV_DOCUMENT -> "prompt/csv-extraction-prompt.txt";
      case MARKDOWN_DOCUMENT,
              TEXT_DOCUMENT,
              PDF_DOCUMENT,
              WORD_DOCUMENT,
              POWERPOINT_DOCUMENT,
              IMAGE_DOCUMENT,
              GENERIC_DOCUMENT,
              UNKNOWN_DOCUMENT ->
          "prompt/enhanced-chunk-extraction-prompt.txt";
    };
  }

  /**
   * Parses JSON with lenient handling of type mismatches. Creates a new ObjectMapper configured to
   * skip invalid values in collections.
   *
   * @param jsonResponse JSON string to parse
   * @return Parsed KnowledgeExtractionResult with potentially missing fields
   */
  private KnowledgeExtractionResult parseLenient(String jsonResponse) throws Exception {
    ObjectMapper lenientMapper = new ObjectMapper();
    // Copy configuration from main mapper
    lenientMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    lenientMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        true);
    lenientMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
        true);
    // Add additional lenient settings
    lenientMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
        true);
    lenientMapper.configure(
        com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    // Try to parse - if it still fails, return empty result
    try {
      JsonNode root = lenientMapper.readTree(jsonResponse);
      normalize(root, null);
      return lenientMapper.treeToValue(root, KnowledgeExtractionResult.class);
    } catch (Exception e) {
      log.warn("Even lenient parsing failed. Returning empty result: {}", e.getMessage());
      return new KnowledgeExtractionResult();
    }
  }

  /**
   * Parses model JSON and falls back to normalization when the model returns unexpected shapes.
   *
   * @param jsonResponse JSON object extracted from the model response
   * @return parsed extraction result, using lenient conversion for recoverable type mismatches
   */
  private KnowledgeExtractionResult parseKnowledgeExtractionResponse(String jsonResponse)
      throws Exception {
    try {
      return mapper.readValue(jsonResponse, KnowledgeExtractionResult.class);
    } catch (JsonProcessingException e) {
      log.warn(
          "JSON deserialization mismatch (AI returned unexpected types). Attempting lenient parsing: {}",
          e.getMessage());
      return parseLenient(jsonResponse);
    }
  }

  /**
   * Normalizes common model response mismatches before DTO binding.
   *
   * <p>Fields declared as strings may arrive as nested objects, and flow steps sometimes arrive as
   * plain strings. This method converts those values to DTO-compatible shapes without discarding
   * the surrounding extraction result.
   */
  private void normalize(JsonNode node, String fieldName) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node instanceof ObjectNode objectNode) {
      Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String childFieldName = entry.getKey();
        JsonNode child = entry.getValue();
        if (STRING_FIELDS.contains(childFieldName) && (child.isObject() || child.isArray())) {
          objectNode.set(childFieldName, TextNode.valueOf(compactText(child)));
        } else {
          normalize(child, childFieldName);
        }
      }
      return;
    }
    if (node instanceof ArrayNode arrayNode) {
      for (int i = 0; i < arrayNode.size(); i++) {
        JsonNode child = arrayNode.get(i);
        if (("steps".equals(fieldName) || "businessFlows".equals(fieldName)) && child.isTextual()) {
          ObjectNode step = mapper.createObjectNode();
          step.put("stepName", child.asText());
          arrayNode.set(i, step);
        } else {
          normalize(child, fieldName);
        }
      }
    }
  }

  /** Converts nested scalar, object, or array nodes into compact text for string DTO fields. */
  private String compactText(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isNumber() || node.isBoolean()) {
      return node.asText();
    }
    if (node.isArray()) {
      StringBuilder text = new StringBuilder();
      for (JsonNode item : node) {
        if (!text.isEmpty()) {
          text.append("; ");
        }
        text.append(compactText(item));
      }
      return text.toString();
    }
    try {
      return mapper.writeValueAsString(node);
    } catch (JsonProcessingException e) {
      return node.toString();
    }
  }
}
