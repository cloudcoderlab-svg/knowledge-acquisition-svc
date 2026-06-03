package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for business decision points extracted from enterprise documentation.
 *
 * <p>Represents critical choice points within workflows and processes where actions or outcomes are
 * determined based on specific criteria, rules, or conditions. Understanding decision points is
 * crucial for:
 *
 * <ul>
 *   <li>Process automation and workflow design
 *   <li>Business rules engine configuration
 *   <li>Decision management system implementation
 *   <li>Authority matrix and approval routing
 *   <li>Exception handling and escalation paths
 * </ul>
 *
 * <p>Decision points typically involve evaluating conditions, applying business rules, and choosing
 * between alternative paths or actions. They often map to gateway nodes in BPMN diagrams or
 * decision nodes in process flows.
 *
 * <p>Examples: Credit Approval Decision, Discount Eligibility Check, Route Selection, Priority
 * Assignment, Escalation Determination, Pricing Tier Selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDecision {
  /**
   * Name of the decision as extracted from the document.
   *
   * <p>Should be descriptive of what is being decided. Examples: "Credit Approval", "Discount
   * Eligibility", "Route Selection".
   */
  private String decisionName;

  /**
   * Type classification for this decision.
   *
   * <p>Possible values from source code extraction: approval, routing, prioritization, calculation,
   * eligibility.
   */
  private String decisionType;

  /**
   * The question or choice that needs to be made at this decision point.
   *
   * <p>Phrased as a question that requires an answer. Examples: "Should credit be approved?",
   * "Which shipping route should be used?", "Is customer eligible for discount?".
   */
  private String decisionQuestion;

  /**
   * Criteria, rules, or conditions used to make this decision.
   *
   * <p>Describes the logic, thresholds, or evaluation factors. Examples: "Credit score > 700 and
   * debt-to-income < 40%", "Order value > $100 and customer tier = Premium".
   */
  private String decisionCriteria;

  /**
   * Role or position responsible for making this decision.
   *
   * <p>Should reference role names from document-level analysis for linking. May be a human role
   * (e.g., "Manager", "Approver") or system role (e.g., "Automated Rules Engine").
   */
  @JsonAlias("decisionMaker")
  private String decisionMakerRole;

  /**
   * Available options, outcomes, or paths from this decision.
   *
   * <p>List of possible results or actions that can be taken. Examples: ["Approve", "Deny",
   * "Escalate"], ["Standard Shipping", "Express Shipping", "Same-Day Delivery"].
   */
  @JsonAlias("possibleOutcomes")
  private List<String> decisionOptions;

  /**
   * Context or circumstances under which this decision is made.
   *
   * <p>Provides additional background about when and why this decision occurs. Examples: "During
   * order checkout after payment validation", "At end of month for subscription renewal".
   */
  private String decisionContext;

  /**
   * Workflow or process this decision is part of.
   *
   * <p>Should reference workflow names from document-level analysis for linking. Helps map
   * decisions to process flows.
   */
  private String workflowName;

  /**
   * Sequence number of the workflow step where this decision occurs.
   *
   * <p>Indicates the position of this decision within the workflow. Used to maintain step ordering
   * and understand process flow.
   */
  private Integer workflowStepSequence;

  /**
   * AI extraction confidence score.
   *
   * <p>Range: 0.0 to 1.0, where:
   *
   * <ul>
   *   <li>0.9-1.0 = High confidence (clearly mentioned AND aligns with document context)
   *   <li>0.7-0.89 = Medium confidence (clearly mentioned but not in document context)
   *   <li>0.5-0.69 = Low confidence (implied or partially described)
   * </ul>
   */
  private Double confidence;
}
