package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import lombok.Data;

/**
 * Result of chunk-level knowledge extraction from enterprise documentation.
 *
 * <p>This DTO captures structured knowledge extracted from individual document chunks, including
 * both technical and business entities. Entity linking is performed using document-level context to
 * ensure consistency across the knowledge graph.
 *
 * <p><strong>Schema Alignment:</strong> This DTO is aligned with the optimized knowledge extraction
 * prompt (knowledge-extraction-prompt.txt) which follows anti-hallucination best practices,
 * confidence scoring rubrics, and output quality rules.
 *
 * <p><strong>Removed Fields (as of schema v2):</strong>
 *
 * <ul>
 *   <li><b>businessComponents:</b> Replaced by separate businessCapabilities and solutionComponents
 *       (technicalComponents) to clearly distinguish business-level capabilities from technical
 *       implementation components
 *   <li><b>costEstimates:</b> Removed - rarely present in documentation and prone to hallucination
 *   <li><b>usageProfiles:</b> Removed - rarely present in documentation and prone to hallucination
 *   <li><b>migrationNotes:</b> Removed - consolidated into knowledgeNotes with noteType filter
 * </ul>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeExtractionResult {
  /** Brief summary of what this chunk describes in architectural context. */
  private String architecturalSummary;

  /** Business roles and actors mentioned in this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessRole> businessRoles;

  /** Business rules extracted from this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessRule> businessRules;

  /** Business workflows and processes extracted from this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessFlow> businessFlows;

  /** Business decision points extracted from this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessDecision> businessDecisions;

  /** Business terms and domain vocabulary extracted from this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessTerm> businessTerms;

  /** Business policies and compliance requirements extracted from this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessPolicy> businessPolicies;

  /** Business metrics and KPIs extracted from this chunk. */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessMetric> businessMetrics;

  /**
   * Business capabilities extracted from this chunk.
   *
   * <p>Capabilities represent WHAT the business does (business functions), independent of HOW
   * they're implemented. Examples: "Order Management", "Customer Onboarding", "Risk Assessment".
   */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessCapability> businessCapabilities;

  /**
   * Solution components (technical) extracted from this chunk.
   *
   * <p>Components represent HOW capabilities are implemented (technical solutions). Aliased from
   * "solutionComponents" in the extraction prompt. Examples: "OrderService", "CustomerAPI".
   */
  @JsonAlias({"solutionComponents"})
  @JsonSetter(nulls = Nulls.SKIP)
  private List<TechnicalComponent> technicalComponents;

  @JsonAlias({"apis", "knowledgeApis", "knowledgeAPIs"})
  @JsonSetter(nulls = Nulls.SKIP)
  private List<KnowledgeAPI> apis;

  @JsonAlias({"dataModels"})
  @JsonSetter(nulls = Nulls.SKIP)
  private List<KnowledgeDataModel> dataModels;

  @JsonAlias({"integrations", "knowledgeIntegrations"})
  @JsonSetter(nulls = Nulls.SKIP)
  private List<KnowledgeIntegration> integrations;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<DeploymentResource> deploymentResources;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<KnowledgeRelationship> relationships;

  /**
   * Miscellaneous knowledge notes extracted from this chunk.
   *
   * <p>Includes architecture decisions, design decisions, constraints, assumptions, risks, and
   * recommendations. Use noteType field to filter by category. Migration-specific notes should use
   * noteType="recommendation" or "constraint".
   */
  @JsonSetter(nulls = Nulls.SKIP)
  private List<KnowledgeNote> knowledgeNotes;
}
