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
 * Entity for miscellaneous knowledge notes extracted from enterprise documentation.
 *
 * <p>Stores important observations, insights, decisions, constraints, and contextual information
 * that don't fit into other structured categories. These notes capture critical "why" information
 * about architecture decisions, design rationale, constraints, assumptions, risks, and
 * recommendations.
 *
 * <p>Knowledge notes are particularly valuable during system modernization, technical debt
 * assessment, and architecture review processes.
 */
@Entity
@Table(name = "knowledge_notes", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEntity {
  /** Primary key for the knowledge note. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "note_id")
  private UUID noteId;

  /** Project this note belongs to. */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /** Domain this note is associated with. */
  @Column(name = "domain_id")
  private UUID domainId;

  /** Subdomain this note is associated with. */
  @Column(name = "subdomain_id")
  private UUID subdomainId;

  /**
   * Type classification for this note.
   *
   * <p>Values: architecture, design_decision, constraint, assumption, risk, recommendation.
   */
  @Column(name = "note_type", columnDefinition = "text")
  private String noteType;

  /** The actual note content. */
  @Column(name = "note_text", columnDefinition = "text", nullable = false)
  private String noteText;

  /** Semantic embedding vector for similarity search (768 dimensions). */
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  /** AI extraction confidence score (0.0 to 1.0). */
  @Column(name = "confidence")
  private Double confidence;

  /** Source chunk this note was extracted from. */
  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  /** Additional metadata stored as JSON (tags, related entities, priority, etc.). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  /** Timestamp when this note was created. */
  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  /** Timestamp when this note was last updated. */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
