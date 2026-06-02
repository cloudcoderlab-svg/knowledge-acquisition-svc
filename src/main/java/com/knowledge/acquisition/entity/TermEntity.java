package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/**
 * Represents a business term or domain vocabulary extracted from enterprise documentation.
 *
 * <p>Business terms capture the key vocabulary and domain-specific language used within a business
 * context. These terms form the ubiquitous language of the domain and help maintain consistency
 * across documentation and implementations. Terms are extracted during document-level analysis and
 * used for entity linking during chunk-level extraction.
 *
 * <p>Examples include: Order, Customer, SKU, Contract, Policy, Premium Customer, Discount Tier,
 * Account Balance, etc.
 */
@Entity
@jakarta.persistence.Table(name = "knowledge_terms", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermEntity {
  /** Unique identifier for this term. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "term_id")
  private UUID termId;

  /** Project this term belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this term is associated with, if applicable. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this term is associated with, if applicable. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /** Name of the term as extracted from the document. */
  @Column(name = "term_name", nullable = false, columnDefinition = "text")
  private String termName;

  /** Category: domain, process, metric, role, or entity. */
  @Column(name = "category", columnDefinition = "text")
  private String category;

  /** Definition of the term from a business perspective. */
  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  /** Definition of the term from a technical implementation perspective. */
  @Column(name = "technical_definition", columnDefinition = "text")
  private String technicalDefinition;

  /** Vector embedding for semantic search and similarity matching. */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Reference to the source chunk where this term was defined. */
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  /** Additional metadata including synonyms and related terms in JSON format. */
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
