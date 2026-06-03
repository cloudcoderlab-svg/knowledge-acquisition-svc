package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for business policies and compliance requirements extracted from enterprise documentation.
 *
 * <p>Represents organizational policies, regulatory requirements, governance rules, and compliance
 * mandates that constrain or guide how the business operates. Policies are high-level directives
 * that are typically implemented through business rules, workflows, and operational procedures.
 *
 * <p>Understanding policies is critical for:
 *
 * <ul>
 *   <li>Compliance management and regulatory adherence
 *   <li>Risk mitigation and governance frameworks
 *   <li>Security and data protection requirements
 *   <li>Quality assurance and operational standards
 *   <li>Audit trail and documentation requirements
 *   <li>Business continuity and disaster recovery planning
 * </ul>
 *
 * <p>Policies differ from business rules in that they are broader, more strategic, and often
 * mandated by external regulatory bodies or internal governance committees. Business rules are the
 * tactical implementation of policies.
 *
 * <p>Examples: Data Retention Policy, GDPR Compliance Policy, Password Security Policy, Code Review
 * Policy, SLA Compliance Policy, PCI-DSS Payment Security Policy, Backup and Recovery Policy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessPolicy {
  /**
   * Name of the policy as extracted from the document.
   *
   * <p>Should be descriptive and match known policy names from document-level analysis. Examples:
   * "Data Retention Policy", "GDPR Compliance Policy", "Password Security Policy".
   */
  private String policyName;

  /**
   * Category classification for this policy.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>compliance</b> - Regulatory or legal compliance requirement
   *   <li><b>operational</b> - Internal operational standard or procedure
   *   <li><b>security</b> - Security or access control policy
   *   <li><b>quality</b> - Quality assurance or standards policy
   *   <li><b>governance</b> - Governance or oversight policy
   * </ul>
   */
  private String policyType;

  /**
   * Detailed description of what this policy requires or mandates.
   *
   * <p>Explains the policy's scope, requirements, and what behaviors or outcomes it demands. Should
   * be comprehensive enough to understand the policy's intent without reading the source document.
   */
  private String description;

  /**
   * Business reason or justification for this policy.
   *
   * <p>Explains WHY this policy exists - the business drivers, risks being mitigated, or value
   * being protected. Example: "Protects customer privacy and ensures GDPR compliance to avoid
   * regulatory fines."
   */
  private String businessRationale;

  /**
   * Regulatory framework or legal requirement this policy addresses.
   *
   * <p>References specific regulations, standards, or compliance frameworks. Examples: "GDPR
   * (General Data Protection Regulation)", "SOX (Sarbanes-Oxley Act)", "HIPAA (Health Insurance
   * Portability and Accountability Act)", "PCI-DSS (Payment Card Industry Data Security Standard)",
   * "ISO 27001".
   */
  private String regulatoryRequirement;

  /**
   * Enforcement level indicating how strictly this policy must be followed.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>mandatory</b> - Must be followed; violations result in consequences
   *   <li><b>recommended</b> - Strongly encouraged but not required
   *   <li><b>optional</b> - Suggested best practice
   * </ul>
   *
   * <p>Also accepts 'enforcementMechanism' from source code extraction prompt.
   */
  @JsonAlias("enforcementMechanism")
  private String enforcementLevel;

  /**
   * Business rules that implement or enforce this policy.
   *
   * <p>Should reference rule names from chunk-level extraction for linking. Policies are high-level
   * directives; rules are the tactical implementation. Example: ["Password Complexity Rule",
   * "Password Expiration Rule"] implementing "Password Security Policy".
   */
  private List<String> implementedByRules;

  /**
   * Workflows or processes that implement or enforce this policy.
   *
   * <p>Should reference workflow names from document-level or chunk-level analysis for linking.
   * Example: ["User Registration Workflow", "Password Reset Workflow"] implementing "Password
   * Security Policy".
   */
  private List<String> implementedByWorkflows;

  /**
   * Business owner or steward responsible for defining and maintaining this policy.
   *
   * <p>The person, role, or department with authority over this policy. Example: "Chief Information
   * Security Officer", "Compliance Team", "Legal Department", "Data Protection Officer".
   */
  private String businessOwner;

  /**
   * Date when this policy becomes or became effective.
   *
   * <p>Format should match what's in the document (e.g., "2024-01-01", "January 1, 2024", "Q1
   * 2024"). Used for understanding policy timeline and version control.
   */
  private String effectiveDate;

  /**
   * AI extraction confidence score.
   *
   * <p>Range: 0.0 to 1.0, where:
   *
   * <ul>
   *   <li>0.9-1.0 = High confidence (clearly defined AND aligns with document context)
   *   <li>0.7-0.89 = Medium confidence (clearly mentioned but not formally defined)
   *   <li>0.5-0.69 = Low confidence (implied or partially described)
   * </ul>
   */
  private Double confidence;
}
