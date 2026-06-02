package com.knowledge.acquisition.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing an entity that appears in multiple documents across the knowledge base.
 *
 * <p>Shared entities are identified during cross-document analysis to detect when the same
 * component, API, workflow, or business concept is referenced or defined in multiple source
 * documents. This entity supports:
 *
 * <ul>
 *   <li>Entity deduplication and consolidation across documents
 *   <li>Consistency verification and scoring across multiple definitions
 *   <li>Traceability - identifying all documents that reference an entity
 *   <li>Impact analysis - understanding which documents are affected by entity changes
 *   <li>Canonical definition identification - finding the authoritative source
 * </ul>
 *
 * <p>The {@code consistencyScore} measures how uniformly the entity is described across documents,
 * with 1.0 indicating perfect alignment and lower scores indicating variations or conflicts
 * requiring review.
 *
 * <p><b>Consistency Score Interpretation:</b>
 *
 * <ul>
 *   <li><b>1.0 (Perfect):</b> Identical descriptions, no contradictions
 *   <li><b>0.8-0.99 (High):</b> Mostly consistent, minor detail differences
 *   <li><b>0.6-0.79 (Medium):</b> Some differences but compatible
 *   <li><b>0.3-0.59 (Low):</b> Significant differences, possible conflicts
 *   <li><b>0.0-0.29 (Conflict):</b> Contradictory descriptions requiring resolution
 * </ul>
 */
@Entity
@Table(name = "knowledge_shared_entities", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedEntityEntity {
  /** Primary key for the shared entity record. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "shared_entity_id")
  private UUID sharedEntityId;

  /** Project this shared entity belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /**
   * Exact name of the entity as it appears in source documents.
   *
   * <p>Uses the most common or canonical form if naming variations exist. Example: "OrderService",
   * "PaymentGateway", "Customer".
   */
  @Column(name = "entity_name", nullable = false, columnDefinition = "text")
  private String entityName;

  /**
   * Type classification of the shared entity.
   *
   * <p>Values: solution_component, api, business_flow, business_role, business_capability,
   * business_rule, business_term, business_policy, business_decision, business_metric, data_model,
   * integration, deployment_resource, knowledge_note.
   */
  @Column(name = "entity_type", nullable = false, columnDefinition = "text")
  private String entityType;

  /**
   * List of document filenames where this entity appears.
   *
   * <p>Stored as JSON array. Tracks which documents reference or define this entity for
   * traceability and impact analysis.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "appears_in_documents", columnDefinition = "jsonb")
  private String appearsInDocuments;

  /**
   * Consistency score measuring how uniformly the entity is described across documents.
   *
   * <p>Range: 0.0 to 1.0. Higher scores indicate better consistency. Scores below 0.6 should
   * trigger manual review to resolve discrepancies.
   */
  @Column(name = "consistency_score")
  private Double consistencyScore;

  /**
   * Detailed notes about how the entity is described across documents.
   *
   * <p>Describes alignment or discrepancies in definitions, responsibilities, properties, etc.
   * Example: "Consistently described as REST API for payment processing. Architecture doc shows v1,
   * implementation guide references v2 - potential version mismatch."
   */
  @Column(name = "notes", columnDefinition = "text")
  private String notes;

  /** Additional metadata stored as JSON (version info, aliases, related entities, etc.). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  /** Timestamp when this shared entity record was created. */
  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  /** Timestamp when this shared entity record was last updated. */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
