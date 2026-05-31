package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "knowledge_modules", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "module_id")
  private UUID moduleId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "subdomain_id")
  private UUID subdomainId;

  @Column(name = "module_name", nullable = false, columnDefinition = "text")
  private String moduleName;

  @Column(name = "module_type", columnDefinition = "text")
  private String moduleType;

  @Column(name = "knowledge", columnDefinition = "text")
  private String knowledge;

  @Column(name = "responsibility", columnDefinition = "text")
  private String responsibility;

  @Column(name = "technology", columnDefinition = "text")
  private String technology;

  @Column(name = "owner", columnDefinition = "text")
  private String owner;

  @Column(name = "lifecycle", columnDefinition = "text")
  private String lifecycle;

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
