package com.knowledge.acquisition.controller;

import com.knowledge.acquisition.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for data quality verification and knowledge entity sampling.
 *
 * <p>This controller provides comprehensive endpoints for verifying the quality and completeness of
 * extracted knowledge entities after pipeline processing. It enables quality assurance workflows,
 * data validation, and spot-checking of extraction results.
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li>Entity count aggregation across all knowledge types
 *   <li>Sample data retrieval for manual verification
 *   <li>Quality metrics calculation for processed projects
 *   <li>Support for live dashboard integration
 * </ul>
 *
 * <p><b>Supported Entity Types:</b>
 *
 * <ul>
 *   <li>Domains - Business domains and capabilities
 *   <li>Workflows - Business processes and flows
 *   <li>APIs - REST/SOAP endpoints and interfaces
 *   <li>Business Rules - Validation rules and rulebases
 *   <li>Data Models - Entity definitions and schemas
 *   <li>Components - System components and modules
 *   <li>Integrations - External system integrations
 *   <li>Relationships - Cross-document entity relationships
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>
 * GET /api/v1/quality/projects/{projectId}
 * GET /api/v1/quality/projects/{projectId}/domains?limit=10
 * GET /api/v1/quality/projects/{projectId}/workflows?limit=5
 * </pre>
 *
 * @author Knowledge Acquisition Service
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/quality")
@RequiredArgsConstructor
public class DataQualityController {

  /** Repository for accessing domain entities. */
  private final DomainRepository domainRepository;

  /** Repository for accessing workflow entities. */
  private final WorkflowRepository workflowRepository;

  /** Repository for accessing API entities. */
  private final ApiRepository apiRepository;

  /** Repository for accessing business rule entities. */
  private final BusinessRuleRepository businessRuleRepository;

  /** Repository for accessing data model entities. */
  private final DataModelRepository dataModelRepository;

  /** Repository for accessing component entities. */
  private final ComponentRepository componentRepository;

  /** Repository for accessing module entities. */
  private final ModuleRepository moduleRepository;

  /** Repository for accessing integration entities. */
  private final IntegrationRepository integrationRepository;

  /** Repository for accessing source document entities. */
  private final SourceDocumentRepository sourceDocumentRepository;

  /** Repository for accessing knowledge chunk entities. */
  private final KnowledgeChunkRepository knowledgeChunkRepository;

  /** Repository for accessing relationship entities. */
  private final RelationshipRepository relationshipRepository;

  /**
   * Gets comprehensive data quality metrics for a project.
   *
   * <p>This endpoint aggregates entity counts across all knowledge types to provide a holistic view
   * of extraction quality and completeness. The total knowledge entities count excludes
   * intermediate artifacts (source documents and knowledge chunks) to focus on final extracted
   * knowledge.
   *
   * <p><b>Calculation Logic:</b>
   *
   * <ul>
   *   <li>Query each repository for entity count by project ID
   *   <li>Aggregate all entity types into a comprehensive map
   *   <li>Calculate total knowledge entities (excluding source documents and chunks)
   * </ul>
   *
   * <p><b>Example Response:</b>
   *
   * <pre>
   * {
   *   "projectId": "70af773c-2174-4d22-a269-bed316084a34",
   *   "entityCounts": {
   *     "domains": 11,
   *     "workflows": 70,
   *     "businessRules": 397,
   *     "dataModels": 48,
   *     "components": 382,
   *     "apis": 0,
   *     "modules": 0,
   *     "integrations": 0,
   *     "sourceDocuments": 26,
   *     "knowledgeChunks": 921,
   *     "relationships": 61
   *   },
   *   "totalKnowledgeEntities": 969
   * }
   * </pre>
   *
   * @param projectId the UUID of the project to analyze
   * @return DataQualityReport containing entity counts and total knowledge entities
   */
  @GetMapping("/projects/{projectId}")
  public DataQualityReport getDataQuality(@PathVariable UUID projectId) {
    // Initialize entity counts map
    Map<String, Integer> entityCounts = new HashMap<>();

    // Query each repository for count by project ID
    entityCounts.put("domains", domainRepository.findByProjectId(projectId).size());
    entityCounts.put("workflows", workflowRepository.findByProjectId(projectId).size());
    entityCounts.put("apis", apiRepository.findByProjectId(projectId).size());
    entityCounts.put("businessRules", businessRuleRepository.findByProjectId(projectId).size());
    entityCounts.put("dataModels", dataModelRepository.findByProjectId(projectId).size());
    entityCounts.put("components", componentRepository.findByProjectId(projectId).size());
    entityCounts.put("modules", moduleRepository.findByProjectId(projectId).size());
    entityCounts.put("integrations", integrationRepository.findByProjectId(projectId).size());
    entityCounts.put("sourceDocuments", sourceDocumentRepository.findByProjectId(projectId).size());
    entityCounts.put("knowledgeChunks", knowledgeChunkRepository.findByProjectId(projectId).size());
    entityCounts.put("relationships", relationshipRepository.findByProjectId(projectId).size());

    // Calculate total knowledge entities (excluding intermediate artifacts)
    int totalEntities =
        entityCounts.values().stream().mapToInt(Integer::intValue).sum()
            - entityCounts.get("sourceDocuments") // Exclude source documents
            - entityCounts.get("knowledgeChunks"); // Exclude intermediate chunks

    return new DataQualityReport(projectId, entityCounts, totalEntities);
  }

