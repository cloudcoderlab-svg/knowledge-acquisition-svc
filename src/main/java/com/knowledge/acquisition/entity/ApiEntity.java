package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_apis", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "api_id")
  private UUID apiId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "api_name", nullable = false, columnDefinition = "text")
  private String apiName;

  @Column(name = "api_type", columnDefinition = "text")
  private String apiType;

  @Column(name = "endpoint_path", columnDefinition = "text")
  private String endpointPath;

  @Column(name = "http_method", columnDefinition = "text")
  private String httpMethod;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "request_schema", columnDefinition = "jsonb")
  private String requestSchema;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response_schema", columnDefinition = "jsonb")
  private String responseSchema;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;
}
