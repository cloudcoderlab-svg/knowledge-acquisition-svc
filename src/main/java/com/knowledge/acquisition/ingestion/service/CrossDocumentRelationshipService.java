package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.CrossDocumentAnalysisResult;
import com.knowledge.acquisition.dto.CrossDocumentRelationship;
import com.knowledge.acquisition.dto.DocumentConflict;
import com.knowledge.acquisition.dto.DocumentGap;
import com.knowledge.acquisition.dto.SharedEntity;
import com.knowledge.acquisition.entity.DocumentConflictEntity;
import com.knowledge.acquisition.entity.DocumentGapEntity;
import com.knowledge.acquisition.entity.IngestionDocumentEntity;
import com.knowledge.acquisition.entity.RelationshipEntity;
import com.knowledge.acquisition.entity.SharedEntityEntity;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.JsonResponseUtils;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import com.knowledge.acquisition.ingestion.util.StringUtils;
import com.knowledge.acquisition.repository.DocumentConflictRepository;
import com.knowledge.acquisition.repository.DocumentGapRepository;
import com.knowledge.acquisition.repository.IngestionDocumentRepository;
import com.knowledge.acquisition.repository.RelationshipRepository;
import com.knowledge.acquisition.repository.SharedEntityRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for analyzing and inferring relationships, shared entities, conflicts, and gaps across
 * multiple documents.
 *
 * <p>This service performs comprehensive cross-document consolidation during the knowledge
 * extraction pipeline. It uses AI (Vertex AI Gemini) to analyze document summaries and identify:
 *
 * <ul>
 *   <li><b>Cross-document relationships:</b> Connections between entities in different documents
 *       (e.g., workflows triggering rules, components depending on other components, data models
 *       referencing other entities)
 *   <li><b>Shared entities:</b> Entities appearing in multiple documents with consistency scoring
 *   <li><b>Conflicts:</b> Contradictions, version mismatches, or incompatibilities across documents
 *   <li><b>Gaps:</b> Missing definitions, incomplete specifications, or undefined dependencies
 * </ul>
 *
 * <p><strong>Analysis and Persistence Process:</strong>
 *
 * <ol>
 *   <li>Retrieves all ingested documents for the project
 *   <li>Constructs a prompt with document summaries and metadata
 *   <li>Sends prompt to Vertex AI for comprehensive cross-document analysis
 *   <li>Parses AI response to extract relationships, shared entities, conflicts, and gaps
 *   <li>Filters relationships by confidence threshold (≥0.6)
 *   <li>Persists all findings to the knowledge graph:
 *       <ul>
 *         <li>Relationships → knowledge_relationships table
 *         <li>Shared entities → knowledge_shared_entities table
 *         <li>Conflicts → knowledge_document_conflicts table (status: UNRESOLVED)
 *         <li>Gaps → knowledge_document_gaps table (status: OPEN)
 *       </ul>
 * </ol>
 *
 * <p><strong>Data Quality Benefits:</strong>
 *
 * <ul>
 *   <li>Entity deduplication and consolidation via shared entity tracking
 *   <li>Consistency verification across documentation sets
 *   <li>Conflict detection and resolution workflow
 *   <li>Documentation completeness assessment through gap analysis
 *   <li>Unified knowledge graph spanning all project documents
 * </ul>
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
  private final SharedEntityRepository sharedEntityRepository;
  private final DocumentConflictRepository documentConflictRepository;
  private final DocumentGapRepository documentGapRepository;
  private final VertexAIService vertexAIService;
  private final PromptLoaderUtils promptLoaderUtils;
  private final ObjectMapper mapper;

  /**
   * Analyzes all documents in a project to identify cross-document relationships, shared entities,
   * conflicts, and gaps.
   *
   * <p>This method orchestrates the comprehensive cross-document analysis process by:
   *
   * <ol>
   *   <li>Retrieving all ingested documents for the project
   *   <li>Building a comprehensive prompt with document summaries
   *   <li>Invoking Vertex AI to analyze relationships, shared entities, conflicts, and gaps
   *   <li>Filtering relationships by confidence threshold (≥0.6)
   *   <li>Persisting all findings to the database:
   *       <ul>
   *         <li>Cross-document relationships (with confidence ≥ 0.6)
   *         <li>Shared entities (entities appearing in multiple documents)
   *         <li>Conflicts (contradictions/inconsistencies marked as UNRESOLVED)
   *         <li>Gaps (missing/incomplete information marked as OPEN)
   *       </ul>
   * </ol>
   *
   * <p><strong>Requirements:</strong> Project must have at least 2 documents to perform
   * cross-document analysis. Single-document projects return an empty result.
   *
   * <p><strong>AI Model:</strong> Uses Gemini 2.5 Flash for comprehensive cross-document analysis.
   *
   * <p><strong>Output:</strong> Returns complete analysis result containing relationships, shared
   * entities, architectural insights, conflicts, and gaps. All findings are persisted to the
   * database for subsequent data quality verification and resolution workflows.
   *
   * @param projectId the unique identifier of the project to analyze
   * @param projectName the human-readable name of the project for context
   * @return analysis result containing relationships, shared entities, conflicts, and gaps; or
   *     empty result if analysis fails or insufficient documents
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
      com.knowledge.acquisition.dto.AIResponse aiResponse = vertexAIService.generate(prompt);

      // Parse AI response into structured relationship objects
      CrossDocumentAnalysisResult result =
          mapper.readValue(
              JsonResponseUtils.object(aiResponse.getContent()), CrossDocumentAnalysisResult.class);

      // Filter and persist relationships with sufficient confidence
      if (result.getCrossDocumentRelationships() != null) {
        result.getCrossDocumentRelationships().stream()
            .filter(rel -> rel.getConfidence() == null || rel.getConfidence() >= 0.6)
            .forEach(rel -> saveRelationship(projectId, rel));
      }

      // Persist cross-document insights (shared entities, conflicts, gaps)
      if (result.getCrossDocumentInsights() != null) {
        // Save shared entities with fail-safe handling
        if (result.getCrossDocumentInsights().getSharedEntities() != null) {
          result.getCrossDocumentInsights().getSharedEntities().stream()
              .filter(entity -> entity != null && entity.getEntityName() != null)
              .forEach(entity -> saveSharedEntitySafe(projectId, entity));
        }

        // Save conflicts with fail-safe handling
        if (result.getCrossDocumentInsights().getConflicts() != null) {
          result.getCrossDocumentInsights().getConflicts().stream()
              .filter(
                  conflict ->
                      conflict != null
                          && conflict.getEntityName() != null
                          && conflict.getDescription() != null)
              .forEach(conflict -> saveConflictSafe(projectId, conflict));
        }

        // Save gaps with fail-safe handling
        if (result.getCrossDocumentInsights().getGaps() != null) {
          result.getCrossDocumentInsights().getGaps().stream()
              .filter(
                  gap -> gap != null && gap.getEntityName() != null && gap.getDescription() != null)
              .forEach(gap -> saveGapSafe(projectId, gap));
        }
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
   * Persists a shared entity to the database with fail-safe error handling.
   *
   * <p>Converts the DTO into a SharedEntityEntity and saves it for entity deduplication and
   * consistency tracking across documents. Logs and continues on failure to prevent one bad entity
   * from blocking others.
   *
   * @param projectId the project identifier
   * @param entity the shared entity to save
   */
  private void saveSharedEntitySafe(UUID projectId, SharedEntity entity) {
    try {
      sharedEntityRepository.save(
          SharedEntityEntity.builder()
              .projectId(projectId)
              .entityName(entity.getEntityName())
              .entityType(nullToUnknown(entity.getEntityType()))
              .appearsInDocuments(toJson(entity.getAppearsInDocuments()))
              .consistencyScore(entity.getConsistencyScore())
              .notes(entity.getNotes())
              .build());
    } catch (Exception e) {
      log.warn(
          "Failed to save shared entity '{}' for project {}: {}",
          entity.getEntityName(),
          projectId,
          e.getMessage());
    }
  }

  /**
   * Persists a document conflict to the database with fail-safe error handling.
   *
   * <p>Converts the DTO into a DocumentConflictEntity and saves it for conflict tracking and
   * resolution management. Logs and continues on failure to prevent one bad conflict from blocking
   * others.
   *
   * @param projectId the project identifier
   * @param conflict the document conflict to save
   */
  private void saveConflictSafe(UUID projectId, DocumentConflict conflict) {
    try {
      documentConflictRepository.save(
          DocumentConflictEntity.builder()
              .projectId(projectId)
              .entityName(conflict.getEntityName())
              .entityType(nullToUnknown(conflict.getEntityType()))
              .conflictType(nullToUnknown(conflict.getConflictType()))
              .description(conflict.getDescription())
              .documentsInvolved(toJson(conflict.getDocumentsInvolved()))
              .severity(conflict.getSeverity())
              .resolutionStatus("UNRESOLVED")
              .build());
    } catch (Exception e) {
      log.warn(
          "Failed to save conflict for entity '{}' in project {}: {}",
          conflict.getEntityName(),
          projectId,
          e.getMessage());
    }
  }

  /**
   * Persists a document gap to the database with fail-safe error handling.
   *
   * <p>Converts the DTO into a DocumentGapEntity and saves it for gap tracking and documentation
   * completeness assessment. Logs and continues on failure to prevent one bad gap from blocking
   * others.
   *
   * @param projectId the project identifier
   * @param gap the document gap to save
   */
  private void saveGapSafe(UUID projectId, DocumentGap gap) {
    try {
      documentGapRepository.save(
          DocumentGapEntity.builder()
              .projectId(projectId)
              .entityName(gap.getEntityName())
              .entityType(nullToUnknown(gap.getEntityType()))
              .gapType(nullToUnknown(gap.getGapType()))
              .description(gap.getDescription())
              .referencedIn(toJson(gap.getReferencedIn()))
              .expectedIn(gap.getExpectedIn())
              .resolutionStatus("OPEN")
              .build());
    } catch (Exception e) {
      log.warn(
          "Failed to save gap for entity '{}' in project {}: {}",
          gap.getEntityName(),
          projectId,
          e.getMessage());
    }
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

  /**
   * Serializes an object to JSON string for JSONB column storage.
   *
   * @param value the object to serialize (can be null)
   * @return JSON string representation, or null if value is null or serialization fails
   */
  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      log.warn("Could not serialize value to JSON", e);
      return null;
    }
  }
}
