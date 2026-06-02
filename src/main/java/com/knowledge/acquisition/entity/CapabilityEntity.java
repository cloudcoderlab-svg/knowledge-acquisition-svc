package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/**
 * Represents a business capability extracted from enterprise documentation.
 *
 * <p>Business capabilities describe what an organization does or can do to achieve its goals,
 * independent of how it's implemented. Capabilities are extracted during document-level analysis
 * and used for entity linking during chunk-level extraction to maintain consistency across the
 * knowledge graph.
 *
 * <p>Examples include: Customer Management, Order Processing, Payment Handling, Inventory
 * Management, etc.
 */
@Entity
@jakarta.persistence.Table(name = "knowledge_capabilities", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityEntity {
  /** Unique identifier for this capability. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "capability_id")
  private UUID capabilityId;

  /** Project this capability belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this capability is associated with, if applicable. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this capability is associated with, if applicable. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /** Name of the capability as extracted from the document. */
  @Column(name = "capability_name", nullable = false, columnDefinition = "text")
  private String capabilityName;

  /** Type classification: core, supporting, or enabling capability. */
  @Column(name = "capability_type", columnDefinition = "text")
  private String capabilityType;

  /** Detailed description of what this capability provides. */
  @Column(name = "description", columnDefinition = "text")
  private String description;

  /** The business value or benefit this capability delivers. */
  @Column(name = "business_value", columnDefinition = "text")
  private String businessValue;

  /** Vector embedding for semantic search and similarity matching. */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Reference to the source chunk where this capability was mentioned. */
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