  /**
   * Gets sample domain entities for manual quality verification.
   *
   * <p>Retrieves a limited number of domain entities to enable spot-checking of extraction quality.
   * Domains represent business domains and capabilities identified during knowledge extraction.
   *
   * <p><b>Use Cases:</b>
   *
   * <ul>
   *   <li>Manual verification of domain identification accuracy
   *   <li>Quality assurance spot-checks
   *   <li>Dashboard preview widgets
   *   <li>Extraction result validation
   * </ul>
   *
   * <p><b>Response Fields:</b>
   *
   * <ul>
   *   <li><b>domainId</b> - Unique identifier for the domain
   *   <li><b>domainName</b> - Name of the business domain
   *   <li><b>description</b> - Detailed description of the domain
   *   <li><b>confidence</b> - Extraction confidence score (0.0 to 1.0)
   * </ul>
   *
   * @param projectId the UUID of the project
   * @param limit maximum number of samples to return (default: 10)
   * @return list of domain sample data as key-value maps
   */
  @GetMapping("/projects/{projectId}/domains")
  public List<Map<String, Object>> getDomainSamples(
      @PathVariable UUID projectId, @RequestParam(defaultValue = "10") int limit) {
    return domainRepository.findByProjectId(projectId).stream()
        .limit(limit)
        .map(
            d -> {
              // Build sample map with essential domain fields
              Map<String, Object> map = new HashMap<>();
              map.put("domainId", d.getDomainId());
              map.put("domainName", d.getDomainName());
              map.put("description", d.getDescription() != null ? d.getDescription() : "");
              map.put("confidence", d.getConfidence() != null ? d.getConfidence() : 0.0);
              return map;
            })
        .toList();
  }

