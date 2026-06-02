package com.knowledge.acquisition.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for business roles and actors extracted from enterprise documentation.
 *
 * <p>Captures both human and system actors that interact with business processes, workflows, and
 * capabilities. This unified DTO represents both organizational roles (e.g., Admin, Approver) and
 * user personas (e.g., Premium Customer, Guest User), eliminating the redundancy between separate
 * businessRoles and businessUsers categories.
 *
 * <p>Business roles are critical for understanding:
 *
 * <ul>
 *   <li>Who performs actions in workflows and processes
 *   <li>Who makes decisions at critical decision points
 *   <li>Who owns or is responsible for capabilities and components
 *   <li>What permissions and access levels are required
 *   <li>User needs and pain points for UX design
 * </ul>
 *
 * <p>Examples: Customer, Admin, Approver, Sales Representative, System User, Account Manager,
 * External Partner, Premium Customer Persona, Guest User.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRole {
  /**
   * Name of the role as extracted from the document.
   *
   * <p>Should match known roles from document-level analysis for entity linking consistency.
   * Examples: "Customer", "Admin", "Approver", "Sales Rep", "Premium Customer".
   */
  private String roleName;

  /**
   * Type classification for this role.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>internal</b> - Employee or internal system user
   *   <li><b>external</b> - Customer, partner, or external user
   *   <li><b>system</b> - Automated system or service account
   *   <li><b>partner</b> - Partner organization user or system
   * </ul>
   */
  private String roleType;

  /**
   * Detailed description of this role's purpose and function.
   *
   * <p>Should explain what this role represents and its scope within the business context.
   */
  private String description;

  /**
   * Key responsibilities, duties, and activities performed by this role.
   *
   * <p>Describes what actions this role is authorized or expected to perform.
   */
  private String responsibilities;

  /**
   * Department or organizational unit this role belongs to.
   *
   * <p>Examples: "Sales", "Customer Service", "IT Operations", "Finance".
   */
  private String department;

  /**
   * Business capability this role supports, owns, or is associated with.
   *
   * <p>Should reference capability names from document-level analysis for linking. Example: "Order
   * Management", "Customer Support".
   */
  private String businessCapability;

  /**
   * User persona or archetype for this role (optional).
   *
   * <p>Used for UX design and user journey mapping. Examples: "Tech-Savvy Mobile User", "First-Time
   * Buyer", "Power User".
   */
  private String persona;

  /**
   * User needs, requirements, and goals (optional).
   *
   * <p>List of what users in this role need to accomplish or expect from the system. Useful for
   * requirements analysis and UX design.
   */
  private List<String> needs;

  /**
   * Pain points, challenges, or frustrations faced by this role (optional).
   *
   * <p>Describes problems users encounter that the system should address. Useful for identifying
   * improvement opportunities.
   */
  private String painPoints;

  /**
   * Workflows or processes this role participates in.
   *
   * <p>Should reference workflow names from document-level analysis for linking. Helps understand
   * role involvement across processes.
   */
  private List<String> workflowsInvolved;

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
