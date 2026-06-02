package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for business capabilities extracted from enterprise documentation.
 *
 * <p>A business capability represents what the organization does or can do to achieve its goals,
 * independent of how it's implemented. Capabilities are fundamental building blocks of business
 * architecture and provide a stable, technology-agnostic view of the business.
 *
 * <p>Business capabilities answer the question "What does the business do?" rather than "How does
 * the business do it?" They remain relatively stable over time even as technology, processes, and
 * organizational structures change.
 *
 * <p><strong>Capability vs Component:</strong>
 *
 * <ul>
 *   <li><b>Business Capability:</b> WHAT the business does (e.g., "Customer Management", "Order
 *       Processing") - business-focused, technology-independent
 *   <li><b>Solution Component:</b> HOW it's implemented (e.g., "OrderService", "CustomerAPI") -
 *       technical implementation, technology-specific
 * </ul>
 *
 * <p><strong>Capability Types:</strong>
 *
 * <ul>
 *   <li><b>Core:</b> Capabilities that differentiate the business and directly deliver customer
 *       value (e.g., "Product Recommendation", "Fraud Detection")
 *   <li><b>Supporting:</b> Capabilities that enable core capabilities but don't directly
 *       differentiate (e.g., "Customer Support", "Billing")
 *   <li><b>Enabling:</b> Foundational capabilities that support the entire organization (e.g.,
 *       "Identity Management", "Data Analytics")
 * </ul>
 *
 * <p><strong>Examples:</strong>
 *
 * <ul>
 *   <li>Order Management
 *   <li>Customer Onboarding
 *   <li>Inventory Management
 *   <li>Payment Processing
 *   <li>Risk Assessment
 *   <li>Reporting and Analytics
 * </ul>
 *
 * @see TechnicalComponent for implementation-level solution components
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessCapability {
  /**
   * Name of the business capability.
   *
   * <p>Should be expressed as a business function or activity using business terminology, not
   * technical jargon. Examples: "Order Management", "Customer Onboarding", "Risk Assessment".
   *
   * <p>Should match known capabilities from document-level analysis for entity linking consistency.
   */
  private String capabilityName;

  /**
   * Classification of the capability's strategic importance.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>core</b> - Differentiating capabilities that directly deliver customer value
   *   <li><b>supporting</b> - Capabilities that enable core capabilities but don't differentiate
   *   <li><b>enabling</b> - Foundational capabilities supporting the entire organization
   * </ul>
   */
  private String capabilityType;

  /**
   * Description of what this capability enables the business to do.
   *
   * <p>Should explain the business purpose and scope of this capability in business terms, not
   * technical implementation details.
   */
  private String description;

  /**
   * Business value this capability delivers to the organization.
   *
   * <p>Describes why this capability matters, what business outcomes it enables, or what customer
   * needs it satisfies. Examples: "Enables faster customer onboarding", "Reduces fraud risk",
   * "Improves order fulfillment accuracy".
   */
  private String businessValue;

  /**
   * AI extraction confidence score.
   *
   * <p>Range: 0.0 to 1.0, where:
   *
   * <ul>
   *   <li>0.9-1.0 = Very high confidence (explicitly mentioned AND described in detail)
   *   <li>0.7-0.89 = High confidence (explicitly mentioned with some details)
   *   <li>0.5-0.69 = Medium confidence (implied or partially described)
   *   <li>&lt;0.5 = Do not extract (insufficient evidence)
   * </ul>
   */
  private Double confidence;
}
