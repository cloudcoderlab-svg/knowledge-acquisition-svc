package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;

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

  @Column(name = "source_system", length = 255)
  private String sourceSystem;

  @Column(name = "target_system", length = 255)
  private String targetSystem;

  @Column(name = "protocol", length = 100)
  private String protocol;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;
}
