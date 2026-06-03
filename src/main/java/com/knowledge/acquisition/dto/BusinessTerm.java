package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for business terms and domain vocabulary extracted from enterprise documentation.
 *
 * <p>Captures the ubiquitous language of the domain - the shared vocabulary used by both business
 * stakeholders and technical teams. These terms form the foundation for:
 *
 * <ul>
 *   <li>Domain-Driven Design (DDD) bounded contexts
 *   <li>Consistent naming across documentation and code
 *   <li>Business glossaries and data dictionaries
 *   <li>API and data model design
 *   <li>Communication between business and IT teams
 * </ul>
 *
 * <p>Business terms can represent domain entities (Order, Customer), process concepts (Fulfillment,
 * Onboarding), metrics (Customer Lifetime Value), roles (Account Manager), or any other
 * domain-specific vocabulary that has special meaning within the business context.
 *
 * <p>Maintaining a well-defined vocabulary prevents misunderstandings and ensures that the same
 * concept is referred to consistently across requirements, design, implementation, and testing.
 *
 * <p>Examples: Order, Customer, SKU, Contract, Policy, Premium Customer, Discount Tier, Account
 * Balance, Fulfillment Status, Customer Lifetime Value, Churn Rate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTerm {
  /**
   * Name of the term as it appears in the domain language.
   *
   * <p>Should match known terms from document-level analysis for entity linking. Use the most
   * common or official name. Examples: "Order", "Premium Customer", "SKU", "Fulfillment".
   */
  private String termName;

  /**
   * Definition of the term from a business perspective.
   *
   * <p>Explains what this term means in business language without technical jargon. Should be
   * understandable to business stakeholders. Example: "An Order is a customer's request to purchase
   * one or more products or services."
   */
  @JsonAlias("definition")
  private String businessDefinition;

  /**
   * Definition of the term from a technical implementation perspective.
   *
   * <p>Explains how this term is represented or implemented in the system. Example: "An Order is
   * represented as an aggregate root in the Order domain service, containing order items, customer
   * reference, and fulfillment status."
   */
  private String technicalDefinition;

  /**
   * Business context describing where and how this term is used in business logic.
   *
   * <p>Particularly useful from source code extraction to understand term usage patterns.
   */
  private String businessContext;

  /**
   * Synonyms or alternative names for this term (semicolon-separated).
   *
   * <p>Other terms that refer to the same concept, including legacy names, regional variations, or
   * system-specific aliases. Example: "Purchase; Sales Order; Customer Order" for "Order".
   */
  private String synonyms;

  /**
   * Category classification for this term.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li><b>domain</b> - Core domain entity or concept
   *   <li><b>process</b> - Business process or workflow term
   *   <li><b>metric</b> - Measurement or KPI term
   *   <li><b>role</b> - Role or actor designation
   *   <li><b>entity</b> - Data entity or model term
   *   <li><b>glossary</b> - General glossary term (source code extraction)
   *   <li><b>abbreviation</b> - Abbreviated term (source code extraction)
   *   <li><b>acronym</b> - Acronym (source code extraction)
   *   <li><b>domain_concept</b> - Domain-specific concept (source code extraction)
   * </ul>
   */
  @JsonAlias("termType")
  private String category;

  /**
   * Business owner or steward responsible for defining and maintaining this term.
   *
   * <p>The person, role, or department that has authority over this term's definition. Example:
   * "Chief Data Officer", "Product Management", "Finance Department".
   */
  private String businessOwner;

  /**
   * Workflows or processes where this term is used.
   *
   * <p>Should reference workflow names from document-level analysis for linking. Helps understand
   * term usage context.
   */
  private List<String> usedInWorkflows;

  /**
   * Business rules where this term is referenced or used.
   *
   * <p>List of rule names that use this term. Helps track term dependencies in business logic.
   */
  private List<String> usedInRules;

  /**
   * Data model entity or table that this term maps to.
   *
   * <p>The technical data structure that represents this business term. Example: "order_header
   * table", "OrderAggregate class", "orders collection".
   */
  private String mappedToDataModel;

  /**
   * AI extraction confidence score.
   *
   * <p>Range: 0.0 to 1.0, where:
   *
   * <ul>
   *   <li>0.9-1.0 = High confidence (clearly defined AND aligns with document context)
   *   <li>0.7-0.89 = Medium confidence (clearly mentioned but not formally defined)
   *   <li>0.5-0.69 = Low confidence (implied or inferred from usage)
   * </ul>
   */
  private Double confidence;
}
