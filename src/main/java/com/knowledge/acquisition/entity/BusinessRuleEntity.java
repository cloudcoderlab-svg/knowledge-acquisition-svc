package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "knowledge_business_rules", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "rule_id")
  private UUID ruleId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "module_id")
  private UUID moduleId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "rule_name", nullable = false, columnDefinition = "text")
  private String ruleName;

  @Column(name = "rule_type", columnDefinition = "text")
  private String ruleType;

  @Column(name = "condition_text", columnDefinition = "text")
  private String conditionText;

  @Column(name = "outcome_text", columnDefinition = "text")
  private String outcomeText;

  @Column(name = "exception_text", columnDefinition = "text")
  private String exceptionText;

  @Column(name = "validation_criteria", columnDefinition = "text")
  private String validationCriteria;

  @Column(name = "priority", columnDefinition = "text")
  private String priority;

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
