package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_facts", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeFactEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "fact_id")
  private UUID factId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "parent_fact_id")
  private UUID parentFactId;

  @Column(name = "root_fact_id")
  private UUID rootFactId;

  @Column(name = "fact_type", nullable = false, columnDefinition = "text")
  private String factType;

  @Column(name = "fact_key", columnDefinition = "text")
  private String factKey;

  @Column(name = "title", nullable = false, columnDefinition = "text")
  private String title;

  @Column(name = "summary", columnDefinition = "text")
  private String summary;

  @Column(name = "content", columnDefinition = "text")
  private String content;

  @Column(name = "priority", columnDefinition = "text")
  private String priority;

  @Column(name = "source_entity_type", columnDefinition = "text")
  private String sourceEntityType;

  @Column(name = "source_entity_id")
  private UUID sourceEntityId;

  @Column(name = "source_rule_id")
  private UUID sourceRuleId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attributes", columnDefinition = "jsonb")
  private String attributes;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
