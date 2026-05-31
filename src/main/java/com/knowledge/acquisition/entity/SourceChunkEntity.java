package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "knowledge_source_chunks", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceChunkEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "document_id")
  private UUID documentId;

  @Column(name = "source_document_id")
  private UUID sourceDocumentId;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "chunk_index")
  private Integer chunkIndex;

  @Column(name = "char_start")
  private Long charStart;

  @Column(name = "char_end")
  private Long charEnd;

  @Column(name = "context_summary", columnDefinition = "text")
  private String contextSummary;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  @Column(name = "embedding_status", columnDefinition = "text")
  private String embeddingStatus;

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
