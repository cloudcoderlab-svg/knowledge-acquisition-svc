package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a conflict or inconsistency detected across multiple documents.
 *
 * <p>Document conflicts arise when the same entity is defined or described differently across
 * multiple source documents, leading to contradictions, version mismatches, duplicate
 * responsibilities, or incompatible dependencies. Identifying these conflicts is critical for:
 *
 * <ul>
 *   <li>Data quality and consistency verification
 *   <li>Migration planning (resolving legacy vs new system conflicts)
 *   <li>Architectural governance (detecting duplicate or overlapping components)
 *   <li>Risk assessment (identifying technical debt and integration issues)
 * </ul>
 *
 * <p>Conflicts should be reviewed and resolved by domain experts or architects to ensure the
 * knowledge base accurately represents the system's true state.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>OrderService described as REST in arch_v1.pdf but gRPC in impl_v2.docx (version mismatch)
 *   <li>PaymentProcessor and PaymentHandler both handle payments (duplicate responsibility)
 *   <li>Customer entity has different schemas in data_model_A.xlsx vs data_model_B.csv
 *       (contradictory definitions)
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentConflict {
  /**
   * Name of the entity that has conflicting descriptions or definitions.
   *
   * <p>Example: "OrderService", "Customer", "PaymentProcessor".
   */
  private String entityName;

  /**
   * Type of the conflicting entity.
   *
   * <p>Possible values: solution_component, api, business_flow, business_role, business_capability,
   * business_rule, business_term, business_policy, business_decision, business_metric, data_model,
   * integration, deployment_resource.
   */
  private String entityType;

  /**
   * Category of conflict detected.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>contradictory_definitions:</b> Same entity defined differently across documents
   *   <li><b>version_mismatch:</b> Different versions causing incompatibility
   *   <li><b>duplicate_responsibility:</b> Multiple entities performing the same function
   *   <li><b>incompatible_dependencies:</b> Entities requiring conflicting dependencies
   * </ul>
   */
  private String conflictType;

  /**
   * Detailed explanation of the conflict.
   *
   * <p>Describes what is inconsistent, contradictory, or incompatible. Should provide enough detail
   * for a reviewer to understand and resolve the conflict.
   *
   * <p>Example: "Document A describes PaymentService v1 using REST APIs and MySQL, while Document B
   * shows PaymentService v2 using gRPC and PostgreSQL. These represent different versions with
   * incompatible communication protocols and data stores."
   */
  private String description;

  /**
   * List of document filenames involved in this conflict.
   *
   * <p>Typically 2+ documents that contain contradictory or incompatible information about the
   * entity.
   */
  private List<String> documentsInvolved;

  /**
   * Severity level of the conflict.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>critical:</b> Conflict prevents system from functioning, must resolve immediately
   *   <li><b>high:</b> Significant inconsistency impacting integration or data quality
   *   <li><b>medium:</b> Moderate inconsistency that should be addressed
   *   <li><b>low:</b> Minor discrepancy, informational only
   * </ul>
   *
   * <p>Severity guides prioritization for conflict resolution efforts.
   */
  private String severity;
}
