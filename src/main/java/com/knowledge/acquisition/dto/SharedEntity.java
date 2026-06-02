package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an entity that appears in multiple documents across the knowledge base.
 *
 * <p>Shared entities are identified during cross-document analysis to detect when the same
 * component, API, workflow, or business concept is referenced or defined in multiple source
 * documents. This is critical for:
 *
 * <ul>
 *   <li>Entity deduplication and consolidation
 *   <li>Consistency verification across documentation
 *   <li>Traceability and impact analysis
 *   <li>Identifying canonical vs duplicate definitions
 * </ul>
 *
 * <p>The {@code consistencyScore} indicates how consistently the entity is described across
 * documents, with 1.0 meaning identical descriptions and lower scores indicating variations or
 * potential conflicts.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Component "UserService" defined in both architecture.pdf and implementation_guide.docx
 *   <li>API "PaymentGateway" referenced in integration_spec.md and api_catalog.csv
 *   <li>Business term "Customer" used in requirements.docx and data_model.xlsx
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedEntity {
  /**
   * Exact name of the entity as it appears in the source documents.
   *
   * <p>This should use the most common or canonical form if naming variations exist. Example:
   * "OrderService", "PaymentGateway", "Customer".
   */
  private String entityName;

  /**
   * Type classification of the shared entity.
   *
   * <p>Possible values: solution_component, api, business_flow, business_role, business_capability,
   * business_rule, business_term, business_policy, business_decision, business_metric, data_model,
   * integration, deployment_resource, knowledge_note.
   */
  private String entityType;

  /**
   * List of document filenames where this entity appears.
   *
   * <p>Tracks which documents reference or define this entity. Useful for traceability and impact
   * analysis when documents change.
   */
  private List<String> appearsInDocuments;

  /**
   * Consistency score measuring how uniformly the entity is described across documents.
   *
   * <p>Score range and interpretation:
   *
   * <ul>
   *   <li><b>1.0 (Perfect):</b> Identical descriptions across all documents, no contradictions
   *   <li><b>0.8-0.99 (High):</b> Mostly consistent, minor differences in detail level
   *   <li><b>0.6-0.79 (Medium):</b> Some differences but compatible descriptions
   *   <li><b>0.3-0.59 (Low):</b> Significant differences, possible conflicts
   *   <li><b>0.0-0.29 (Conflict):</b> Contradictory descriptions requiring resolution
   * </ul>
   *
   * <p>Low consistency scores should trigger manual review to resolve discrepancies.
   */
  private Double consistencyScore;

  /**
   * Detailed notes about how the entity is described across documents.
   *
   * <p>Describes alignment or discrepancies in definitions, responsibilities, properties, etc.
   * Example: "Consistently described as REST API for payment processing. Architecture doc shows v1,
   * implementation guide references v2 - potential version mismatch."
   */
  private String notes;
}
