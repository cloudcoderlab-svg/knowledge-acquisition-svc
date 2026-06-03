package com.knowledge.acquisition.ingestion.parser;

import com.knowledge.acquisition.dto.DocumentContent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Orchestrates document parsing by routing to the appropriate content extractor. Routes multimodal
 * file types (PDF, images, Office docs) to Gemini extractor. Routes text-only file types (including
 * source code files like Java, Python, PHP, JSP) to Tika extractor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserOrchestrator {

  private final GeminiMultimodalExtractor geminiMultimodalExtractor;
  private final TikaContentExtractor tikaContentExtractor;

  @Value("${knowledge-engine.parser.use-multimodal:true}")
  private boolean useMultimodal;

  @Value("${knowledge-engine.parser.multimodal-types:pdf,png,jpg,jpeg,docx,pptx}")
  private String multimodalTypesConfig;

  private static final Set<String> TEXT_TYPES =
      Set.of(
          "txt",
          "text",
          "log",
          "md",
          "markdown",
          "csv",
          "tsv",
          "xml",
          "xpdl",
          "bpmn",
          "bpel",
          "json",
          "yaml",
          "yml",
          "properties",
          "sql",
          "html",
          "htm",
          "java",
          "py",
          "jsp",
          "php",
          "fmb");

  private static final Set<String> VISUAL_AND_OFFICE_TYPES =
      Set.of(
          "pdf", "png", "jpg", "jpeg", "gif", "bmp", "webp", "doc", "docx", "ppt", "pptx", "xls",
          "xlsx");

  private Set<String> multimodalTypes;

  /**
   * Parses a document and extracts its content using the appropriate strategy.
   *
   * @param inputStream the document input stream
   * @param fileType the file type (extension)
   * @return DocumentContent containing text, diagrams, tables, and metadata
   * @throws Exception if parsing fails
   */
  public DocumentContent parseDocument(InputStream inputStream, String fileType) throws Exception {
    String normalizedType = normalize(fileType);
    if (isTextType(normalizedType)) {
      log.info("Using Tika text extraction for file type: {}", normalizedType);
      return tikaContentExtractor.extract(inputStream, normalizedType);
    }

    if (useMultimodal && isMultimodalType(normalizedType)) {
      log.info("Using multimodal LLM extraction for file type: {}", normalizedType);
      return geminiMultimodalExtractor.extract(inputStream, normalizedType);
    }

    log.info("Using Tika fallback extraction for file type: {}", normalizedType);
    return tikaContentExtractor.extract(inputStream, normalizedType);
  }

  private boolean isMultimodalType(String fileType) {
    if (multimodalTypes == null) {
      multimodalTypes =
          Arrays.stream(multimodalTypesConfig.toLowerCase().split(","))
              .map(String::strip)
              .filter(value -> !value.isBlank())
              .collect(Collectors.toSet());
    }
    return VISUAL_AND_OFFICE_TYPES.contains(fileType) || multimodalTypes.contains(fileType);
  }

  private boolean isTextType(String fileType) {
    return TEXT_TYPES.contains(fileType);
  }

  private String normalize(String fileType) {
    return fileType == null ? "unknown" : fileType.toLowerCase().strip();
  }
}
