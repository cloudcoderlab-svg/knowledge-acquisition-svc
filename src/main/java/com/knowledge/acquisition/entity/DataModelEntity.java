package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_data_models", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataModelEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "data_model_id")
  private UUID dataModelId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "module_id")
  private UUID moduleId;

  @Column(name = "model_name", nullable = false, length = 500)
  private String modelName;

  @Column(name = "model_type", length = 100)
  private String modelType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schema_definition", columnDefinition = "jsonb")
  private String schemaDefinition;

  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;
}
