package com.knowledge.acquisition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for business rules. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Business rule extracted from documents")
public class BusinessRuleDto {

  @Schema(
      description = "Unique identifier of the rule",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID ruleId;

  @Schema(
      description = "ID of the source document",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private String sourceDocumentId;

  @Schema(description = "Name of the business rule", example = "Customer Age Validation Rule")
  private String ruleName;

  @Schema(
      description = "Type/category of the rule",
      example = "validation",
      allowableValues = {"validation", "calculation", "decision", "constraint"})
  private String ruleType;

  @Schema(description = "Condition or trigger for the rule", example = "IF customer.age < 18")
  private String conditionText;

  @Schema(description = "Outcome or action of the rule", example = "THEN reject application")
  private String outcomeText;

  @Schema(
      description = "Priority level of the rule",
      example = "high",
      allowableValues = {"low", "medium", "high", "critical"})
  private String priority;

  @Schema(
      description = "Confidence score of the extraction (0.0 to 1.0)",
      example = "0.92",
      minimum = "0",
      maximum = "1")
  private Double confidence;
}
