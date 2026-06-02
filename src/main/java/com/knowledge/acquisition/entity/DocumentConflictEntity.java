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
 * Entity representing a conflict or inconsistency detected across multiple documents.
 *
 * <p>Document conflicts arise when the same entity is defined or described differently across
 * multiple source documents, leading to contradictions, version mismatches, duplicate
 * responsibilities, or incompatible dependencies. Capturing and tracking conflicts enables:
 *
 * <ul>
 *   <li>Data quality and consistency verification across documentation
 *   <li>Migration planning by resolving legacy vs new system conflicts
 *   <li>Architectural governance by detecting duplicate or overlapping components
 *   <li>Risk assessment by identifying technical debt and integration issues
 *   <li>Documentation improvement by resolving contradictory information
 * </ul>
 *
 * <p>Conflicts should be reviewed and resolved by domain experts or architects to ensure the
 * knowledge base accurately represents the system's true state.
 *
 * <p><b>Conflict Types:</b>
 *
 * <ul>
 *   <li><b>contradictory_definitions:</b> Same entity defined differently across documents
 *   <li><b>version_mismatch:</b> Different versions causing incompatibility
 *   <li><b>duplicate_responsibility:</b> Multiple entities performing the same function
 *   <li><b>incompatible_dependencies:</b> Entities requiring conflicting dependencies
 * </ul>
 *
 * <p><b>Severity Levels:</b>
 *
 * <ul>
 *   <li><b>critical:</b> Prevents system from functioning, must resolve immediately
 *   <li><b>high:</b> Significant inconsistency impacting integration or data quality
 *   <li><b>medium:</b> Moderate inconsistency that should be addressed
 *   <li><b>low:</b> Minor discrepancy, informational only
 * </ul>
 */
@Entity
@Table(name = "knowledge_document_conflicts", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentConflictEntity {
  /** Primary key for the document conflict record. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "conflict_id")
  private UUID conflictId;

  /** Project this conflict belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /**
   * Name of the entity that has conflicting descriptions or definitions.
   *
   * <p>Example: "OrderService", "Customer", "PaymentProcessor".
   */
  @Column(name = "entity_name", nullable = false, columnDefinition = "text")
  private String entityName;

  /**
   * Type of the conflicting entity.
   *
   * <p>Values: solution_component, api, business_flow, business_role, business_capability,
   * business_rule, business_term, business_policy, business_decision, business_metric, data_model,
   * integration, deployment_resource.
   */
  @Column(name = "entity_type", nullable = false, columnDefinition = "text")
  private String entityType;

  /**
   * Category of conflict detected.
   *
   * <p>Values: contradictory_definitions, version_mismatch, duplicate_responsibility,
   * incompatible_dependencies.
   */
  @Column(name = "conflict_type", nullable = false, columnDefinition = "text")
  private String conflictType;

  /**
   * Detailed explanation of the conflict.
   *
   * <p>Describes what is inconsistent, contradictory, or incompatible. Should provide enough detail
   * for a reviewer to understand and resolve the conflict.
   */
  @Column(name = "description", columnDefinition = "text", nullable = false)
  private String description;

  /**
   * List of document filenames involved in this conflict.
   *
   * <p>Stored as JSON array. Typically contains 2+ documents that contain contradictory or
   * incompatible information about the entity.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "documents_involved", columnDefinition = "jsonb", nullable = false)
  private String documentsInvolved;

  /**
   * Severity level of the conflict.
   *
   * <p>Values: critical, high, medium, low. Guides prioritization for conflict resolution efforts.
   */
  @Column(name = "severity", columnDefinition = "text")
  private String severity;

  /**
   * Current status of conflict resolution.
   *
   * <p>Values: UNRESOLVED, INVESTIGATING, RESOLVED. Tracks progress in addressing conflicts.
   */
  @Column(name = "resolution_status", columnDefinition = "text")
  @Builder.Default
  private String resolutionStatus = "UNRESOLVED";

  /**
   * Notes about conflict resolution efforts.
   *
   * <p>Captures investigation findings, resolution decisions, and actions taken to address the
   * conflict.
   */
  @Column(name = "resolution_notes", columnDefinition = "text")
  private String resolutionNotes;

  /** Additional metadata stored as JSON (assignee, due date, related conflicts, etc.). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  /** Timestamp when this conflict was detected and recorded. */
  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  /** Timestamp when this conflict record was last updated. */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
