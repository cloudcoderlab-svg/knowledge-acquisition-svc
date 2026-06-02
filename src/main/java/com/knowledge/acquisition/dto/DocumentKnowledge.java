package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document-level knowledge extracted from enterprise documentation before chunking.
 *
 * <p>This DTO captures high-level architectural, domain, and business context from analyzing the
 * entire document. The extracted information serves two critical purposes:
 *
 * <ol>
 *   <li><b>Entity Linking</b> - Provides a consistent vocabulary (components, roles, capabilities,
 *       etc.) that is used during chunk-level extraction to ensure entities are named consistently
 *       across the knowledge graph.
 *   <li><b>Contextual Enrichment</b> - Supplies domain, architectural patterns, and technology
 *       context that helps interpret chunk-level content more accurately.
 * </ol>
 *
 * <p>The document-level analysis phase occurs before chunking and uses AI to understand the overall
 * purpose and structure of the document, avoiding the narrow context limitations of chunk-by-chunk
 * analysis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKnowledge {
  /** High-level architecture description (e.g., microservices, event-driven, layered, etc.). */
  private String overallArchitecture;

  /**
   * 1-2 paragraph summary of what this document describes from both technical and business
   * perspectives.
   */
  private String systemSummary;

  /**
   * Document type classification to understand its purpose and structure.
   *
   * <p>Possible values: technical_spec, business_requirements, user_guide, api_documentation,
   * architecture_doc, data_model, workflow_definition, config_file, mixed.
   */
  private String documentType;

  /** List of architectural patterns mentioned (e.g., CQRS, Event Sourcing, API Gateway, etc.). */
  private List<String> keyPatterns;

  /** List of technologies and frameworks mentioned (e.g., Spring Boot, React, PostgreSQL, etc.). */
  private List<String> technologies;

  /** Primary business domain (e.g., E-Commerce, Finance, Healthcare, HR, etc.). */
  private String domain;

  /**
   * Detailed description of the domain's purpose, scope, and key business functions.
   *
   * <p>Typically 2-3 sentences explaining what this domain does and why it matters to the business.
   */
  private String domainDescription;

  /** More specific subdomain if identifiable (e.g., Order Management, Payment Processing, etc.). */
  private String subdomain;

  /** List of major component/service names mentioned for entity linking in chunk extraction. */
  private List<String> identifiedComponents;

  /** List of key API endpoints or integration points for entity linking in chunk extraction. */
  private List<String> identifiedAPIs;

  /** List of major business workflows or processes for entity linking in chunk extraction. */
  private List<String> identifiedWorkflows;

  /**
   * List of business capabilities mentioned for entity linking in chunk extraction.
   *
   * <p>Examples: Customer Management, Order Processing, Payment Handling, etc.
   */
  private List<String> identifiedCapabilities;

  /**
   * List of business roles or actors for entity linking in chunk extraction.
   *
   * <p>Examples: Customer, Admin, Approver, Sales Rep, etc.
   */
  private List<String> identifiedRoles;

  /**
   * List of key business terms or domain vocabulary for entity linking in chunk extraction.
   *
   * <p>Examples: Order, Customer, SKU, Contract, Policy, etc.
   */
  private List<String> identifiedTerms;

  /**
   * List of policies or compliance requirements for entity linking in chunk extraction.
   *
   * <p>Examples: Data Retention Policy, GDPR Compliance, Approval Policy, etc.
   */
  private List<String> identifiedPolicies;

  /**
   * List of decision points for entity linking in chunk extraction.
   *
   * <p>Examples: Credit Approval Decision, Discount Eligibility Check, etc.
   */
  private List<String> identifiedDecisions;

  /** Additional metadata that doesn't fit into predefined fields. */
  private Map<String, Object> additionalMetadata;

  /**
   * Platform detected from the document content for routing to specialized extraction prompts.
   *
   * <p>Examples: "TIBCO_MDM", "PEGA_BPM", "GENERIC_XML", etc. This enables platform-specific
   * knowledge extraction optimizations.
   */
  private String detectedPlatform;
}
