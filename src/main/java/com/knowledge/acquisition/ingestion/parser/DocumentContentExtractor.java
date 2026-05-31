package com.knowledge.acquisition.ingestion.parser;

import com.knowledge.acquisition.dto.DocumentContent;
import java.io.InputStream;

/**
 * Interface for extracting content from documents. Implementations can use different strategies
 * (Tika for text-only, Gemini for multimodal).
 */
public interface DocumentContentExtractor {

  /**
   * Extracts content from the input stream based on file type.
   *
   * @param inputStream the document input stream
   * @param fileType the file type (pdf, docx, txt, etc.)
   * @return DocumentContent containing text, diagrams, tables, and metadata
   * @throws Exception if extraction fails
   */
  DocumentContent extract(InputStream inputStream, String fileType) throws Exception;
}
