package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/**
 * Represents a business decision point extracted from enterprise documentation.
 *
 * <p>Business decisions capture critical decision points within workflows and processes where
 * choices must be made based on specific criteria. Understanding these decisions is crucial for
 * process automation, workflow design, and business logic implementation. Decisions are extracted
 * during document-level analysis and linked to workflows during chunk-level extraction.
 *
 * <p>Examples include: Credit Approval Decision, Discount Eligibility Check, Route Selection,
 * Priority Assignment, Escalation Determination, etc.
 */
@Entity
@jakarta.persistence.Table(name = "knowledge_decisions", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEntity {
  /** Unique identifier for this decision. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "decision_id")
  private UUID decisionId;

  /** Project this decision belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this decision is associated with, if applicable. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this decision is associated with, if applicable. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /** Workflow this decision is part of, if identified. */
  @Column(name = "workflow_id")
  private UUID workflowId;

  /** Name of the decision as extracted from the document. */
  @Column(name = "decision_name", nullable = false, columnDefinition = "text")
  private String decisionName;

  /** The question or choice that needs to be made. */
  @Column(name = "decision_question", columnDefinition = "text")
  private String decisionQuestion;

  /** Criteria or rules used to make this decision. */
  @Column(name = "decision_criteria", columnDefinition = "text")
  private String decisionCriteria;

  /** Context or circumstances under which this decision is made. */
  @Column(name = "decision_context", columnDefinition = "text")
  private String decisionContext;

  /** Vector embedding for semantic search and similarity matching. */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Reference to the source chunk where this decision was described. */
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  /** Additional metadata including decision options and maker role in JSON format. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  /** Timestamp when this record was created. */
  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  /** Timestamp when this record was last updated. */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