  /**
   * Gets sample workflow entities for manual quality verification.
   *
   * <p>Retrieves a limited number of workflow entities to validate extraction of business processes
   * and flows. Workflows represent sequential business operations identified in source documents.
   *
   * <p><b>Use Cases:</b>
   *
   * <ul>
   *   <li>Verify workflow extraction completeness
   *   <li>Validate trigger and outcome identification
   *   <li>Check actor/role assignment accuracy
   *   <li>Quality assurance for process mining results
   * </ul>
   *
   * <p><b>Response Fields:</b>
   *
   * <ul>
   *   <li><b>workflowId</b> - Unique identifier for the workflow
   *   <li><b>workflowName</b> - Name of the workflow or process
   *   <li><b>triggerText</b> - Conditions that trigger the workflow
   *   <li><b>outcomeText</b> - Expected outcomes or results
   *   <li><b>actor</b> - Person or system that executes the workflow
   * </ul>
   *
   * @param projectId the UUID of the project
   * @param limit maximum number of samples to return (default: 10)
   * @return list of workflow sample data as key-value maps
   */
  @GetMapping("/projects/{projectId}/workflows")
  public List<Map<String, Object>> getWorkflowSamples(
      @PathVariable UUID projectId, @RequestParam(defaultValue = "10") int limit) {
    return workflowRepository.findByProjectId(projectId).stream()
        .limit(limit)
        .map(
            w -> {
              // Build sample map with essential workflow fields
              Map<String, Object> map = new HashMap<>();
              map.put("workflowId", w.getWorkflowId());
              map.put("workflowName", w.getWorkflowName() != null ? w.getWorkflowName() : "");
              map.put("triggerText", w.getTriggerText() != null ? w.getTriggerText() : "");
              map.put("outcomeText", w.getOutcomeText() != null ? w.getOutcomeText() : "");
              map.put("actor", w.getActor() != null ? w.getActor() : "");
              return map;
            })
        .toList();
  }

  /**
   * Gets sample API entities for manual quality verification.
   *
   * <p>Retrieves a limited number of API entities to validate extraction of REST/SOAP endpoints and
   * service interfaces. APIs represent external integration points identified in source documents.
   *
   * <p><b>Use Cases:</b>
   *
   * <ul>
   *   <li>Verify API endpoint extraction accuracy
   *   <li>Validate HTTP method identification
   *   <li>Check API type classification (REST, SOAP, GraphQL)
   *   <li>Integration architecture validation
   * </ul>
   *
   * <p><b>Response Fields:</b>
   *
   * <ul>
   *   <li><b>apiId</b> - Unique identifier for the API
   *   <li><b>apiName</b> - Name of the API or service
   *   <li><b>apiType</b> - API type (REST, SOAP, GraphQL, etc.)
   *   <li><b>endpointPath</b> - URL path or endpoint location
   *   <li><b>httpMethod</b> - HTTP method (GET, POST, PUT, DELETE, etc.)
   * </ul>
   *
   * @param projectId the UUID of the project
   * @param limit maximum number of samples to return (default: 10)
   * @return list of API sample data as key-value maps
   */
  @GetMapping("/projects/{projectId}/apis")
  public List<Map<String, Object>> getApiSamples(
      @PathVariable UUID projectId, @RequestParam(defaultValue = "10") int limit) {
    return apiRepository.findByProjectId(projectId).stream()
        .limit(limit)
        .map(
            a -> {
              // Build sample map with essential API fields
              Map<String, Object> map = new HashMap<>();
              map.put("apiId", a.getApiId());
              map.put("apiName", a.getApiName() != null ? a.getApiName() : "");
              map.put("apiType", a.getApiType() != null ? a.getApiType() : "");
              map.put("endpointPath", a.getEndpointPath() != null ? a.getEndpointPath() : "");
              map.put("httpMethod", a.getHttpMethod() != null ? a.getHttpMethod() : "");
              return map;
            })
        .toList();
  }

