package com.knowledge.acquisition.ingestion.service.ai;

import com.knowledge.acquisition.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates embeddings for knowledge entities. Each entity type has a specific strategy for
 * combining fields into embeddable text.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeEntityEmbeddingService {

  private final EmbeddingService embeddingService;

  @Value("${knowledge-engine.extraction.enable-entity-embeddings:true}")
  private boolean enableEntityEmbeddings;

  /**
   * Generates embedding for a knowledge component. Combines: component name, type, description, and
   * responsibility.
   */
  public List<Double> embedComponent(KnowledgeComponent component) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildComponentText(component);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for component: {}", component.getComponentName(), e);
      return null;
    }
  }

  /**
   * Generates embedding for a knowledge API. Combines: API name, endpoint, HTTP method, and
   * description.
   */
  public List<Double> embedAPI(KnowledgeAPI api) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildAPIText(api);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for API: {}", api.getApiName(), e);
      return null;
    }
  }

  /** Generates embedding for a business rule. Combines: rule name, condition, and outcome. */
  public List<Double> embedBusinessRule(KnowledgeBusinessRule rule) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildBusinessRuleText(rule);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for business rule: {}", rule.getRuleName(), e);
      return null;
    }
  }

  /**
   * Generates embedding for a workflow. Combines: workflow name, trigger, outcome, and step
   * summaries.
   */
  public List<Double> embedWorkflow(KnowledgeWorkflow workflow) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildWorkflowText(workflow);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for workflow: {}", workflow.getWorkflowName(), e);
      return null;
    }
  }

  /** Generates embedding for a workflow step. */
  public List<Double> embedWorkflowStep(KnowledgeWorkflowStep step) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildWorkflowStepText(step);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for workflow step: {}", step.getStepName(), e);
      return null;
    }
  }

  /** Generates embedding for a data model. Combines: model name, type, and description. */
  public List<Double> embedDataModel(KnowledgeDataModel dataModel) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildDataModelText(dataModel);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for data model: {}", dataModel.getModelName(), e);
      return null;
    }
  }

  /** Generates embedding for an integration. */
  public List<Double> embedIntegration(KnowledgeIntegration integration) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildIntegrationText(integration);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error(
          "Failed to generate embedding for integration: {}", integration.getIntegrationName(), e);
      return null;
    }
  }

  /** Generates embedding for a resource. */
  public List<Double> embedResource(KnowledgeResource resource) {
    if (!enableEntityEmbeddings) {
      return null;
    }

    try {
      String embeddableText = buildResourceText(resource);
      return embeddingService.embedding(embeddableText);
    } catch (Exception e) {
      log.error("Failed to generate embedding for resource: {}", resource.getResourceName(), e);
      return null;
    }
  }

  // ===== Text Building Methods =====

  private String buildComponentText(KnowledgeComponent component) {
    StringBuilder text = new StringBuilder();
    text.append("Component: ").append(component.getComponentName());
    if (component.getComponentType() != null) {
      text.append(" (").append(component.getComponentType()).append(")");
    }
    if (component.getDescription() != null) {
      text.append(". ").append(component.getDescription());
    }
    if (component.getResponsibility() != null) {
      text.append(". Responsibility: ").append(component.getResponsibility());
    }
    if (component.getTechnology() != null) {
      text.append(". Technology: ").append(component.getTechnology());
    }
    return text.toString();
  }

  private String buildAPIText(KnowledgeAPI api) {
    StringBuilder text = new StringBuilder();
    text.append("API: ").append(api.getApiName());
    if (api.getHttpMethod() != null && api.getEndpointPath() != null) {
      text.append(" (")
          .append(api.getHttpMethod())
          .append(" ")
          .append(api.getEndpointPath())
          .append(")");
    }
    if (api.getDescription() != null) {
      text.append(". ").append(api.getDescription());
    }
    if (api.getApiType() != null) {
      text.append(". Type: ").append(api.getApiType());
    }
    return text.toString();
  }

  private String buildBusinessRuleText(KnowledgeBusinessRule rule) {
    StringBuilder text = new StringBuilder();
    text.append("Business Rule: ").append(rule.getRuleName());
    if (rule.getRuleType() != null) {
      text.append(" (").append(rule.getRuleType()).append(")");
    }
    if (rule.getConditionText() != null) {
      text.append(". When: ").append(rule.getConditionText());
    }
    if (rule.getOutcomeText() != null) {
      text.append(". Then: ").append(rule.getOutcomeText());
    }
    return text.toString();
  }

  private String buildWorkflowText(KnowledgeWorkflow workflow) {
    StringBuilder text = new StringBuilder();
    text.append("Workflow: ").append(workflow.getWorkflowName());
    if (workflow.getTriggerText() != null) {
      text.append(". Triggered by: ").append(workflow.getTriggerText());
    }
    if (workflow.getOutcomeText() != null) {
      text.append(". Outcome: ").append(workflow.getOutcomeText());
    }
    if (workflow.getSteps() != null && !workflow.getSteps().isEmpty()) {
      text.append(". Steps: ");
      workflow
          .getSteps()
          .forEach(
              step -> {
                if (step.getStepName() != null) {
                  text.append(step.getStepName()).append("; ");
                }
              });
    }
    return text.toString();
  }

  private String buildWorkflowStepText(KnowledgeWorkflowStep step) {
    StringBuilder text = new StringBuilder();
    text.append("Step ").append(step.getSequenceNumber()).append(": ").append(step.getStepName());
    if (step.getActor() != null) {
      text.append(" (by ").append(step.getActor()).append(")");
    }
    if (step.getActionText() != null) {
      text.append(". ").append(step.getActionText());
    }
    return text.toString();
  }

  private String buildDataModelText(KnowledgeDataModel dataModel) {
    StringBuilder text = new StringBuilder();
    text.append("Data Model: ").append(dataModel.getModelName());
    if (dataModel.getModelType() != null) {
      text.append(" (").append(dataModel.getModelType()).append(")");
    }
    if (dataModel.getDescription() != null) {
      text.append(". ").append(dataModel.getDescription());
    }
    if (dataModel.getFields() != null && !dataModel.getFields().isEmpty()) {
      text.append(". Fields: ");
      dataModel
          .getFields()
          .forEach(
              field -> {
                text.append(field.getFieldName())
                    .append(" (")
                    .append(field.getFieldType())
                    .append("), ");
              });
    }
    return text.toString();
  }

  private String buildIntegrationText(KnowledgeIntegration integration) {
    StringBuilder text = new StringBuilder();
    text.append("Integration: ").append(integration.getIntegrationName());
    if (integration.getSourceSystem() != null && integration.getTargetSystem() != null) {
      text.append(" (")
          .append(integration.getSourceSystem())
          .append(" -> ")
          .append(integration.getTargetSystem())
          .append(")");
    }
    if (integration.getDescription() != null) {
      text.append(". ").append(integration.getDescription());
    }
    if (integration.getProtocol() != null) {
      text.append(". Protocol: ").append(integration.getProtocol());
    }
    return text.toString();
  }

  private String buildResourceText(KnowledgeResource resource) {
    StringBuilder text = new StringBuilder();
    text.append("Resource: ").append(resource.getResourceName());
    if (resource.getResourceType() != null) {
      text.append(" (").append(resource.getResourceType()).append(")");
    }
    if (resource.getProvider() != null) {
      text.append(". Provider: ").append(resource.getProvider());
    }
    if (resource.getEnvironment() != null) {
      text.append(". Environment: ").append(resource.getEnvironment());
    }
    return text.toString();
  }
}
