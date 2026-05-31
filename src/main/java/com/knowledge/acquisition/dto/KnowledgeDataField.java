package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a field within a data model.
 *
 * <p>Data fields describe the structure and metadata of individual attributes within a data model
 * (e.g., database table columns, API request/response fields, or data structure members). Each
 * field includes name, type, constraints, and business rules.
 *
 * @see KnowledgeDataModel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDataField {
  /** Unique identifier for this data field */
  private UUID fieldId;

  /**
   * Project identifier that owns this data field. Required for database persistence to satisfy NOT
   * NULL constraint. Inherited from parent data model during save operation.
   */
  private UUID projectId;

  /** Reference to the parent data model containing this field */
  private UUID dataModelId;

  /** Name of the field (e.g., column name, attribute name) */
  private String fieldName;

  /** Data type of the field (e.g., String, Integer, Date) */
  private String fieldType;

  /** Whether this field is mandatory/required */
  private Boolean isRequired;

  /** Business description explaining the purpose and meaning of this field */
  private String description;

  /**
   * Validation constraints and business rules for this field. May include min/max values, regex
   * patterns, allowed values, etc.
   */
  private Map<String, Object> constraints;

  /** Timestamp when this field was created */
  private OffsetDateTime createdAt;
}
