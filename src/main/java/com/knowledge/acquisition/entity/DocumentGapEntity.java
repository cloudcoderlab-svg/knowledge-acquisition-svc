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
 * Entity representing a gap or missing information detected during cross-document analysis.
 *
 * <p>Document gaps occur when entities are referenced but never fully defined, when implementations
 * are missing for documented specifications, when dependencies point to undefined entities, or when
 * specifications are incomplete. Identifying and tracking gaps is important for:
 *
 * <ul>
 *   <li>Completeness verification of documentation sets
 *   <li>Missing implementation detection and tracking
 *   <li>Impact analysis and risk assessment for undefined dependencies
 *   <li>Documentation quality improvement by identifying areas needing elaboration
 *   <li>Migration and modernization planning by understanding system incompleteness
 * </ul>
 *
 * <p>Gaps should be addressed by creating missing documentation, implementing missing components,
 * or explicitly documenting assumptions and constraints that explain why certain information is
 * absent.
 *
 * <p><b>Gap Types:</b>
 *
 * <ul>
 *   <li><b>referenced_not_defined:</b> Entity mentioned but never fully specified
 *   <li><b>missing_implementation:</b> Specification exists but no implementation found
 *   <li><b>undefined_dependency:</b> Entity depends on another that doesn't exist
 *   <li><b>incomplete_specification:</b> Partial definition missing critical details
 * </ul>
 *
 * <p><b>Examples:</b>
 *
 * <ul>
 *   <li>CustomerRepository referenced in OrderService but no schema definition found
 *   <li>PaymentAPI specification exists but no implementation documented
 *   <li>OrderWorkflow depends on ValidationService which is not defined anywhere
 *   <li>Customer entity partially defined with only 3 of 10 expected fields
 * </ul>
 */
@Entity
@Table(name = "knowledge_document_gaps", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGapEntity {
  /** Primary key for the document gap record. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "gap_id")
  private UUID gapId;

  /** Project this gap belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /**
   * Name of the entity that is missing or incompletely defined.
   *
   * <p>Example: "CustomerRepository", "ValidationService", "PaymentGateway".
   */
  @Column(name = "entity_name", nullable = false, columnDefinition = "text")
  private String entityName;

  /**
   * Type of the missing or incomplete entity.
   *
   * <p>Values: solution_component, api, business_flow, business_role, business_capability,
   * business_rule, business_term, business_policy, business_decision, business_metric, data_model,
   * integration, deployment_resource.
   */
  @Column(name = "entity_type", nullable = false, columnDefinition = "text")
  private String entityType;

  /**
   * Category of gap detected.
   *
   * <p>Values: referenced_not_defined, missing_implementation, undefined_dependency,
   * incomplete_specification.
   */
  @Column(name = "gap_type", nullable = false, columnDefinition = "text")
  private String gapType;

  /**
   * Detailed explanation of what is missing or incomplete.
   *
   * <p>Describes the gap sufficiently for documentation authors or implementers to understand what
   * needs to be added or completed.
   */
  @Column(name = "description", columnDefinition = "text", nullable = false)
  private String description;

  /**
   * List of document filenames that reference this missing entity.
   *
   * <p>Stored as JSON array. Shows where the gap was detected - which documents mention or depend
   * on the missing entity.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "referenced_in", columnDefinition = "jsonb")
  private String referencedIn;

  /**
   * Expected location or document type where this entity should be defined.
   *
   * <p>Provides guidance on where to look for missing information or where new documentation should
   * be created. Example: "Data_Models.xlsx", "Database schema document", "API specification".
   */
  @Column(name = "expected_in", columnDefinition = "text")
  private String expectedIn;

  /**
   * Current status of gap resolution.
   *
   * <p>Values: OPEN, IN_PROGRESS, CLOSED. Tracks progress in addressing gaps.
   */
  @Column(name = "resolution_status", columnDefinition = "text")
  @Builder.Default
  private String resolutionStatus = "OPEN";

  /**
   * Notes about gap resolution efforts.
   *
   * <p>Captures information about documentation created, implementations added, or explanations for
   * why the gap cannot be filled.
   */
  @Column(name = "resolution_notes", columnDefinition = "text")
  private String resolutionNotes;

  /** Additional metadata stored as JSON (priority, assignee, due date, related gaps, etc.). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  /** Timestamp when this gap was detected and recorded. */
  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  /** Timestamp when this gap record was last updated. */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
