package com.knowledge.acquisition.dto;

import lombok.*;

/**
 * Represents a relationship discovered between entities across different documents.
 *
 * <p>Cross-document relationships are identified during the consolidation phase by analyzing
 * multiple document summaries and their metadata to find meaningful connections between entities
 * (components, workflows, business rules, etc.) that span across file boundaries.
 *
 * <p><strong>Example Relationships:</strong>
 *
 * <ul>
 *   <li>Workflow A in document1.xml triggers Business Rule B in document2.xml
 *   <li>Component X in file1.xml depends on Component Y in file2.xml
 *   <li>Domain A in config1.xml shares data model with Domain B in config2.xml
 * </ul>
 *
 * @author Knowledge Engine Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossDocumentRelationship {

  /** Name of the source document where the source entity is defined. */
  private String sourceDocument;

  /** Name of the target document where the target entity is defined. */
  private String targetDocument;

  /** Name of the source entity in the relationship (e.g., "CreateOrderWorkflow"). */
  private String sourceName;

  /** Type of the source entity (e.g., "workflow", "component", "business_rule"). */
  private String sourceType;

  /** Name of the target entity in the relationship (e.g., "ValidateOrderRule"). */
  private String targetName;

  /** Type of the target entity (e.g., "workflow", "component", "business_rule"). */
  private String targetType;

  /**
   * Type of relationship between source and target.
   *
   * <p>Common relationship types:
   *
   * <ul>
   *   <li>"triggers" - source initiates target
   *   <li>"depends_on" - source requires target
   *   <li>"uses" - source utilizes target
   *   <li>"validates" - source validates target
   *   <li>"transforms" - source transforms data for target
   * </ul>
   */
  private String relationshipType;

  /**
   * Contextual description explaining the nature and purpose of the relationship.
   *
   * <p>Example: "The CreateOrder workflow triggers the ValidateOrder rule to ensure all mandatory
   * fields are present before order creation."
   */
  private String context;

  /**
   * Supporting evidence from the documents that justifies this relationship.
   *
   * <p>This field contains excerpts, metadata, or references from the source documents that were
   * used to infer the relationship. Added to support AI-generated analysis transparency.
   *
   * <p><strong>Note:</strong> This field was added to support Vertex AI responses that include
   * evidence. The application configuration is set to ignore unknown JSON properties during
   * deserialization.
   *
   * @since 1.0.1
   */
  private String evidence;

  /**
   * Confidence score indicating the certainty of this relationship (0.0 to 1.0).
   *
   * <p>Relationships with confidence below 0.6 are typically filtered out during consolidation.
   *
   * <ul>
   *   <li>0.9-1.0: Very high confidence (explicit references)
   *   <li>0.7-0.9: High confidence (strong semantic similarity)
   *   <li>0.6-0.7: Medium confidence (inferred connections)
   *   <li>&lt;0.6: Low confidence (excluded by default)
   * </ul>
   */
  private Double confidence;
}
