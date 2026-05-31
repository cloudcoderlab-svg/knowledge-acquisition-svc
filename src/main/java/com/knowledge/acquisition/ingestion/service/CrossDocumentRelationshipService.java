package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.CrossDocumentAnalysisResult;
import com.knowledge.acquisition.dto.CrossDocumentRelationship;
import com.knowledge.acquisition.entity.IngestionDocumentEntity;
import com.knowledge.acquisition.entity.RelationshipEntity;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.JsonResponseUtils;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import com.knowledge.acquisition.ingestion.util.StringUtils;
import java.util.List;
import java.util.UUID;

import com.knowledge.acquisition.repository.IngestionDocumentRepository;
import com.knowledge.acquisition.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for analyzing and inferring relationships between entities across multiple documents.
 *
 * <p>This service performs cross-document consolidation during the knowledge extraction pipeline.
 * It uses AI (Vertex AI Gemini) to analyze document summaries and identify meaningful relationships
 * that span across file boundaries, such as:
 *
 * <ul>
 *   <li>Workflows that trigger business rules defined in other files
 *   <li>Components that depend on components from different modules
 *   <li>Data models that reference entities defined elsewhere
 * </ul>
 *
 * <p><strong>Consolidation Process:</strong>
 *
 * <ol>
 *   <li>Retrieves all ingested documents for the project
 *   <li>Constructs a prompt with document summaries and metadata
 *   <li>Sends prompt to Vertex AI for relationship analysis
 *   <li>Parses AI response to extract cross-document relationships
 *   <li>Filters relationships by confidence threshold (≥0.6)
 *   <li>Persists validated relationships to the knowledge graph
 * </ol>
 *
 * @author Knowledge Engine Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrossDocumentRelationshipService {
  private final IngestionDocumentRepository documentRepository;
  private final RelationshipRepository relationshipRepository;
  private final VertexAIService vertexAIService;
  private final PromptLoaderUtils promptLoaderUtils;
  private final ObjectMapper mapper;

  /**
   * Analyzes all documents in a project to identify and infer cross-document relationships.
   *
   * <p>This method orchestrates the cross-document consolidation process by:
   *
   * <ol>
   *   <li>Retrieving all ingested documents for the project
   *   <li>Building a comprehensive prompt with document summaries
   *   <li>Invoking Vertex AI to analyze relationships
   *   <li>Filtering relationships by confidence threshold (≥0.6)
   *   <li>Persisting validated relationships to the database
   * </ol>
   *
   * <p><strong>Requirements:</strong> Project must have at least 2 documents to perform
   * cross-document analysis. Single-document projects return an empty result.
   *
   * <p><strong>AI Model:</strong> Uses Gemini 2.5 Flash for relationship inference.
   *
   * @param projectId the unique identifier of the project to analyze
   * @param projectName the human-readable name of the project for context
   * @return analysis result containing discovered relationships, or empty result if analysis fails
   *     or insufficient documents
   */
  @Transactional
  public CrossDocumentAnalysisResult analyzeAndInferRelationships(
      UUID projectId, String projectName) {
    // Retrieve all documents for the project
    List<IngestionDocumentEntity> documents = documentRepository.findByProjectId(projectId);

    // Cross-document analysis requires at least 2 documents
    if (documents == null || documents.size() < 2) {
      return CrossDocumentAnalysisResult.builder().build();
    }

    try {
      // Load the prompt template for cross-document relationship analysis
      String template = promptLoaderUtils.load("prompt/cross-document-relationship-prompt.txt");

      // Populate template with project context and document summaries
      String prompt =
          template
              .replace("{{PROJECT_NAME}}", projectName)
              .replace("{{DOMAIN}}", "Project")
              .replace("{{SUBDOMAIN}}", "Knowledge")
              .replace("{{OVERALL_ARCHITECTURE}}", "Multi-document project analysis")
              .replace("{{DOCUMENT_SUMMARIES}}", summaries(documents));

      // Send prompt to Vertex AI for relationship inference
      String response = vertexAIService.generate(prompt);

      // Parse AI response into structured relationship objects
      CrossDocumentAnalysisResult result =
          mapper.readValue(JsonResponseUtils.object(response), CrossDocumentAnalysisResult.class);

      // Filter and persist relationships with sufficient confidence
      if (result.getCrossDocumentRelationships() != null) {
        result.getCrossDocumentRelationships().stream()
            .filter(rel -> rel.getConfidence() == null || rel.getConfidence() >= 0.6)
            .forEach(rel -> saveRelationship(projectId, rel));
      }
      return result;
    } catch (Exception e) {
      log.error("Cross-document analysis failed for project: {}", projectId, e);
      return CrossDocumentAnalysisResult.builder().build();
    }
  }

  /**
   * Constructs a formatted summary text of all documents for AI analysis.
   *
   * <p>Creates a structured text block containing document metadata (name, type, summary, extracted
   * metadata) that is embedded in the AI prompt to provide context for relationship inference.
   *
   * @param documents list of ingested documents to summarize
   * @return formatted multi-line string with document summaries, safe for AI prompt injection
   */
  private String summaries(List<IngestionDocumentEntity> documents) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < documents.size(); i++) {
      IngestionDocumentEntity doc = documents.get(i);
      builder
          .append("--- Document ")
          .append(i + 1)
          .append(": ")
          .append(StringUtils.safeString(doc.getDocumentName(), "Untitled"))
          .append(" ---\n")
          .append("Type: ")
          .append(StringUtils.safeString(doc.getDocumentType(), "Unknown"))
          .append("\nSummary: ")
          .append(StringUtils.safeString(doc.getSummary(), "No summary available"))
          .append("\nMetadata: ")
          .append(StringUtils.safeString(doc.getExtractedMetadata(), "{}"))
          .append("\n\n");
    }
    return builder.toString();
  }

  /**
   * Persists a cross-document relationship to the database.
   *
   * <p>Converts the DTO relationship into a RelationshipEntity and augments the relationship
   * definition with cross-document source information (source file → target file).
   *
   * @param projectId the project identifier to associate with this relationship
   * @param relationship the cross-document relationship to save
   */
  private void saveRelationship(UUID projectId, CrossDocumentRelationship relationship) {
    relationshipRepository.save(
        RelationshipEntity.builder()
            .projectId(projectId)
            .sourceName(relationship.getSourceName())
            .sourceEntityType(nullToUnknown(relationship.getSourceType()))
            .targetName(relationship.getTargetName())
            .targetEntityType(nullToUnknown(relationship.getTargetType()))
            .relationshipType(nullToUnknown(relationship.getRelationshipType()))
            // Augment context with cross-document file references
            .relationshipDefinition(
                relationship.getContext()
                    + " [Cross-document: "
                    + relationship.getSourceDocument()
                    + " -> "
                    + relationship.getTargetDocument()
                    + "]")
            .confidence(relationship.getConfidence())
            .build());
  }

  /**
   * Converts null or blank strings to "unknown" for safe database storage.
   *
   * @param value the string to check
   * @return the original value if non-null and non-blank, otherwise "unknown"
   */
  private String nullToUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
