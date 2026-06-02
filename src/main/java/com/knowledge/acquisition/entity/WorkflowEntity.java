package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "knowledge_workflows", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "workflow_id")
  private UUID workflowId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "workflow_name", nullable = false, columnDefinition = "text")
  private String workflowName;

  @Column(name = "trigger_text", columnDefinition = "text")
  private String triggerText;

  @Column(name = "outcome_text", columnDefinition = "text")
  private String outcomeText;

  @Column(name = "actor", columnDefinition = "text")
  private String actor;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;
}
