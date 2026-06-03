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
@Table(name = "knowledge_integrations", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "integration_id")
  private UUID integrationId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "source_document_id")
  private UUID sourceDocumentId;

  @Column(name = "integration_name", columnDefinition = "text")
  private String integrationName;

  @Column(name = "integration_type", columnDefinition = "text")
  private String integrationType;

  @Column(name = "source_system", columnDefinition = "text")
  private String sourceSystem;

  @Column(name = "target_system", columnDefinition = "text")
  private String targetSystem;

  @Column(name = "protocol", columnDefinition = "text")
  private String protocol;

  @Column(name = "data_exchanged", columnDefinition = "text")
  private String dataExchanged;

  @Column(name = "description", columnDefinition = "text")
  private String description;

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
