package com.knowledge.acquisition.entity;

import com.knowledge.acquisition.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Entity for business metrics and KPIs extracted from enterprise documentation.
 *
 * <p>Stores quantifiable performance indicators used to measure business capabilities, workflows,
 * and components. Metrics are extracted from chunks and linked to capabilities, workflows, or
 * components for comprehensive performance tracking.
 *
 * <p>This entity supports the knowledge graph by capturing measurement and monitoring aspects of
 * the business and technical architecture.
 */
@Entity
@Table(name = "knowledge_metrics", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricEntity {
  /** Primary key for the metric. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "metric_id")
  private UUID metricId;

  /** Project this metric belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this metric is associated with. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this metric is associated with. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /** Capability this metric measures (if applicable). */
  @Column(name = "capability_id")
  private UUID capabilityId;

  /** Workflow this metric measures (if applicable). */
  @Column(name = "workflow_id")
  private UUID workflowId;

  /** Component that measures this metric (if applicable). */
  @Column(name = "component_id")
  private UUID componentId;

  /** Name of the metric (e.g., "Order Processing Time", "System Uptime"). */
  @Column(name = "metric_name", nullable = false, columnDefinition = "text")
  private String metricName;

  /** Type classification (performance, quality, financial, operational, customer). */
  @Column(name = "metric_type", columnDefinition = "text")
  private String metricType;

  /** Detailed description of what this metric measures. */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Formula or method used to calculate this metric. */
  @Column(name = "calculation_method", columnDefinition = "TEXT")
  private String calculationMethod;

  /** Target or goal value for this metric. */
  @Column(name = "target_value", columnDefinition = "text")
  private String targetValue;

  /** Semantic embedding vector for similarity search (768 dimensions). */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Source chunk this metric was extracted from. */
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  /** Additional metadata stored as JSON (synonyms, units, thresholds, etc.). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  /** Timestamp when this metric was created. */
  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  /** Timestamp when this metric was last updated. */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
