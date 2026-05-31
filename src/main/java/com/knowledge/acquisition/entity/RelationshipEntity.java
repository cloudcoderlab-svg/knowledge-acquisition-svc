package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "knowledge_relationships", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "relationship_id")
  private UUID relationshipId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "source_entity_type", nullable = false, length = 100)
  private String sourceEntityType;

  @Column(name = "source_entity_id")
  private UUID sourceEntityId;

  @Column(name = "source_name", length = 500)
  private String sourceName;

  @Column(name = "target_entity_type", nullable = false, length = 100)
  private String targetEntityType;

  @Column(name = "target_entity_id")
  private UUID targetEntityId;

  @Column(name = "target_name", length = 500)
  private String targetName;

  @Column(name = "relationship_type", nullable = false, length = 100)
  private String relationshipType;

  @Column(name = "relationship_definition", columnDefinition = "text")
  private String relationshipDefinition;

  @Column(name = "business_description", columnDefinition = "text")
  private String businessDescription;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

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
