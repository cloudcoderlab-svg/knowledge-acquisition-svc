package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/**
 * Data transfer object representing the classification result of a document.
 *
 * <p>This DTO captures the AI-generated classification of a document including its domain,
 * capabilities, entities, and integration patterns. It is the primary output of document-level
 * analysis.
 *
 * <h3>Classification Hierarchy</h3>
 *
 * <pre>
 * Domain (e.g., "Customer Management")
 *   └─ Subdomain (e.g., "Customer Onboarding")
 *       ├─ Business Capability (e.g., "Customer Registration")
 *       └─ Technical Capability (e.g., "REST API Integration")
 * </pre>
 *
 * <h3>Classification Process</h3>
 *
 * <ol>
 *   <li>Document is parsed and text extracted
 *   <li>AI model analyzes content with project context
 *   <li>Classification result generated with domain hierarchy and entities
 *   <li>Confidence score calculated (0.0 to 1.0)
 *   <li>Summary generated for quick understanding
 * </ol>
 *
 * <h3>Usage</h3>
 *
 * <p>Classification results are used for:
 *
 * <ul>
 *   <li>Organizing documents into domain hierarchy
 *   <li>Generating planning artifacts (EPICs, FEATUREs)
 *   <li>Identifying cross-document relationships
 *   <li>Data quality assessment
 * </ul>
 *
 * @see com.knowledge.acquisition.ingestion.service.DocumentLevelAnalysisService
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificationResult {

  /**
   * Primary domain of the document (e.g., "Customer Management", "Order Processing").
   *
   * <p>Also accepts "primaryDomain" from JSON for backward compatibility.
   */
  @JsonAlias("primaryDomain")
  private String domain;

  /**
   * Subdomain within the primary domain (e.g., "Customer Onboarding", "Order Fulfillment").
   *
   * <p>Provides finer-grained categorization under the domain.
   */
  private String subdomain;

  /**
   * Business capability described in the document (e.g., "Customer Registration", "Payment
   * Processing").
   *
   * <p>Represents what business function is being addressed.
   */
  private String businessCapability;

  /**
   * Technical capability or implementation pattern (e.g., "REST API Integration", "Batch
   * Processing").
   *
   * <p>Describes how the capability is technically implemented.
   */
  private String technicalCapability;

  /**
   * List of key entities mentioned in the document (e.g., ["Customer", "Order", "Payment"]).
   *
   * <p>Used for relationship detection and data model generation.
   */
  private List<String> entities;

  /**
   * Integration types identified (e.g., ["REST API", "Message Queue", "Database"]).
   *
   * <p>Helps identify integration patterns and dependencies.
   */
  private List<String> integrationTypes;

  /**
   * AI model confidence score for the classification (0.0 to 1.0).
   *
   * <p>Higher values indicate more confident classification. Typically:
   *
   * <ul>
   *   <li>0.8-1.0: High confidence
   *   <li>0.6-0.8: Medium confidence
   *   <li>0.0-0.6: Low confidence (may need manual review)
   * </ul>
   */
  private Double confidence;

  /** Brief summary of the document content and classification. */
  private String summary;

  /**
   * UUID of the source document that was classified.
   *
   * <p>Links the classification result back to the ingested document for traceability.
   */
  private UUID sourceDocumentId;

  /**
   * Total AI model tokens consumed during classification.
   *
   * <p>Used for cost tracking and optimization.
   */
  private Long tokensConsumed;
}