  /**
   * Gets sample business rule entities for manual quality verification.
   *
   * <p>Retrieves a limited number of business rule entities to validate extraction of validation
   * rules, rulebases, and business logic. Business rules represent conditional logic and
   * constraints identified in source documents.
   *
   * <p><b>Use Cases:</b>
   *
   * <ul>
   *   <li>Verify business rule extraction completeness
   *   <li>Validate condition and criteria identification
   *   <li>Check rule type classification accuracy
   *   <li>Compliance and governance validation
   * </ul>
   *
   * <p><b>Response Fields:</b>
   *
   * <ul>
   *   <li><b>ruleId</b> - Unique identifier for the business rule
   *   <li><b>ruleName</b> - Name of the rule or rulebase
   *   <li><b>ruleType</b> - Type of rule (validation, assignment, workflow, etc.)
   *   <li><b>conditionText</b> - Conditions under which the rule applies
   *   <li><b>validationCriteria</b> - Validation criteria or constraints
   * </ul>
   *
   * @param projectId the UUID of the project
   * @param limit maximum number of samples to return (default: 10)
   * @return list of business rule sample data as key-value maps
   */
  @GetMapping("/projects/{projectId}/business-rules")
  public List<Map<String, Object>> getBusinessRuleSamples(
      @PathVariable UUID projectId, @RequestParam(defaultValue = "10") int limit) {
    return businessRuleRepository.findByProjectId(projectId).stream()
        .limit(limit)
        .map(
            br -> {
              // Build sample map with essential business rule fields
              Map<String, Object> map = new HashMap<>();
              map.put("ruleId", br.getRuleId());
              map.put("ruleName", br.getRuleName() != null ? br.getRuleName() : "");
              map.put("ruleType", br.getRuleType() != null ? br.getRuleType() : "");
              map.put("conditionText", br.getConditionText() != null ? br.getConditionText() : "");
              map.put(
                  "validationCriteria",
                  br.getValidationCriteria() != null ? br.getValidationCriteria() : "");
              return map;
            })
        .toList();
  }

  /**
   * Gets sample data model entities for manual quality verification.
   *
   * <p>Retrieves a limited number of data model entities to validate extraction of entity
   * definitions, schemas, and data structures. Data models represent business entities and their
   * attributes identified in source documents.
   *
   * <p><b>Use Cases:</b>
   *
   * <ul>
   *   <li>Verify data model extraction completeness
   *   <li>Validate entity definition accuracy
   *   <li>Check schema extraction quality
   *   <li>Data architecture validation
   * </ul>
   *
   * <p><b>Response Fields:</b>
   *
   * <ul>
   *   <li><b>dataModelId</b> - Unique identifier for the data model
   *   <li><b>modelName</b> - Name of the entity or data model
   *   <li><b>modelType</b> - Type of model (entity, table, document, etc.)
   *   <li><b>businessDefinition</b> - Business description of the model
   * </ul>
   *
   * @param projectId the UUID of the project
   * @param limit maximum number of samples to return (default: 10)
   * @return list of data model sample data as key-value maps
   */
  @GetMapping("/projects/{projectId}/data-models")
  public List<Map<String, Object>> getDataModelSamples(
      @PathVariable UUID projectId, @RequestParam(defaultValue = "10") int limit) {
    return dataModelRepository.findByProjectId(projectId).stream()
        .limit(limit)
        .map(
            dm -> {
              // Build sample map with essential data model fields
              Map<String, Object> map = new HashMap<>();
              map.put("dataModelId", dm.getDataModelId());
              map.put("modelName", dm.getModelName() != null ? dm.getModelName() : "");
              map.put("modelType", dm.getModelType() != null ? dm.getModelType() : "");
              map.put(
                  "businessDefinition",
                  dm.getBusinessDefinition() != null ? dm.getBusinessDefinition() : "");
              return map;
            })
        .toList();
  }

  /**
   * Data quality report containing entity counts and metrics.
   *
   * <p>This record encapsulates the comprehensive quality metrics for a project, including counts
   * of all extracted entity types and a total knowledge entities count.
   *
   * <p><b>Field Descriptions:</b>
   *
   * <ul>
   *   <li><b>projectId</b> - UUID of the analyzed project
   *   <li><b>entityCounts</b> - Map of entity type names to their counts
   *   <li><b>totalKnowledgeEntities</b> - Total count excluding intermediate artifacts (source
   *       documents and knowledge chunks)
   * </ul>
   *
   * @param projectId the project UUID
   * @param entityCounts map of entity types to their counts
   * @param totalKnowledgeEntities total final knowledge entities extracted
   */
  public record DataQualityReport(
      UUID projectId, Map<String, Integer> entityCounts, int totalKnowledgeEntities) {}
}
