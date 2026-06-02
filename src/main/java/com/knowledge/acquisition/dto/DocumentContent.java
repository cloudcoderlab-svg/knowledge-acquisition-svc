package com.knowledge.acquisition.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Data transfer object representing extracted content from a document.
 *
 * <p>This DTO encapsulates all content extracted from a source document including text, structured
 * data (tables), visual elements (diagrams), and metadata.
 *
 * <h3>Content Types</h3>
 *
 * <ul>
 *   <li><b>Text Content:</b> Plain text extracted from the document body
 *   <li><b>Diagrams:</b> Visual elements like flowcharts, architecture diagrams, and images with
 *       descriptions
 *   <li><b>Tables:</b> Structured tabular data with headers and rows
 *   <li><b>Metadata:</b> Document properties (title, author, creation date, file type, etc.)
 * </ul>
 *
 * <h3>Extraction Flow</h3>
 *
 * <pre>
 * Document (PDF, DOCX, etc.)
 *   → DocumentContentExtractor
 *     → DocumentContent (this DTO)
 *       → textContent, diagrams, tables, metadata
 * </pre>
 *
 * @see DiagramContent
 * @see TableContent
 * @see com.knowledge.acquisition.ingestion.parser.DocumentContentExtractor
 */
@Data
@Builder
public class DocumentContent {
  /** Plain text content extracted from the document (paragraphs, headers, footers). */
  private String textContent;

  /**
   * List of diagrams/images extracted from the document with descriptions.
   *
   * <p>May be empty if no diagrams found or extractor doesn't support diagram extraction.
   */
  private List<DiagramContent> diagrams;

  /**
   * List of tables extracted from the document with structured data.
   *
   * <p>May be empty if no tables found or extractor doesn't support table extraction.
   */
  private List<TableContent> tables;

  /**
   * Document metadata (title, author, creation date, file type, etc.).
   *
   * <p>May be empty if no metadata available or extractor doesn't support metadata extraction.
   */
  private Map<String, Object> metadata;

  /**
   * Total AI model tokens consumed during content extraction.
   *
   * <p>Used for cost tracking and optimization. May be null if extraction didn't use AI models.
   */
  private Long tokensConsumed;
}
