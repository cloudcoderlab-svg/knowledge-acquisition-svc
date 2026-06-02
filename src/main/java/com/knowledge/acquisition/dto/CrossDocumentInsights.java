package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing high-level insights from cross-document analysis.
 *
 * <p>This DTO aggregates findings from analyzing multiple documents together, including:
 *
 * <ul>
 *   <li><b>Shared entities:</b> Entities appearing in multiple documents
 *   <li><b>Architectural insights:</b> How documents fit together in the overall system
 *       architecture
 *   <li><b>Conflicts:</b> Contradictions or inconsistencies across documents
 *   <li><b>Gaps:</b> Missing or incomplete information detected
 * </ul>
 *
 * <p>These insights enable:
 *
 * <ul>
 *   <li>Entity consolidation and deduplication
 *   <li>Consistency verification and quality assurance
 *   <li>Documentation completeness assessment
 *   <li>Architectural understanding and validation
 *   <li>Risk identification (conflicts, gaps, missing implementations)
 * </ul>
 *
 * <p>Cross-document analysis is typically performed after individual document extraction to build a
 * unified knowledge graph that spans the entire documentation set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossDocumentInsights {
  /**
   * Entities that appear in multiple documents across the knowledge base.
   *
   * <p>Each shared entity includes consistency scoring to identify alignment or discrepancies in
   * how it's described across documents.
   */
  private List<SharedEntity> sharedEntities;

  /**
   * High-level architectural insights synthesized from multiple documents.
   *
   * <p>Describes how the documents fit together architecturally: patterns identified
   * (microservices, layered, event-driven), integration approaches, architectural principles,
   * separation of concerns, etc.
   *
   * <p>Example: "Three-tier architecture with clear separation: presentation layer (UI docs),
   * business logic layer (service specs), and data layer (DB schemas). Integration primarily
   * through REST APIs with event-driven async processing for long-running workflows."
   */
  private String architecturalInsights;

  /**
   * Conflicts or inconsistencies detected across documents.
   *
   * <p>Includes contradictory definitions, version mismatches, duplicate responsibilities, and
   * incompatible dependencies. Each conflict should be reviewed and resolved to ensure data
   * quality.
   */
  private List<DocumentConflict> conflicts;

  /**
   * Gaps or missing information detected during cross-document analysis.
   *
   * <p>Includes entities referenced but not defined, missing implementations, undefined
   * dependencies, and incomplete specifications. Gaps should be addressed by creating missing
   * documentation or implementations.
   */
  private List<DocumentGap> gaps;
}
