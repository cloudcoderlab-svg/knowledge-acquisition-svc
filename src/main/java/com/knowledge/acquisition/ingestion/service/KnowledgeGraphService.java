package com.knowledge.acquisition.ingestion.service;

import com.knowledge.acquisition.dto.*;
import com.knowledge.acquisition.ingestion.service.ai.KnowledgeEntityEmbeddingService;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Converts extracted document knowledge into the normalized knowledge graph tables.
 *
 * <p>This service processes extraction results from multiple sources:
 *
 * <ul>
 *   <li><b>Document-level analysis:</b> High-level business concepts (capabilities, roles, terms,
 *       policies, decisions) extracted from entire documents for entity linking
 *   <li><b>Chunk-level extraction:</b> Detailed technical and business entities extracted from
 *       document chunks (including text, PDF, markdown)
 *   <li><b>CSV extraction:</b> Structured mappings, rules, workflows, and data models extracted
 *       from tabular files
 *   <li><b>XML extraction:</b> Platform-specific knowledge from configuration files (TIBCO, Appian,
 *       SAP, etc.)
 * </ul>
 *
 * <p>This service is intentionally defensive about AI output quality. Names that participate in
 * database constraints are filled from nearby descriptive fields when the model omits them, and
 * embeddings are attached immediately before persistence so quota-aware embedding failures can be
 * handled by the embedding layer without dropping the entity.
 *
 * <p>CSV extraction results are processed through the same chunk-level pipeline, allowing
 * structured data from mapping files to be integrated seamlessly into the knowledge graph alongside
 * extracted documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphService {
  private final KnowledgeEntityEmbeddingService embeddingService;
  private final PostgresStorageService storageService;

  @Value("${knowledge-engine.knowledge-graph.enable-persistence:true}")
  private boolean enablePersistence;

  /**
   * Persists document-level and chunk-level extraction results into graph entities.
   *
   * <p>This is the main entry point for knowledge graph construction. It processes:
   *
   * <ul>
   *   <li><b>Document-level business concepts</b> (capabilities, roles, terms, policies, decisions)
   *       for entity linking
   *   <li><b>Chunk-level technical entities</b> (components, APIs, data models, integrations,
   *       resources)
   *   <li><b>Chunk-level business entities</b> (rules, workflows, roles, decisions, terms,
   *       policies, metrics, notes)
   *   <li><b>Relationships</b> between all entity types
   * </ul>
   *
   * <p>CSV extraction results flow through {@code chunkKnowledge} parameter, making this method
   * handle both traditional document chunks and structured CSV data extraction results.
   *
   * @param sourceDocumentId stored source document identifier
   * @param source metadata for the source object and project
   * @param docKnowledge document-level classification and summary
   * @param chunkKnowledge chunk-level extraction results to normalize and persist (includes CSV
   *     extraction results)
   */
  public void buildKnowledgeGraph(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      DocumentKnowledge docKnowledge,
      List<KnowledgeExtractionResult> chunkKnowledge) {
    if (!enablePersistence) {
      return;
    }
    UUID domainId = null;
    UUID subdomainId = null;
    if (docKnowledge != null && docKnowledge.getDomain() != null) {
      domainId =
          storageService.ensureDomain(
              source.projectId(),
              docKnowledge.getDomain(),
              docKnowledge.getDomainDescription(),
              0.9); // High confidence for document-level domain analysis
      if (docKnowledge.getSubdomain() != null) {
        subdomainId =
            storageService.ensureSubdomain(
                source.projectId(),
                docKnowledge.getDomain(),
                domainId,
                docKnowledge.getSubdomain());
      }
      storageService.saveDocumentKnowledge(sourceDocumentId, source.projectId(), docKnowledge);

      // Save document-level business concepts for entity linking in chunk extraction
      saveDocumentLevelBusinessConcepts(source.projectId(), domainId, subdomainId, docKnowledge);
    }

    Map<String, UUID> componentIds =
        saveComponents(
            extractComponents(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId));
    saveBusinessRules(
        extractBusinessRules(sourceDocumentId, source, chunkKnowledge, domainId, componentIds));
    saveWorkflows(extractWorkflows(sourceDocumentId, source, chunkKnowledge));
    saveApis(extractApis(sourceDocumentId, source, chunkKnowledge, componentIds));
    saveDataModels(extractDataModels(sourceDocumentId, source, chunkKnowledge, domainId));
    saveIntegrations(extractIntegrations(sourceDocumentId, source, chunkKnowledge, componentIds));
    saveResources(extractResources(sourceDocumentId, source, chunkKnowledge));
    saveRelationships(sourceDocumentId, source, extractRelationships(chunkKnowledge));

    // Save chunk-level business entities
    saveChunkBusinessCapabilities(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
    saveChunkBusinessRoles(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
    saveChunkBusinessDecisions(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
    saveChunkBusinessTerms(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
    saveChunkBusinessPolicies(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
    saveChunkBusinessMetrics(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
    saveChunkKnowledgeNotes(sourceDocumentId, source, chunkKnowledge, domainId, subdomainId);
  }

  /**
   * Extracts technical components (solution components) from chunk-level extraction results.
   *
   * <p>Processes the technicalComponents field (aliased from "solutionComponents" in the prompt) to
   * create KnowledgeComponent entities. These represent HOW capabilities are implemented (technical
   * solutions like services, APIs, modules) rather than WHAT the business does.
   *
   * <p><strong>Note:</strong> Business capabilities are now saved separately via
   * saveChunkBusinessCapabilities method, providing clear separation between business-level
   * capabilities and technical implementation components.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunkKnowledge list of chunk extraction results
   * @param domainId the domain identifier
   * @param subdomainId the subdomain identifier
   * @return list of extracted technical components
   */
  private List<KnowledgeComponent> extractComponents(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunkKnowledge,
      UUID domainId,
      UUID subdomainId) {
    List<KnowledgeComponent> components = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunkKnowledge) {
      if (chunk.getTechnicalComponents() != null) {
        chunk
            .getTechnicalComponents()
            .forEach(
                tc ->
                    components.add(
                        KnowledgeComponent.builder()
                            .sourceDocumentId(sourceDocumentId)
                            .projectId(source.projectId())
                            .domainId(domainId)
                            .subdomainId(subdomainId)
                            .componentName(
                                firstNonBlank(
                                    tc.getComponentName(),
                                    tc.getResponsibility(),
                                    "Technical component from " + source.title()))
                            .componentType(tc.getComponentType())
                            .category("technical")
                            .responsibility(tc.getResponsibility())
                            .technology(tc.getTechnology())
                            .lifecycle(tc.getLifecycle())
                            .confidence(tc.getConfidence())
                            .build()));
      }
    }
    return components;
  }

  private Map<String, UUID> saveComponents(List<KnowledgeComponent> components) {
    Map<String, UUID> ids = new HashMap<>();
    for (KnowledgeComponent component : components) {
      component.setEmbedding(embeddingService.embedComponent(component));
      ids.put(component.getComponentName(), storageService.saveKnowledgeComponent(component));
    }
    return ids;
  }

  private List<KnowledgeBusinessRule> extractBusinessRules(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      Map<String, UUID> componentIds) {
    List<KnowledgeBusinessRule> rules = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessRules() == null) {
        continue;
      }
      chunk
          .getBusinessRules()
          .forEach(
              br ->
                  rules.add(
                      KnowledgeBusinessRule.builder()
                          .sourceDocumentId(sourceDocumentId)
                          .componentId(componentIds.get(br.getSourceBusinessComponentName()))
                          .projectId(source.projectId())
                          .domainId(domainId)
                          .ruleName(
                              firstNonBlank(
                                  br.getRuleName(),
                                  br.getCondition(),
                                  "Business rule from " + source.title()))
                          .ruleType(br.getRuleType())
                          .conditionText(br.getCondition())
                          .outcomeText(br.getOutcome())
                          .priority(br.getPriority())
                          .confidence(br.getConfidence())
                          .technicalImplementation(br.getTechnicalImplementation())
                          .validationCriteria(br.getValidationCriteria())
                          .build()));
    }
    return rules;
  }

  private void saveBusinessRules(List<KnowledgeBusinessRule> rules) {
    saveEntities(
        rules,
        rule -> rule.setEmbedding(embeddingService.embedBusinessRule(rule)),
        storageService::saveKnowledgeBusinessRule);
  }

  private List<KnowledgeWorkflow> extractWorkflows(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks) {
    List<KnowledgeWorkflow> workflows = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessFlows() == null) {
        continue;
      }
      chunk
          .getBusinessFlows()
          .forEach(
              flow -> {
                List<KnowledgeWorkflowStep> steps = new ArrayList<>();
                if (flow.getSteps() != null) {
                  flow.getSteps()
                      .forEach(
                          step ->
                              steps.add(
                                  KnowledgeWorkflowStep.builder()
                                      .sequenceNumber(step.getSequence())
                                      .stepName(step.getStepName())
                                      .actor(step.getActor())
                                      .actionText(step.getAction())
                                      .inputData(step.getInput())
                                      .outputData(step.getOutput())
                                      .nextStep(step.getNextStep())
                                      .technicalDetails(step.getTechnicalDetails())
                                      .inputParameters(step.getInputParameters())
                                      .outputParameters(step.getOutputParameters())
                                      .build()));
                }
                workflows.add(
                    KnowledgeWorkflow.builder()
                        .sourceDocumentId(sourceDocumentId)
                        .projectId(source.projectId())
                        .workflowName(
                            firstNonBlank(
                                flow.getFlowName(),
                                flow.getTrigger(),
                                "Workflow from " + source.title()))
                        .triggerText(flow.getTrigger())
                        .outcomeText(flow.getOutcome())
                        .owner(flow.getOwner())
                        .confidence(flow.getConfidence())
                        .steps(steps)
                        .build());
              });
    }
    return workflows;
  }

  private List<KnowledgeAPI> extractApis(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      Map<String, UUID> componentIds) {
    List<KnowledgeAPI> apis = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getApis() == null) {
        continue;
      }
      chunk
          .getApis()
          .forEach(
              api -> {
                api.setSourceDocumentId(sourceDocumentId);
                api.setProjectId(source.projectId());
                api.setApiName(
                    firstNonBlank(
                        api.getApiName(), api.getEndpointPath(), "API from " + source.title()));
                if (api.getComponentId() == null && api.getApiName() != null) {
                  api.setComponentId(componentIds.get(api.getApiName()));
                }
                apis.add(api);
              });
    }
    return apis;
  }

  private void saveApis(List<KnowledgeAPI> apis) {
    saveEntities(
        apis,
        api -> api.setEmbedding(embeddingService.embedAPI(api)),
        storageService::saveKnowledgeAPI);
  }

  private List<KnowledgeDataModel> extractDataModels(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId) {
    List<KnowledgeDataModel> dataModels = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getDataModels() == null) {
        continue;
      }
      chunk
          .getDataModels()
          .forEach(
              dataModel -> {
                dataModel.setSourceDocumentId(sourceDocumentId);
                dataModel.setProjectId(source.projectId());
                dataModel.setDomainId(domainId);
                dataModel.setModelName(
                    firstNonBlank(dataModel.getModelName(), "Data model from " + source.title()));
                dataModels.add(dataModel);
              });
    }
    return dataModels;
  }

  /**
   * Saves data models and their fields while supplying required field defaults.
   *
   * <p>LLM extraction can return useful field descriptions without stable field names. The fallback
   * sequence preserves real names when present, uses descriptions when available, and finally
   * generates a deterministic placeholder that satisfies persistence constraints.
   *
   * <p>For each data model:
   *
   * <ol>
   *   <li>Generates and sets vector embedding for semantic search
   *   <li>Saves the data model entity and obtains the generated model ID
   *   <li>For each field in the data model:
   *       <ul>
   *         <li>Inherits projectId from parent data model (required for DB constraint)
   *         <li>Associates field with parent model via dataModelId
   *         <li>Applies intelligent fallback logic for field name (name → description → generated)
   *         <li>Sets default field type if not provided by LLM
   *         <li>Persists the field to the database
   *       </ul>
   * </ol>
   *
   * @param dataModels list of data models to save, including their nested fields
   */
  private void saveDataModels(List<KnowledgeDataModel> dataModels) {
    for (KnowledgeDataModel dataModel : dataModels) {
      dataModel.setEmbedding(embeddingService.embedDataModel(dataModel));
      UUID dataModelId = storageService.saveKnowledgeDataModel(dataModel);
      if (dataModel.getFields() != null) {
        int[] fieldIndex = {1};
        dataModel
            .getFields()
            .forEach(
                field -> {
                  // Inherit projectId from parent data model to satisfy NOT NULL constraint
                  field.setProjectId(dataModel.getProjectId());
                  field.setDataModelId(dataModelId);
                  field.setFieldName(
                      bounded(
                          firstNonBlank(
                              field.getFieldName(),
                              field.getDescription(),
                              "Field "
                                  + fieldIndex[0]
                                  + " in "
                                  + firstNonBlank(dataModel.getModelName(), "data model")),
                          255));
                  field.setFieldType(firstNonBlank(field.getFieldType(), "unknown"));
                  fieldIndex[0]++;
                  storageService.saveKnowledgeDataField(field);
                });
      }
    }
  }

  private List<KnowledgeIntegration> extractIntegrations(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      Map<String, UUID> componentIds) {
    List<KnowledgeIntegration> integrations = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getIntegrations() == null) {
        continue;
      }
      chunk
          .getIntegrations()
          .forEach(
              integration -> {
                integration.setSourceDocumentId(sourceDocumentId);
                integration.setProjectId(source.projectId());
                if (integration.getComponentId() == null) {
                  integration.setComponentId(componentIds.get(integration.getSourceSystem()));
                }
                integrations.add(integration);
              });
    }
    return integrations;
  }

  private void saveIntegrations(List<KnowledgeIntegration> integrations) {
    saveEntities(
        integrations,
        integration -> integration.setEmbedding(embeddingService.embedIntegration(integration)),
        storageService::saveKnowledgeIntegration);
  }

  /**
   * Persists workflows and their associated steps to the database.
   *
   * <p>This method performs the following operations for each workflow:
   *
   * <ol>
   *   <li>Generates and sets vector embedding for semantic search
   *   <li>Saves the workflow entity and obtains the generated workflow ID
   *   <li>For each step in the workflow:
   *       <ul>
   *         <li>Inherits projectId from parent workflow (required for DB constraint)
   *         <li>Associates step with parent workflow via workflowId
   *         <li>Generates vector embedding for the step
   *         <li>Persists the step to the database
   *       </ul>
   * </ol>
   *
   * @param workflows list of workflows to save, including their nested steps
   */
  private void saveWorkflows(List<KnowledgeWorkflow> workflows) {
    for (KnowledgeWorkflow workflow : workflows) {
      workflow.setEmbedding(embeddingService.embedWorkflow(workflow));
      UUID workflowId = storageService.saveKnowledgeWorkflow(workflow);
      if (workflow.getSteps() != null) {
        workflow
            .getSteps()
            .forEach(
                step -> {
                  // Inherit projectId from parent workflow to satisfy NOT NULL constraint
                  step.setProjectId(workflow.getProjectId());
                  step.setWorkflowId(workflowId);
                  step.setEmbedding(embeddingService.embedWorkflowStep(step));
                  storageService.saveKnowledgeWorkflowStep(step);
                });
      }
    }
  }

  private List<KnowledgeResource> extractResources(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks) {
    List<KnowledgeResource> resources = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getDeploymentResources() == null) {
        continue;
      }
      chunk
          .getDeploymentResources()
          .forEach(
              dr ->
                  resources.add(
                      KnowledgeResource.builder()
                          .sourceDocumentId(sourceDocumentId)
                          .projectId(source.projectId())
                          .resourceName(
                              firstNonBlank(
                                  dr.getResourceName(),
                                  "Deployment resource from " + source.title()))
                          .resourceType(dr.getResourceType())
                          .provider(dr.getProvider())
                          .environment(dr.getEnvironment())
                          .region(dr.getRegion())
                          .criticality(dr.getCriticality())
                          .lifecycle(dr.getLifecycle())
                          .confidence(dr.getConfidence())
                          .build()));
    }
    return resources;
  }

  private void saveResources(List<KnowledgeResource> resources) {
    saveEntities(
        resources,
        resource -> resource.setEmbedding(embeddingService.embedResource(resource)),
        storageService::saveKnowledgeResource);
  }

  private List<KnowledgeRelationship> extractRelationships(List<KnowledgeExtractionResult> chunks) {
    List<KnowledgeRelationship> relationships = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getRelationships() != null) {
        relationships.addAll(chunk.getRelationships());
      }
    }
    return relationships;
  }

  private void saveRelationships(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeRelationship> relationships) {
    relationships.forEach(
        relationship ->
            storageService.saveKnowledgeRelationship(
                sourceDocumentId, source.projectId(), relationship));
  }

  /**
   * Saves document-level business concepts extracted during document-level analysis.
   *
   * <p>This method persists five categories of business concepts that are identified during the
   * initial document-level analysis phase:
   *
   * <ul>
   *   <li><b>Capabilities</b> - Business functions and what the organization does
   *   <li><b>Roles</b> - Actors (human or system) that interact with processes
   *   <li><b>Terms</b> - Domain vocabulary and ubiquitous language
   *   <li><b>Policies</b> - Rules, constraints, and compliance requirements
   *   <li><b>Decisions</b> - Critical decision points within workflows
   * </ul>
   *
   * <p>These entities are extracted early to establish a consistent vocabulary that is used during
   * chunk-level extraction for entity linking. This ensures that when the same capability, role, or
   * term is mentioned in different chunks, it is linked to the same knowledge graph entity.
   *
   * <p>All entities are saved with high confidence (0.9) since they come from document-level
   * analysis which provides better context than individual chunk analysis.
   *
   * @param projectId the project these concepts belong to
   * @param domainId the domain these concepts are associated with (nullable)
   * @param subdomainId the subdomain these concepts are associated with (nullable)
   * @param docKnowledge the document-level knowledge containing lists of identified business
   *     concepts
   */
  private void saveDocumentLevelBusinessConcepts(
      UUID projectId, UUID domainId, UUID subdomainId, DocumentKnowledge docKnowledge) {
    // High confidence for document-level analysis - better context than chunk-level
    double confidence = 0.9;

    // Save capabilities with fail-safe handling
    if (docKnowledge.getIdentifiedCapabilities() != null) {
      docKnowledge.getIdentifiedCapabilities().stream()
          .filter(capability -> capability != null && !capability.isBlank())
          .forEach(
              capability ->
                  safeSave(
                      () ->
                          storageService.saveKnowledgeCapability(
                              projectId, domainId, subdomainId, capability, confidence),
                      "capability",
                      capability));
    }

    // Save roles with fail-safe handling
    if (docKnowledge.getIdentifiedRoles() != null) {
      docKnowledge.getIdentifiedRoles().stream()
          .filter(role -> role != null && !role.isBlank())
          .forEach(
              role ->
                  safeSave(
                      () ->
                          storageService.saveKnowledgeRole(
                              projectId, domainId, subdomainId, role, confidence),
                      "role",
                      role));
    }

    // Save terms with fail-safe handling
    if (docKnowledge.getIdentifiedTerms() != null) {
      docKnowledge.getIdentifiedTerms().stream()
          .filter(term -> term != null && !term.isBlank())
          .forEach(
              term ->
                  safeSave(
                      () ->
                          storageService.saveKnowledgeTerm(
                              projectId, domainId, subdomainId, term, confidence),
                      "term",
                      term));
    }

    // Save policies with fail-safe handling
    if (docKnowledge.getIdentifiedPolicies() != null) {
      docKnowledge.getIdentifiedPolicies().stream()
          .filter(policy -> policy != null && !policy.isBlank())
          .forEach(
              policy ->
                  safeSave(
                      () ->
                          storageService.saveKnowledgePolicy(
                              projectId, domainId, subdomainId, policy, confidence),
                      "policy",
                      policy));
    }

    // Save decisions with fail-safe handling
    if (docKnowledge.getIdentifiedDecisions() != null) {
      docKnowledge.getIdentifiedDecisions().stream()
          .filter(decision -> decision != null && !decision.isBlank())
          .forEach(
              decision ->
                  safeSave(
                      () ->
                          storageService.saveKnowledgeDecision(
                              projectId, domainId, subdomainId, decision, confidence),
                      "decision",
                      decision));
    }
  }

  /**
   * Saves business metrics extracted from chunks to the knowledge graph.
   *
   * <p>Processes KPIs and performance indicators found in chunk-level extraction results and
   * persists them for performance tracking and analytics.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results
   * @param domainId the domain this metric is associated with
   * @param subdomainId the subdomain this metric is associated with
   */
  /**
   * Saves business roles extracted from chunks to the knowledge graph.
   *
   * <p>Processes roles and actors found in chunk-level extraction results (including CSV files) and
   * persists them to track who performs actions, owns capabilities, and participates in workflows.
   *
   * <p>This method handles roles from both regular document chunks and CSV mapping files where
   * roles may be specified in owner, approver, or actor columns.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results (including CSV extraction results)
   * @param domainId the domain this role is associated with
   * @param subdomainId the subdomain this role is associated with
   */
  private void saveChunkBusinessRoles(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessRoles() == null) {
        continue;
      }
      for (BusinessRole role : chunk.getBusinessRoles()) {
        if (role.getRoleName() == null || role.getRoleName().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO to preserve all fields
        storageService.saveKnowledgeRole(source.projectId(), domainId, subdomainId, role);
      }
    }
  }

  /**
   * Saves business decisions extracted from chunks to the knowledge graph.
   *
   * <p>Processes decision points found in chunk-level extraction results (including CSV files) and
   * persists them to understand critical choice points in workflows and processes.
   *
   * <p>CSV files often contain decision logic in columns like "condition", "criteria", "approval",
   * or "decision_point" which are extracted and stored as business decisions.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results (including CSV extraction results)
   * @param domainId the domain this decision is associated with
   * @param subdomainId the subdomain this decision is associated with
   */
  private void saveChunkBusinessDecisions(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessDecisions() == null) {
        continue;
      }
      for (BusinessDecision decision : chunk.getBusinessDecisions()) {
        if (decision.getDecisionName() == null || decision.getDecisionName().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO to preserve all fields
        storageService.saveKnowledgeDecision(source.projectId(), domainId, subdomainId, decision);
      }
    }
  }

  /**
   * Saves business terms extracted from chunks to the knowledge graph.
   *
   * <p>Processes domain vocabulary and terminology found in chunk-level extraction results
   * (including CSV files) and persists them to build a consistent ubiquitous language for the
   * project.
   *
   * <p>CSV files with glossary columns or term definitions are particularly valuable sources of
   * business terms that establish shared vocabulary across the organization.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results (including CSV extraction results)
   * @param domainId the domain this term is associated with
   * @param subdomainId the subdomain this term is associated with
   */
  private void saveChunkBusinessTerms(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessTerms() == null) {
        continue;
      }
      for (BusinessTerm term : chunk.getBusinessTerms()) {
        if (term.getTermName() == null || term.getTermName().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO to preserve all fields
        storageService.saveKnowledgeTerm(source.projectId(), domainId, subdomainId, term);
      }
    }
  }

  /**
   * Saves business policies extracted from chunks to the knowledge graph.
   *
   * <p>Processes policies and compliance requirements found in chunk-level extraction results
   * (including CSV files) and persists them to track governance, regulatory, and operational
   * policies.
   *
   * <p>CSV files often contain policy references in columns related to compliance, regulations, or
   * governance requirements.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results (including CSV extraction results)
   * @param domainId the domain this policy is associated with
   * @param subdomainId the subdomain this policy is associated with
   */
  private void saveChunkBusinessPolicies(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessPolicies() == null) {
        continue;
      }
      for (BusinessPolicy policy : chunk.getBusinessPolicies()) {
        if (policy.getPolicyName() == null || policy.getPolicyName().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO to preserve all fields
        storageService.saveKnowledgePolicy(source.projectId(), domainId, subdomainId, policy);
      }
    }
  }

  /**
   * Saves business metrics extracted from chunks to the knowledge graph.
   *
   * <p>Processes KPIs and performance indicators found in chunk-level extraction results (including
   * CSV files) and persists them for performance tracking and analytics.
   *
   * <p>CSV files often contain metrics in columns related to measurements, KPIs, targets, or SLAs
   * which are extracted and linked to capabilities, workflows, or components.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results (including CSV extraction results)
   * @param domainId the domain this metric is associated with
   * @param subdomainId the subdomain this metric is associated with
   */
  private void saveChunkBusinessMetrics(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessMetrics() == null) {
        continue;
      }
      for (BusinessMetric metric : chunk.getBusinessMetrics()) {
        if (metric.getMetricName() == null || metric.getMetricName().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO to preserve all fields
        storageService.saveKnowledgeMetric(source.projectId(), domainId, subdomainId, metric);
      }
    }
  }

  /**
   * Saves knowledge notes extracted from chunks to the knowledge graph.
   *
   * <p>Processes miscellaneous insights such as architecture decisions, constraints, assumptions,
   * risks, and recommendations found in chunk-level extraction results.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results
   * @param domainId the domain this note is associated with
   * @param subdomainId the subdomain this note is associated with
   */
  private void saveChunkKnowledgeNotes(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getKnowledgeNotes() == null) {
        continue;
      }
      for (KnowledgeNote note : chunk.getKnowledgeNotes()) {
        if (note.getContent() == null || note.getContent().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO for consistency
        safeSave(
            () -> storageService.saveKnowledgeNote(source.projectId(), domainId, subdomainId, note),
            "note",
            note.getContent().substring(0, Math.min(50, note.getContent().length())));
      }
    }
  }

  /**
   * Saves business capabilities extracted from chunk-level analysis (including CSV).
   *
   * <p>Processes businessCapabilities from extraction results and persists them using the detailed
   * save method that preserves capability type, description, business value, and other metadata.
   *
   * <p><strong>Capability vs Component:</strong>
   *
   * <ul>
   *   <li><b>Business Capability (this method):</b> WHAT the business does - technology-independent
   *       business functions (e.g., "Order Management", "Customer Onboarding", "Risk Assessment")
   *   <li><b>Technical Component:</b> HOW it's implemented - technical solutions and services
   *       (e.g., "OrderService", "CustomerAPI", "PaymentGateway")
   * </ul>
   *
   * <p>Business capabilities provide a stable view of the business that doesn't change even when
   * technology stacks, processes, or organizational structures evolve. They are essential for
   * business architecture and capability mapping.
   *
   * @param sourceDocumentId the source document identifier
   * @param source the source metadata containing project information
   * @param chunks the chunk-level extraction results (including CSV extraction results)
   * @param domainId the domain this capability is associated with
   * @param subdomainId the subdomain this capability is associated with
   */
  private void saveChunkBusinessCapabilities(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunks,
      UUID domainId,
      UUID subdomainId) {
    for (KnowledgeExtractionResult chunk : chunks) {
      if (chunk.getBusinessCapabilities() == null) {
        continue;
      }
      for (BusinessCapability capability : chunk.getBusinessCapabilities()) {
        if (capability.getCapabilityName() == null || capability.getCapabilityName().isBlank()) {
          continue;
        }
        // Use overloaded method that accepts full DTO to preserve all fields
        safeSave(
            () ->
                storageService.saveKnowledgeCapability(
                    source.projectId(), domainId, subdomainId, capability),
            "capability",
            capability.getCapabilityName());
      }
    }
  }

  /**
   * Applies the entity-specific enrichment step before writing each entity with fail-safe error
   * handling.
   *
   * <p>Wraps each entity save operation in a try-catch to ensure one malformed entity doesn't
   * prevent others from being saved. Logs errors and continues processing.
   *
   * @param entities entities to persist
   * @param enricher callback that attaches derived fields such as embeddings
   * @param saver persistence callback returning the stored entity identifier
   * @param <T> entity type
   */
  private <T> void saveEntities(List<T> entities, Consumer<T> enricher, Function<T, UUID> saver) {
    for (T entity : entities) {
      try {
        enricher.accept(entity);
        saver.apply(entity);
      } catch (Exception e) {
        log.warn("Failed to save entity {}: {}", entity.getClass().getSimpleName(), e.getMessage());
      }
    }
  }

  /**
   * Executes a save operation with fail-safe error handling.
   *
   * <p>Wraps save operations in try-catch to ensure one malformed entity doesn't prevent others
   * from being saved. Logs errors with entity type and name for troubleshooting.
   *
   * @param saveOperation the save operation to execute
   * @param entityType the type of entity being saved (for logging)
   * @param entityName the name of the entity being saved (for logging)
   */
  private void safeSave(Runnable saveOperation, String entityType, String entityName) {
    try {
      saveOperation.run();
    } catch (Exception e) {
      log.warn("Failed to save {} '{}': {}", entityType, entityName, e.getMessage());
    }
  }

  /** Returns the first non-blank value, or {@code Unknown} when every candidate is blank. */
  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "Unknown";
  }

  /** Truncates a value to the database-safe maximum length without changing blank handling. */
  private String bounded(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
