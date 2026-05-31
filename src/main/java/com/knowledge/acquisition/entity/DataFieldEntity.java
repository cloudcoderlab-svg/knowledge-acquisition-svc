package com.knowledge.acquisition.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity representing a field within a data model in the knowledge graph.
 *
 * <p>This entity maps to the knowledge.knowledge_data_fields table and stores metadata about
 * individual attributes within data structures (e.g., database table columns, API fields, or data
 * object properties). Each field includes name, type, business definition, and validation
 * constraints.
 *
 * <p><strong>Database Constraints:</strong>
 *
 * <ul>
 *   <li>project_id: NOT NULL - must be inherited from parent data model before persistence
 *   <li>data_model_id: NOT NULL - links field to its parent data model
 *   <li>field_name: NOT NULL - every field must have a name
 * </ul>
 *
 * @see com.knowledge.acquisition.dto.KnowledgeDataField
 * @see DataModelEntity
 */
@Entity
@Table(name = "knowledge_data_fields", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFieldEntity {
  /** Primary key - uniquely identifies this data field */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "field_id")
  private UUID fieldId;

  /**
   * Project identifier that owns this data field.
   *
   * <p><strong>NOT NULL constraint</strong> - must be set before saving to database. Typically
   * inherited from parent data model via KnowledgeGraphService.
   */
  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  /**
   * Foreign key to parent data model.
   *
   * <p><strong>NOT NULL constraint</strong> - every field must belong to a data model.
   */
  @Column(name = "data_model_id", nullable = false)
  private UUID dataModelId;

  /**
   * Name of the field (e.g., column name, attribute name, property name).
   *
   * <p><strong>NOT NULL constraint</strong> - every field must have a name. Stored as PostgreSQL
   * TEXT to accommodate long field names.
   */
  @Column(name = "field_name", nullable = false, columnDefinition = "text")
  private String fieldName;

  /**
   * Data type of the field (e.g., String, Integer, Date, UUID).
   *
   * <p>Stored as PostgreSQL TEXT to accommodate complex type definitions.
   */
  @Column(name = "field_type", columnDefinition = "text")
  private String fieldType;

  /**
   * Business description explaining the purpose and meaning of this field.
   *
   * <p>Stored as PostgreSQL TEXT to accommodate detailed explanations.
   */
  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  /**
   * JSON structure containing validation constraints and business rules for this field.
   *
   * <p>May include min/max values, regex patterns, allowed values, required flags, etc. Stored as
   * PostgreSQL JSONB for efficient querying and indexing.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "business_rules", columnDefinition = "jsonb")
  private String businessRules;
}
