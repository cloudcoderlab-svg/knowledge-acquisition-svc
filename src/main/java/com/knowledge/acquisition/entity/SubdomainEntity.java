package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "knowledge_subdomains", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubdomainEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "domain_id", nullable = false)
  private UUID domainId;

  @Column(name = "subdomain_name", nullable = false, columnDefinition = "text")
  private String subdomainName;

  @Column(name = "knowledge", columnDefinition = "text")
  private String knowledge;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
