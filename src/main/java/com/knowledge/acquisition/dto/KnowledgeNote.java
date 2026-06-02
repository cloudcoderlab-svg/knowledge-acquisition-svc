package com.knowledge.acquisition.dto;

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
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>architecture</b> - High-level system architecture or structural decision
   *   <li><b>design_decision</b> - Specific design choice or pattern selection with rationale
   *   <li><b>constraint</b> - Technical, business, or regulatory limitation or requirement
   *   <li><b>assumption</b> - Presumption or condition assumed to be true during planning/design
   *   <li><b>risk</b> - Potential issue, vulnerability, technical debt, or concern
   *   <li><b>recommendation</b> - Suggested improvement, best practice, or future enhancement
   * </ul>
   */
  private String noteType;

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
  private String noteText;

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
