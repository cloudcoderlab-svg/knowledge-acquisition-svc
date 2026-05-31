package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_workflow_steps", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "step_id")
  private UUID stepId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "sequence_number")
  private Integer sequenceNumber;

  @Column(name = "actor", length = 255)
  private String actor;

  @Column(name = "action_text", columnDefinition = "text")
  private String actionText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_parameters", columnDefinition = "jsonb")
  private String inputParameters;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "output_parameters", columnDefinition = "jsonb")
  private String outputParameters;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "business_rules", columnDefinition = "jsonb")
  private String businessRules;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;
}
