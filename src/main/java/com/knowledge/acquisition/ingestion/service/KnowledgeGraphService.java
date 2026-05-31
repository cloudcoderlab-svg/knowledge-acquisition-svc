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
 * <p>This service is intentionally defensive about AI output quality. Names that participate in
 * database constraints are filled from nearby descriptive fields when the model omits them, and
 * embeddings are attached immediately before persistence so quota-aware embedding failures can be
 * handled by the embedding layer without dropping the entity.
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
   * @param sourceDocumentId stored source document identifier
   * @param source metadata for the source object and project
   * @param docKnowledge document-level classification and summary
   * @param chunkKnowledge chunk-level extraction results to normalize and persist
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
      domainId = storageService.ensureDomain(source.projectId(), docKnowledge.getDomain());
      if (docKnowledge.getSubdomain() != null) {
        subdomainId =
            storageService.ensureSubdomain(
                source.projectId(),
                docKnowledge.getDomain(),
                domainId,
                docKnowledge.getSubdomain());
      }
      storageService.saveDocumentKnowledge(sourceDocumentId, source.projectId(), docKnowledge);
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
  }

  private List<KnowledgeComponent> extractComponents(
      UUID sourceDocumentId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunkKnowledge,
      UUID domainId,
      UUID subdomainId) {
    List<KnowledgeComponent> components = new ArrayList<>();
    for (KnowledgeExtractionResult chunk : chunkKnowledge) {
      if (chunk.getBusinessComponents() != null) {
        chunk
            .getBusinessComponents()
            .forEach(
                bc ->
                    components.add(
                        KnowledgeComponent.builder()
                            .sourceDocumentId(sourceDocumentId)
                            .projectId(source.projectId())
                            .domainId(domainId)
                            .subdomainId(subdomainId)
                            .componentName(
                                firstNonBlank(
                                    bc.getComponentName(),
                                    bc.getCapability(),
                                    "Business component from " + source.title()))
                            .componentType(bc.getComponentType())
                            .category("business")
                            .capability(bc.getCapability())
                            .owner(bc.getOwner())
                            .lifecycle(bc.getLifecycle())
                            .confidence(bc.getConfidence())
                            .build()));
      }
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

  private void saveWorkflows(List<KnowledgeWorkflow> workflows) {
    for (KnowledgeWorkflow workflow : workflows) {
      workflow.setEmbedding(embeddingService.embedWorkflow(workflow));
      UUID workflowId = storageService.saveKnowledgeWorkflow(workflow);
      if (workflow.getSteps() != null) {
        workflow
            .getSteps()
            .forEach(
                step -> {
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
   * Applies the entity-specific enrichment step before writing each entity.
   *
   * @param entities entities to persist
   * @param enricher callback that attaches derived fields such as embeddings
   * @param saver persistence callback returning the stored entity identifier
   * @param <T> entity type
   */
  private <T> void saveEntities(List<T> entities, Consumer<T> enricher, Function<T, UUID> saver) {
    for (T entity : entities) {
      enricher.accept(entity);
      saver.apply(entity);
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
