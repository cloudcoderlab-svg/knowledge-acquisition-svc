package com.knowledge.acquisition.ingestion.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.AIResponse;
import com.knowledge.acquisition.dto.DiagramContent;
import com.knowledge.acquisition.dto.DocumentContent;
import com.knowledge.acquisition.dto.TableContent;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Gemini-based multimodal content extractor for documents with visual content. Uses Vertex AI
 * Gemini 2.5 Flash to extract text, diagrams, and tables from PDFs and images.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiMultimodalExtractor implements DocumentContentExtractor {

  private final VertexAIService vertexAIService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String EXTRACTION_PROMPT =
      """
      Analyze this document comprehensively and extract the following:

      1. **Full Text Content**: Extract all text content from the document.

      2. **Diagrams**: Identify and describe all diagrams, charts, flowcharts, and architectural images.
         For each diagram, provide:
         - A detailed description of what the diagram shows
         - The page number where it appears
         - Your confidence level (0.0-1.0)

      3. **Tables**: Extract all tables from the document.
         For each table, provide:
         - Column headers
         - All rows of data
         - Caption or title if present
         - Page number

      Return your response as a JSON object with this structure:
      {
        "textContent": "Full extracted text...",
        "diagrams": [
          {
            "description": "Description of the diagram",
            "pageNumber": 1,
            "confidence": 0.95
          }
        ],
        "tables": [
          {
            "headers": ["Column 1", "Column 2"],
            "rows": [["Row 1 Col 1", "Row 1 Col 2"]],
            "caption": "Table caption",
            "pageNumber": 2
          }
        ]
      }

      Be thorough and capture all visual and textual information.
      """;

  @Override
  public DocumentContent extract(InputStream inputStream, String fileType) throws Exception {
    log.info("Extracting multimodal content using Gemini for file type: {}", fileType);

    // Read the entire input stream into a byte array
    byte[] documentData = readInputStream(inputStream);

    // Determine MIME type
    String mimeType = getMimeType(fileType);

    // Call Gemini with image/document analysis
    AIResponse aiResponse =
        vertexAIService.generateWithImage(EXTRACTION_PROMPT, documentData, mimeType);

    // Parse the JSON response
    return parseGeminiResponse(aiResponse.getContent(), documentData, aiResponse.getTotalTokens());
  }

  private byte[] readInputStream(InputStream inputStream) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[8192];
    int bytesRead;
    while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return buffer.toByteArray();
  }

  private String getMimeType(String fileType) {
    return switch (fileType.toLowerCase()) {
      case "pdf" -> "application/pdf";
      case "png" -> "image/png";
      case "jpg", "jpeg" -> "image/jpeg";
      case "gif" -> "image/gif";
      case "bmp" -> "image/bmp";
      case "webp" -> "image/webp";
      case "doc" -> "application/msword";
      case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      case "ppt" -> "application/vnd.ms-powerpoint";
      case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
      case "xls" -> "application/vnd.ms-excel";
      case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
      default -> "application/octet-stream";
    };
  }

  private DocumentContent parseGeminiResponse(
      String jsonResponse, byte[] documentData, long tokensConsumed) throws Exception {
    try {
      // Clean the response if it's wrapped in markdown code blocks
      String cleanedJson = jsonResponse.trim();
      if (cleanedJson.startsWith("```json")) {
        cleanedJson = cleanedJson.substring(7);
      }
      if (cleanedJson.startsWith("```")) {
        cleanedJson = cleanedJson.substring(3);
      }
      if (cleanedJson.endsWith("```")) {
        cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
      }
      cleanedJson = cleanedJson.trim();

      JsonNode root = objectMapper.readTree(cleanedJson);

      // Extract text content
      String textContent = root.has("textContent") ? root.get("textContent").asText() : "";

      // Extract diagrams
      List<DiagramContent> diagrams = new ArrayList<>();
      if (root.has("diagrams") && root.get("diagrams").isArray()) {
        for (JsonNode diagramNode : root.get("diagrams")) {
          diagrams.add(
              DiagramContent.builder()
                  .description(diagramNode.get("description").asText())
                  .imageData(documentData) // Store reference to original document
                  .pageNumber(diagramNode.get("pageNumber").asInt())
                  .confidence(
                      diagramNode.has("confidence")
                          ? diagramNode.get("confidence").asDouble()
                          : 0.9)
                  .build());
        }
      }

      // Extract tables
      List<TableContent> tables = new ArrayList<>();
      if (root.has("tables") && root.get("tables").isArray()) {
        for (JsonNode tableNode : root.get("tables")) {
          List<String> headers = new ArrayList<>();
          if (tableNode.has("headers") && tableNode.get("headers").isArray()) {
            tableNode.get("headers").forEach(h -> headers.add(h.asText()));
          }

          List<List<String>> rows = new ArrayList<>();
          if (tableNode.has("rows") && tableNode.get("rows").isArray()) {
            for (JsonNode rowNode : tableNode.get("rows")) {
              List<String> row = new ArrayList<>();
              rowNode.forEach(cell -> row.add(cell.asText()));
              rows.add(row);
            }
          }

          tables.add(
              TableContent.builder()
                  .headers(headers)
                  .rows(rows)
                  .caption(tableNode.has("caption") ? tableNode.get("caption").asText() : null)
                  .pageNumber(tableNode.has("pageNumber") ? tableNode.get("pageNumber").asInt() : 0)
                  .build());
        }
      }

      return DocumentContent.builder()
          .textContent(textContent)
          .diagrams(diagrams)
          .tables(tables)
          .metadata(new HashMap<>())
          .tokensConsumed(tokensConsumed)
          .build();

    } catch (Exception e) {
      log.error("Failed to parse Gemini response: {}", jsonResponse, e);
      // Fallback: return just the text content
      return DocumentContent.builder()
          .textContent(jsonResponse)
          .diagrams(new ArrayList<>())
          .tables(new ArrayList<>())
          .metadata(new HashMap<>())
          .tokensConsumed(tokensConsumed)
          .build();
    }
  }
}
