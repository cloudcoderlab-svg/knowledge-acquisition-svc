package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a gap or missing information detected during cross-document analysis.
 *
 * <p>Document gaps occur when entities are referenced but never fully defined, when implementations
 * are missing for documented specifications, when dependencies point to undefined entities, or when
 * specifications are incomplete. Identifying gaps is important for:
 *
 * <ul>
 *   <li>Completeness verification of documentation
 *   <li>Missing implementation detection
 *   <li>Impact analysis and risk assessment
 *   <li>Documentation quality improvement
 *   <li>Migration and modernization planning
 * </ul>
 *
 * <p>Gaps should be addressed by creating missing documentation, implementing missing components,
 * or explicitly documenting assumptions and constraints.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>CustomerRepository referenced in OrderService but no schema definition found
 *   <li>PaymentAPI specification exists but no implementation documented
 *   <li>OrderWorkflow depends on ValidationService which is not defined anywhere
 *   <li>Customer entity partially defined with only 3 of 10 expected fields
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGap {
  /**
   * Name of the entity that is missing or incompletely defined.
   *
   * <p>Example: "CustomerRepository", "ValidationService", "PaymentGateway".
   */
  private String entityName;

  /**
   * Type of the missing or incomplete entity.
   *
   * <p>Possible values: solution_component, api, business_flow, business_role, business_capability,
   * business_rule, business_term, business_policy, business_decision, business_metric, data_model,
   * integration, deployment_resource.
   */
  private String entityType;

  /**
   * Category of gap detected.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>referenced_not_defined:</b> Entity mentioned but never fully specified
   *   <li><b>missing_implementation:</b> Specification exists but no implementation found
   *   <li><b>undefined_dependency:</b> Entity depends on another that doesn't exist
   *   <li><b>incomplete_specification:</b> Partial definition missing critical details
   * </ul>
   */
  private String gapType;

  /**
   * Detailed explanation of what is missing or incomplete.
   *
   * <p>Describes the gap sufficiently for documentation authors or implementers to understand what
   * needs to be added or completed.
   *
   * <p>Example: "CustomerRepository is referenced by OrderService for fetching customer data, but
   * no schema definition, API specification, or implementation documentation exists in any analyzed
   * document. Expected to find at least a data model definition or database schema."
   */
  private String description;

  /**
   * List of document filenames that reference this missing entity.
   *
   * <p>Shows where the gap was detected - which documents mention or depend on the missing entity.
   */
  private List<String> referencedIn;

  /**
   * Expected location or document type where this entity should be defined.
   *
   * <p>Provides guidance on where to look for missing information or where new documentation should
   * be created. Example: "Data_Models.xlsx", "Database schema document", "API specification".
   */
  private String expectedIn;
}
