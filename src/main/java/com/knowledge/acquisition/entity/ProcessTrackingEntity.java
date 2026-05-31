package com.knowledge.acquisition.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Entity
@jakarta.persistence.Table(name = "knowledge_engine_processes", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTrackingEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "process_id")
  private UUID processId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "process_type", length = 50)
  private String processType;

  @Column(name = "status", nullable = false, length = 50)
  private String status;

  @Column(name = "total_files")
  private Integer totalFiles;

  @Column(name = "processed_files")
  private Integer processedFiles;

  @Column(name = "failed_files")
  private Integer failedFiles;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "file_list", columnDefinition = "jsonb")
  private String fileList;

  @Column(name = "current_file", columnDefinition = "text")
  private String currentFile;

  @Column(name = "failure_cause", columnDefinition = "text")
  private String failureCause;

  @Column(name = "total_tokens_processed")
  private Long totalTokensProcessed;

  @Column(name = "total_bytes_processed")
  private Long totalBytesProcessed;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
