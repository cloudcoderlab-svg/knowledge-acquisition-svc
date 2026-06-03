package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for miscellaneous knowledge notes extracted from enterprise documentation.
 *
 * <p>Captures important observations, insights, decisions, constraints, and contextual information
 * that don't fit into other structured categories (like BusinessRule, BusinessFlow, etc.). This
 * serves as a catch-all for valuable knowledge that would otherwise be lost during extraction.
 *
 * <p>Knowledge notes are particularly valuable for:
 *
 * <ul>
 *   <li><b>Architecture Decisions</b> - Why certain patterns or technologies were chosen
 *   <li><b>Design Rationale</b> - Trade-offs and reasoning behind design choices
 *   <li><b>Constraints</b> - Technical, business, or regulatory limitations
 *   <li><b>Assumptions</b> - Presumptions made during design or planning
 *   <li><b>Risks</b> - Potential issues, vulnerabilities, or concerns
 *   <li><b>Recommendations</b> - Suggested improvements or best practices
 * </ul>
 *
 * <p>These notes often represent critical contextual information that helps developers and
 * architects understand the "why" behind the system. They are especially useful during:
 *
 * <ul>
 *   <li>System modernization and migration planning
 *   <li>Technical debt assessment
 *   <li>Architecture review and documentation
 *   <li>Onboarding new team members
 *   <li>Impact analysis for proposed changes
 * </ul>
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Architecture: "Microservices pattern chosen over monolith to enable independent team
 *       scaling"
 *   <li>Design Decision: "PostgreSQL selected over MongoDB due to strong ACID guarantees required
 *       for financial transactions"
 *   <li>Constraint: "System must support Internet Explorer 11 until Q2 2024 due to enterprise
 *       customer requirements"
 *   <li>Assumption: "Assuming average order size of 5 items for capacity planning"
 *   <li>Risk: "Single database instance represents potential single point of failure"
 *   <li>Recommendation: "Consider implementing caching layer to reduce database load during peak
 *       hours"
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeNote {
  /**
   * Category classification for this knowledge note.
   *
   * <p>Possible values (enhanced chunk and platform-specific prompts):
   *
   * <ul>
   *   <li><b>architecture</b> - High-level system architecture or structural decision
   *   <li><b>design_decision</b> or <b>design</b> - Specific design choice or pattern selection
   *       with rationale
   *   <li><b>constraint</b> - Technical, business, or regulatory limitation or requirement
   *   <li><b>assumption</b> - Presumption or condition assumed to be true during planning/design
   *   <li><b>risk</b> - Potential issue, vulnerability, technical debt, or concern
   *   <li><b>recommendation</b> - Suggested improvement, best practice, or future enhancement
   *   <li><b>migration</b> - Migration-specific notes (platform-specific prompts)
   * </ul>
   *
   * <p>Additional values from source code extraction prompt:
   *
   * <ul>
   *   <li><b>architecture_decision</b> - Architecture decision from code
   *   <li><b>technical_debt</b> - Technical debt or code smell
   *   <li><b>migration_concern</b> - Migration or modernization concern
   *   <li><b>business_constraint</b> - Business constraint from code
   *   <li><b>todo</b> - TODO comment or incomplete code
   *   <li><b>code_smell</b> - Code quality issue
   * </ul>
   */
  @JsonAlias("category")
  private String noteType;

  /**
   * Brief topic or title for this knowledge note.
   *
   * <p>Used by source code extraction prompt to provide a concise summary of what the note is
   * about. Examples: "Database Connection Pooling", "Exception Handling Pattern", "Deprecated API
   * Usage".
   */
  private String topic;

  /**
   * The actual note content.
   *
   * <p>Should be clear, concise, and self-contained. For design decisions, include the reasoning
   * (why this choice was made). For risks, describe the potential impact. For recommendations,
   * explain the expected benefit.
   *
   * <p>Good notes are actionable and provide enough context to be understood without needing to
   * reference the source document.
   */
  @JsonAlias("noteText")
  private String content;

  /**
   * Name of the related entity, component, workflow, or capability this note applies to.
   *
   * <p>Used by platform-specific extraction prompts (TIBCO MDM, Pega BPM, Camunda BPMN) to link
   * notes to specific entities or components they relate to. Examples: "OrderManagementWorkflow",
   * "CustomerDataModel", "PaymentService".
   *
   * <p>This field helps contextualize notes and enables filtering notes by related entity.
   */
  private String relatedEntity;

  /**
   * AI extraction confidence score.
   *
   * <p>Range: 0.0 to 1.0, where:
   *
   * <ul>
   *   <li>0.9-1.0 = High confidence (explicitly stated in document with clear context)
   *   <li>0.7-0.89 = Medium confidence (clearly mentioned but context may be incomplete)
   *   <li>0.5-0.69 = Low confidence (implied or inferred from surrounding text)
   * </ul>
   */
  private Double confidence;
}
