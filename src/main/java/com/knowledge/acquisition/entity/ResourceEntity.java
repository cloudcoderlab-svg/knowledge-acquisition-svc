package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_resources", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "resource_name", nullable = false, length = 500)
  private String resourceName;

  @Column(name = "resource_type", length = 100)
  private String resourceType;

  @Column(name = "provider", length = 100)
  private String provider;

  @Column(name = "environment", length = 50)
  private String environment;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "configs", columnDefinition = "jsonb")
  private String configs;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;
}
