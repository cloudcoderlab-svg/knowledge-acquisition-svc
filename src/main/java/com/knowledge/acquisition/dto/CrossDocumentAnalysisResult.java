package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.*;

/**
 * DTO containing the complete results of cross-document relationship analysis.
 *
 * <p>This DTO encapsulates all findings from analyzing multiple documents together to identify:
 *
 * <ul>
 *   <li><b>Cross-document relationships:</b> Connections between entities in different documents
 *       (e.g., Component A in doc1 calls API B in doc2)
 *   <li><b>Shared entities:</b> Entities appearing in multiple documents with consistency scoring
 *   <li><b>Architectural insights:</b> High-level patterns and principles across the documentation
 *       set
 *   <li><b>Conflicts:</b> Contradictions, version mismatches, or incompatibilities across documents
 *   <li><b>Gaps:</b> Missing definitions, incomplete specifications, or undefined dependencies
 * </ul>
 *
 * <p>Cross-document analysis is performed after individual document extraction to:
 *
 * <ul>
 *   <li>Build a unified knowledge graph spanning all documents
 *   <li>Detect entity duplication and enable consolidation
 *   <li>Verify consistency and identify data quality issues
 *   <li>Assess documentation completeness
 *   <li>Understand system architecture holistically
 * </ul>
 *
 * <p>The analysis leverages AI to compare document summaries, match entities across documents,
 * detect naming variations, identify relationships, and flag conflicts and gaps. Results are used
 * to enrich the knowledge graph with cross-document links and metadata.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>
 * CrossDocumentAnalysisResult result = crossDocAnalysisService.analyze(documentSummaries);
 *
 * // Process relationships
 * for (CrossDocumentRelationship rel : result.getCrossDocumentRelationships()) {
 *   if (rel.getConfidence() >= 0.8) {
 *     knowledgeGraph.addRelationship(rel);
 *   }
 * }
 *
 * // Review conflicts requiring resolution
 * for (DocumentConflict conflict : result.getCrossDocumentInsights().getConflicts()) {
 *   if ("critical".equals(conflict.getSeverity())) {
 *     alertArchitect(conflict);
 *   }
 * }
 *
 * // Track gaps for documentation improvement
 * for (DocumentGap gap : result.getCrossDocumentInsights().getGaps()) {
 *   documentationBacklog.add(gap);
 * }
 * </pre>
 *
 * @see CrossDocumentRelationship
 * @see CrossDocumentInsights
 * @see SharedEntity
 * @see DocumentConflict
 * @see DocumentGap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossDocumentAnalysisResult {
  /**
   * List of relationships discovered between entities across different documents.
   *
   * <p>Each relationship specifies the source entity (in one document), target entity (in another
   * document), relationship type (calls, depends_on, etc.), supporting evidence, and confidence
   * score.
   *
   * <p>Relationships with confidence >= 0.6 are typically included. Higher confidence (>= 0.9)
   * indicates explicit evidence in document summaries.
   */
  private List<CrossDocumentRelationship> crossDocumentRelationships;

  /**
   * High-level insights aggregated from cross-document analysis.
   *
   * <p>Includes shared entities (appearing in multiple documents), architectural insights (how
   * documents fit together), conflicts (contradictions across documents), and gaps (missing or
   * incomplete information).
   *
   * <p>This provides a comprehensive quality and consistency assessment of the documentation set.
   */
  private CrossDocumentInsights crossDocumentInsights;
}
