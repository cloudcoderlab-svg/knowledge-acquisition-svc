package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/**
 * Represents a business role or actor extracted from enterprise documentation.
 *
 * <p>Business roles identify the actors (human or system) that interact with business processes,
 * workflows, and capabilities. Roles are extracted during document-level analysis and used for
 * entity linking during chunk-level extraction to understand who performs what actions.
 *
 * <p>Examples include: Customer, Admin, Approver, Sales Representative, System User, Account
 * Manager, External Partner, etc.
 */
@Entity
@jakarta.persistence.Table(name = "knowledge_roles", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {
  /** Unique identifier for this role. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "role_id")
  private UUID roleId;

  /** Project this role belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this role is associated with, if applicable. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this role is associated with, if applicable. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /** Name of the role as extracted from the document. */
  @Column(name = "role_name", nullable = false, columnDefinition = "text")
  private String roleName;

  /** Type classification: internal, external, system, or partner. */
  @Column(name = "role_type", columnDefinition = "text")
  private String roleType;

  /** Detailed description of this role. */
  @Column(name = "description", columnDefinition = "text")
  private String description;

  /** Key responsibilities and duties of this role. */
  @Column(name = "responsibilities", columnDefinition = "text")
  private String responsibilities;

  /** Vector embedding for semantic search and similarity matching. */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Reference to the source chunk where this role was mentioned. */
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  /** Additional metadata in JSON format. */
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
