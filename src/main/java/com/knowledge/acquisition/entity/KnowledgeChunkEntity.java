package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "knowledge_chunks", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "knowledge_chunk_id")
  private UUID knowledgeChunkId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "chunk_type", length = 100)
  private String chunkType;

  @Column(name = "entity_type", length = 100)
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;
}
