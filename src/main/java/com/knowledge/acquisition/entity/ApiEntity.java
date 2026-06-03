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

  @Column(name = "source_document_id")
  private UUID sourceDocumentId;

  @Column(name = "api_name", nullable = false, columnDefinition = "text")
  private String apiName;

  @Column(name = "api_type", columnDefinition = "text")
  private String apiType;

  @Column(name = "endpoint_path", columnDefinition = "text")
  private String endpointPath;

  @Column(name = "http_method", columnDefinition = "text")
  private String httpMethod;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "request_schema", columnDefinition = "jsonb")
  private String requestSchema;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response_schema", columnDefinition = "jsonb")
  private String responseSchema;

  @Column(name = "authentication", columnDefinition = "text")
  private String authentication;

  @Column(name = "source_component_name", columnDefinition = "text")
  private String sourceComponentName;

  @Column(name = "business_capability", columnDefinition = "text")
  private String businessCapability;

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

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
