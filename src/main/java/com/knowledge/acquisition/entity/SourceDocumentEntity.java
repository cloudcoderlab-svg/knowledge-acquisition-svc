package com.knowledge.acquisition.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(
    name = "documents",
    schema = "knowledge",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_documents_project_source_hash",
            columnNames = {"project_id", "source_bucket", "source_object", "content_hash"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocumentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "source_document_id")
  private UUID sourceDocumentId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "document_name", columnDefinition = "text")
  private String documentName;

  @Column(name = "document_type", columnDefinition = "text")
  private String documentType;

  @Column(name = "gcs_url", columnDefinition = "text")
  private String gcsUrl;

  @Column(name = "source_bucket", columnDefinition = "text")
  private String sourceBucket;

  @Column(name = "source_object", columnDefinition = "text")
  private String sourceObject;

  @Column(name = "source_checksum", length = 64)
  private String sourceChecksum;

  @Column(name = "source_generation")
  private Long sourceGeneration;

  @Column(name = "content_hash", length = 64)
  private String contentHash;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "mime_type", columnDefinition = "text")
  private String mimeType;

  @Column(name = "file_type", columnDefinition = "text")
  private String fileType;

  @Column(name = "title", columnDefinition = "text")
  private String title;

  @Column(name = "is_current")
  private Boolean isCurrent;

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
