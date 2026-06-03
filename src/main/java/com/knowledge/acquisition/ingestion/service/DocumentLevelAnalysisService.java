package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.DocumentContent;
import com.knowledge.acquisition.dto.DocumentKnowledge;
import com.knowledge.acquisition.dto.SourceDocumentMetadata;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.JsonResponseUtils;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Performs document-level analysis before chunking. This service analyzes the entire document to
 * extract high-level architectural context, domain classification, and major components/patterns
 * that will inform chunk-level extraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLevelAnalysisService {

  private final VertexAIService vertexAIService;
  private final PromptLoaderUtils promptLoaderUtils;
  private final XMLPlatformDetector platformDetector;
  private final ObjectMapper mapper;

  @Value("${knowledge-engine.extraction.enable-document-level-analysis:true}")
  private boolean enableDocumentLevelAnalysis;

  @Value("${knowledge-engine.parser.max-document-size-mb:50}")
  private int maxDocumentSizeMb;

  /**
   * Analyzes the full document content to extract high-level knowledge. This runs BEFORE chunking
   * to preserve document-level context.
   *
   * @param content the document content (text, diagrams, tables)
   * @param source the source document metadata
   * @return DocumentKnowledge containing overall architecture, domain, components, etc.
   */
  public DocumentKnowledge analyze(DocumentContent content, SourceDocumentMetadata source) {
    return analyze(content, source, null);
  }

  /**
   * Analyzes a document with optional raw source content for platform detection.
   *
   * <p>For XML-like files, {@code rawContent} is preferred for platform detection because parser
   * text can remove tag names and namespaces. The prompt sent to the LLM still uses the parsed
   * {@link DocumentContent}, since that is the normalized text representation used by downstream
   * chunking.
   *
   * @param content parsed document content used for document-level LLM analysis
   * @param source source metadata including file type and object name
   * @param rawContent decoded raw object content, used only for XML platform detection when present
   * @return document-level context with {@code detectedPlatform} populated
   */
  public DocumentKnowledge analyze(
      DocumentContent content, SourceDocumentMetadata source, String rawContent) {
    XMLPlatformDetector.XMLPlatform platform = defaultPlatform(source);
    if (!enableDocumentLevelAnalysis) {
      log.info(
          "Document-level analysis is disabled. Skipping for document: {}", source.objectName());
      return createEmptyDocumentKnowledge(platform);
    }

    try {
      log.info("Starting document-level analysis for document: {}", source.objectName());

      // Get full document text
      String fullText = buildFullDocumentText(content);

      // Check document size limit
      if (isDocumentTooLarge(fullText)) {
        log.warn(
            "Document too large for full analysis ({}MB limit). Using truncated analysis.",
            maxDocumentSizeMb);
        fullText = truncateDocument(fullText);
      }

      // Detect XML platform type for specialized extraction
      platform = detectPlatformFromFileType(source, fullText, rawContent);
      log.info(
          "Detected document/platform type: {} for document: {}", platform, source.objectName());

      // Load platform-specific or generic prompt
      String promptFile = getPromptFileForPlatform(platform);
      String template = promptLoaderUtils.load(promptFile);
      String prompt = template.replace("{{DOCUMENT_CONTENT}}", fullText);

      log.info("Using prompt file: {} for platform: {}", promptFile, platform);

      // Call Vertex AI with the full document
      log.debug("Calling Vertex AI for document-level analysis");
      com.knowledge.acquisition.dto.AIResponse aiResponse = vertexAIService.generate(prompt);

      // Parse JSON response into DocumentKnowledge
      DocumentKnowledge knowledge =
          mapper.readValue(
              JsonResponseUtils.object(aiResponse.getContent()), DocumentKnowledge.class);

      // Store detected platform for downstream use
      knowledge.setDetectedPlatform(platform.name());

      log.info(
          "Document-level analysis completed. Platform: {}, Domain: {}, Components: {}, APIs: {}",
          platform,
          knowledge.getDomain(),
          knowledge.getIdentifiedComponents() != null
              ? knowledge.getIdentifiedComponents().size()
              : 0,
          knowledge.getIdentifiedAPIs() != null ? knowledge.getIdentifiedAPIs().size() : 0);

      return knowledge;

    } catch (Exception e) {
      log.error("Error during document-level analysis for document: {}", source.objectName(), e);
      return createEmptyDocumentKnowledge(platform);
    }
  }

  /** Builds full document text from DocumentContent including diagrams and tables. */
  private String buildFullDocumentText(DocumentContent content) {
    StringBuilder fullText = new StringBuilder();

    // Add main text content
    if (content.getTextContent() != null && !content.getTextContent().isEmpty()) {
      fullText.append(content.getTextContent());
    }

    // Add diagram descriptions
    if (content.getDiagrams() != null && !content.getDiagrams().isEmpty()) {
      fullText.append("\n\n=== DIAGRAMS ===\n");
      content
          .getDiagrams()
          .forEach(
              diagram -> {
                fullText.append("\nDiagram (Page ").append(diagram.getPageNumber()).append("): ");
                fullText.append(diagram.getDescription());
              });
    }

    // Add table data
    if (content.getTables() != null && !content.getTables().isEmpty()) {
      fullText.append("\n\n=== TABLES ===\n");
      content
          .getTables()
          .forEach(
              table -> {
                if (table.getCaption() != null && !table.getCaption().isEmpty()) {
                  fullText.append("\nTable: ").append(table.getCaption());
                }
                fullText.append("\nHeaders: ").append(String.join(" | ", table.getHeaders()));
                table
                    .getRows()
                    .forEach(
                        row -> {
                          fullText.append("\n").append(String.join(" | ", row));
                        });
                fullText.append("\n");
              });
    }

    return fullText.toString();
  }

  private boolean isDocumentTooLarge(String text) {
    // Approximate: 1 char ≈ 1 byte
    long sizeInMb = text.length() / (1024 * 1024);
    return sizeInMb > maxDocumentSizeMb;
  }

  private String truncateDocument(String text) {
    // Keep first 80% and last 20% to preserve beginning and end context
    int maxChars = maxDocumentSizeMb * 1024 * 1024;
    int keepFirst = (int) (maxChars * 0.8);
    int keepLast = (int) (maxChars * 0.2);

    if (text.length() <= maxChars) {
      return text;
    }

    String beginning = text.substring(0, keepFirst);
    String ending = text.substring(text.length() - keepLast);

    return beginning + "\n\n... [DOCUMENT TRUNCATED] ...\n\n" + ending;
  }

  private DocumentKnowledge createEmptyDocumentKnowledge(XMLPlatformDetector.XMLPlatform platform) {
    return DocumentKnowledge.builder()
        .overallArchitecture("Not analyzed")
        .systemSummary("Document-level analysis was skipped or failed")
        .domain("Unknown")
        .subdomain("Unknown")
        .detectedPlatform(platform.name())
        .keyPatterns(java.util.Collections.emptyList())
        .technologies(java.util.Collections.emptyList())
        .identifiedComponents(java.util.Collections.emptyList())
        .identifiedAPIs(java.util.Collections.emptyList())
        .identifiedWorkflows(java.util.Collections.emptyList())
        .build();
  }

  /**
   * Detects platform type based on file type and content.
   *
   * <p>Only XML-like files go through platform-specific XML detection. Raw XML is preferred when
   * available so tags such as {@code Rulebase}, namespaces, and product-specific element names are
   * preserved.
   */
  private XMLPlatformDetector.XMLPlatform detectPlatformFromFileType(
      SourceDocumentMetadata source, String content, String rawContent) {
    // Only detect platform for XML files
    if (isXmlLike(source.fileType())) {
      String detectionContent = rawContent == null || rawContent.isBlank() ? content : rawContent;
      return platformDetector.detectPlatform(detectionContent, source.objectName());
    }
    return defaultPlatform(source);
  }

  /**
   * Gets the appropriate prompt file for the detected platform. Falls back to generic prompt if
   * platform-specific prompt doesn't exist.
   */
  private String getPromptFileForPlatform(XMLPlatformDetector.XMLPlatform platform) {
    // For generic XML or if platform-specific prompt doesn't exist yet,
    // use the generic document-level analysis prompt
    if (platform == XMLPlatformDetector.XMLPlatform.GENERIC_XML) {
      return "prompt/document-level-analysis-prompt.txt";
    }
    if (platform == XMLPlatformDetector.XMLPlatform.GENERIC_DOCUMENT) {
      return "prompt/document-level-analysis-prompt.txt";
    }

    // Try to load platform-specific prompt, fall back to generic if not found
    String platformPromptFile = "prompt/" + platformDetector.getPromptFileName(platform);

    // For now, return generic prompt for all platforms
    // Platform-specific prompts will be created incrementally
    // TODO: Create platform-specific document-level analysis prompts
    log.debug(
        "Platform-specific prompt {} not yet implemented, using generic prompt",
        platformPromptFile);
    return "prompt/document-level-analysis-prompt.txt";
  }

  private XMLPlatformDetector.XMLPlatform defaultPlatform(SourceDocumentMetadata source) {
    String fileType = source.fileType() == null ? "" : source.fileType().toLowerCase();
    return switch (fileType) {
      case "xml", "xpdl", "bpmn", "bpel" -> XMLPlatformDetector.XMLPlatform.GENERIC_XML;
      case "csv", "tsv" -> XMLPlatformDetector.XMLPlatform.CSV_DOCUMENT;
      case "java", "py", "jsp", "php", "sql", "fmb" ->
          XMLPlatformDetector.XMLPlatform.SOURCE_CODE_DOCUMENT;
      case "md", "markdown" -> XMLPlatformDetector.XMLPlatform.MARKDOWN_DOCUMENT;
      case "txt", "log" -> XMLPlatformDetector.XMLPlatform.TEXT_DOCUMENT;
      case "pdf" -> XMLPlatformDetector.XMLPlatform.PDF_DOCUMENT;
      case "doc", "docx" -> XMLPlatformDetector.XMLPlatform.WORD_DOCUMENT;
      case "ppt", "pptx" -> XMLPlatformDetector.XMLPlatform.POWERPOINT_DOCUMENT;
      case "png", "jpg", "jpeg", "gif", "bmp", "webp" ->
          XMLPlatformDetector.XMLPlatform.IMAGE_DOCUMENT;
      case "" -> XMLPlatformDetector.XMLPlatform.UNKNOWN_DOCUMENT;
      default -> XMLPlatformDetector.XMLPlatform.GENERIC_DOCUMENT;
    };
  }

  private boolean isXmlLike(String fileType) {
    return "xml".equalsIgnoreCase(fileType)
        || "xpdl".equalsIgnoreCase(fileType)
        || "bpmn".equalsIgnoreCase(fileType)
        || "bpel".equalsIgnoreCase(fileType);
  }
}
