package com.knowledge.acquisition.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "ingestion_documents", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionDocumentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "document_id")
  private UUID documentId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "source_document_id")
  private UUID sourceDocumentId;

  @Column(name = "document_name", columnDefinition = "text")
  private String documentName;

  @Column(name = "document_type", columnDefinition = "text")
  private String documentType;

  @Column(name = "summary", columnDefinition = "text")
  private String summary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extracted_metadata", columnDefinition = "jsonb")
  private String extractedMetadata;

  @Column(name = "extracted_at")
  private OffsetDateTime extractedAt;

  @Column(name = "chunk_count")
  private Integer chunkCount;

  @Column(name = "extraction_status", columnDefinition = "text")
  private String extractionStatus;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
