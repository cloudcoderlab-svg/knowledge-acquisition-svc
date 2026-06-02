package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/**
 * Represents a business policy or compliance requirement extracted from enterprise documentation.
 *
 * <p>Business policies define the rules, constraints, and compliance requirements that govern how
 * an organization operates. These can be operational policies, security policies, regulatory
 * requirements, or governance rules. Policies are extracted during document-level analysis and used
 * to understand compliance and regulatory context.
 *
 * <p>Examples include: Data Retention Policy, GDPR Compliance, Approval Policy, Security Policy,
 * Access Control Policy, etc.
 */
@Entity
@jakarta.persistence.Table(name = "knowledge_policies", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEntity {
  /** Unique identifier for this policy. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "policy_id")
  private UUID policyId;

  /** Project this policy belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this policy is associated with, if applicable. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this policy is associated with, if applicable. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /** Name of the policy as extracted from the document. */
  @Column(name = "policy_name", nullable = false, columnDefinition = "text")
  private String policyName;

  /** Type classification: compliance, operational, security, quality, or governance. */
  @Column(name = "policy_type", columnDefinition = "text")
  private String policyType;

  /** Detailed description of what this policy requires or governs. */
  @Column(name = "description", columnDefinition = "text")
  private String description;

  /** The business reason or justification for this policy. */
  @Column(name = "business_rationale", columnDefinition = "text")
  private String businessRationale;

  /** Regulatory framework or requirement this policy addresses (e.g., GDPR, SOX, HIPAA). */
  @Column(name = "regulatory_requirement", columnDefinition = "text")
  private String regulatoryRequirement;

  /** Vector embedding for semantic search and similarity matching. */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Reference to the source chunk where this policy was mentioned. */
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  /** Additional metadata including enforcement level and effective date in JSON format. */
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
