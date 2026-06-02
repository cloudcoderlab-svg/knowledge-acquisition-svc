package com.knowledge.acquisition.ingestion.parser;

import com.knowledge.acquisition.dto.DocumentContent;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Apache Tika-based content extractor for documents.
 *
 * <p>This extractor uses Apache Tika to parse documents and extract plain text content. It
 * implements a simple extraction strategy suitable for text-heavy documents where structured data
 * (tables, diagrams) is not critical.
 *
 * <h3>Extraction Strategy</h3>
 *
 * <ul>
 *   <li><b>Text Content:</b> Extracts all text from the document (paragraphs, headers, footers)
 *   <li><b>Tables:</b> Not extracted (returned as empty list)
 *   <li><b>Diagrams:</b> Not extracted (returned as empty list)
 *   <li><b>Metadata:</b> Not extracted (returned as empty map)
 * </ul>
 *
 * <h3>Supported Formats</h3>
 *
 * Supports all formats handled by Apache Tika, including:
 *
 * <ul>
 *   <li>Office documents (DOCX, XLSX, PPTX)
 *   <li>PDF files
 *   <li>HTML/XML files
 *   <li>Plain text files
 *   <li>Email (MSG, EML)
 * </ul>
 *
 * <h3>When to Use</h3>
 *
 * Use this extractor when:
 *
 * <ul>
 *   <li>You only need text content (not tables or diagrams)
 *   <li>You want fast, simple extraction without additional processing
 *   <li>You don't need to preserve document structure
 * </ul>
 *
 * <p>For advanced extraction with table and diagram support, consider implementing a more
 * specialized {@link DocumentContentExtractor}.
 *
 * @see DocumentContentExtractor
 * @see DocumentParser
 * @see DocumentContent
 */
@Component
@RequiredArgsConstructor
public class TikaContentExtractor implements DocumentContentExtractor {

  private final DocumentParser documentParser;

  /**
   * Extracts text content from a document input stream.
   *
   * <p>This implementation extracts only plain text content. Tables, diagrams, and metadata are
   * returned as empty collections.
   *
   * @param inputStream the document input stream
   * @param fileType the file type/extension (not used by this implementation - format is
   *     auto-detected)
   * @return {@link DocumentContent} with text content populated, other fields empty
   * @throws Exception if document parsing fails
   */
  @Override
  public DocumentContent extract(InputStream inputStream, String fileType) throws Exception {
    String textContent = documentParser.parse(inputStream);

    return DocumentContent.builder()
        .textContent(textContent)
        .diagrams(Collections.emptyList())
        .tables(Collections.emptyList())
        .metadata(new HashMap<>())
        .build();
  }
}
