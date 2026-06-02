package com.knowledge.acquisition.ingestion.parser;

import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * Component for parsing various document formats into plain text using Apache Tika.
 *
 * <p>This parser handles a wide variety of document formats including:
 *
 * <ul>
 *   <li><b>Office Documents:</b> DOCX, DOC, XLSX, XLS, PPTX, PPT
 *   <li><b>PDF Documents:</b> PDF
 *   <li><b>Web Documents:</b> HTML, XML
 *   <li><b>Text Documents:</b> TXT, MD, CSV
 *   <li><b>Email:</b> MSG, EML
 *   <li><b>Images with OCR:</b> PNG, JPG (when Tesseract is available)
 * </ul>
 *
 * <h3>Automatic Format Detection</h3>
 *
 * Apache Tika automatically detects the document format based on content (magic bytes) rather than
 * file extension, making parsing robust and reliable.
 *
 * <h3>Text Extraction</h3>
 *
 * The parser extracts:
 *
 * <ul>
 *   <li>Main document text content
 *   <li>Embedded tables (as text)
 *   <li>Headers and footers
 *   <li>Metadata (title, author, creation date)
 * </ul>
 *
 * <p>For enhanced extraction with structured data (tables, diagrams), use {@link
 * TikaContentExtractor}.
 *
 * <h3>Usage Example</h3>
 *
 * <pre>
 * &#64;Autowired
 * private DocumentParser parser;
 *
 * try (InputStream stream = storage.readAllBytes(blobId)) {
 *   String text = parser.parse(stream);
 * }
 * </pre>
 *
 * @see TikaContentExtractor
 * @see Tika
 */
@Component
public class DocumentParser {

  private final Tika tika = new Tika();

  /**
   * Parses a document from an input stream to plain text.
   *
   * <p>Automatically detects the document format and extracts all text content including headers,
   * footers, and embedded tables.
   *
   * @param inputStream the document input stream to parse
   * @return the extracted plain text content
   * @throws Exception if parsing fails due to unsupported format, corrupted file, or I/O error
   */
  public String parse(InputStream inputStream) throws Exception {
    return tika.parseToString(inputStream);
  }
}
