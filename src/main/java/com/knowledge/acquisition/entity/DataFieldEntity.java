package com.knowledge.acquisition.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_data_fields", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFieldEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "field_id")
  private UUID fieldId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "data_model_id", nullable = false)
  private UUID dataModelId;

  @Column(name = "field_name", nullable = false, length = 255)
  private String fieldName;

  @Column(name = "field_type", length = 100)
  private String fieldType;

  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "business_rules", columnDefinition = "jsonb")
  private String businessRules;
}
