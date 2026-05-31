package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(
    name = "projects",
    schema = "knowledge",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_projects_name_version",
            columnNames = {"project_name", "version"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "project_name", nullable = false, length = 500)
  private String projectName;

  @Column(name = "version", nullable = false)
  private Integer version;

  @Column(name = "title", length = 500)
  private String title;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "definition", columnDefinition = "text")
  private String definition;

  @Column(name = "definition_embedding")
  @Type(VectorType.class)
  private String definitionEmbedding;

  @Column(name = "summary", columnDefinition = "text")
  private String summary;

  @Column(name = "summary_embedding")
  @Type(VectorType.class)
  private String summaryEmbedding;

  @Column(name = "summary_generated_at")
  private OffsetDateTime summaryGeneratedAt;

  @Column(name = "source_bucket", length = 255)
  private String sourceBucket;

  @Column(name = "gcs_prefix", columnDefinition = "text")
  private String gcsPrefix;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 50)
  private ProjectStatus status;

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
