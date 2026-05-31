package com.knowledge.acquisition.ingestion.service;

import com.knowledge.acquisition.entity.*;
import com.knowledge.acquisition.ingestion.service.ai.EmbeddingService;
import com.knowledge.acquisition.ingestion.util.EmbeddingUtils;
import com.knowledge.acquisition.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating semantic knowledge chunks from extracted knowledge entities.
 *
 * <p>Knowledge chunks are text representations of various knowledge entities (domains, modules,
 * components, business rules, workflows, relationships) that are optimized for semantic search.
 * Each chunk includes:
 *
 * <ul>
 *   <li>Textual description of the entity
 *   <li>Vector embedding for similarity search
 *   <li>Metadata linking back to the source entity
 * </ul>
 *
 * <p><strong>Purpose:</strong> Enables efficient semantic search across the entire knowledge base
 * regardless of entity type. Instead of searching separate tables (components, workflows, rules),
 * users can perform a single vector similarity search across all knowledge chunks.
 *
 * <p><strong>Consolidation Flow:</strong> This service is invoked during the consolidation phase
 * after cross-document relationships are established. It regenerates all knowledge chunks to
 * reflect the latest state of the knowledge graph.
 *
 * @author Knowledge Engine Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkGenerationService {
  private final DomainRepository domainRepository;
  private final ModuleRepository moduleRepository;
  private final ComponentRepository componentRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final WorkflowRepository workflowRepository;
  private final RelationshipRepository relationshipRepository;
  private final KnowledgeChunkRepository knowledgeChunkRepository;
  private final EmbeddingService embeddingService;

  /**
   * Regenerates all knowledge chunks for a project from current knowledge entities.
   *
   * <p>This method performs a complete refresh of the knowledge chunk index:
   *
   * <ol>
   *   <li>Deletes all existing chunks for the project (using bulk delete)
   *   <li>Iterates through all knowledge entities (domains, modules, components, rules, workflows,
   *       relationships)
   *   <li>Generates text representation for each entity
   *   <li>Creates vector embedding using text-embedding-005 model
   *   <li>Saves new chunk with embedding to database
   * </ol>
   *
   * <p><strong>Use Cases:</strong>
   *
   * <ul>
   *   <li>After consolidation phase to include cross-document relationships
   *   <li>When knowledge entities are updated and search index needs refresh
   *   <li>To rebuild chunks with new embedding model
   * </ul>
   *
   * <p><strong>Performance:</strong> Processing time depends on number of entities and embedding
   * API latency. Typical projects with 100-200 entities complete in 3-5 minutes.
   *
   * @param projectId the unique identifier of the project to refresh chunks for
   * @return total number of knowledge chunks generated and saved
   */
  @Transactional
  public int refreshKnowledgeChunks(UUID projectId) {
    // Delete all existing chunks using bulk delete to avoid row count errors
    knowledgeChunkRepository.deleteByProjectId(projectId);

    int count = 0;

    // Generate chunks for domains
    for (DomainEntity domain : domainRepository.findByProjectId(projectId)) {
      save(projectId, "domain", "knowledge_domains", domain.getDomainId(), domainText(domain));
      count++;
    }

    // Generate chunks for modules
    for (ModuleEntity module : moduleRepository.findByProjectId(projectId)) {
      save(projectId, "module", "knowledge_modules", module.getModuleId(), moduleText(module));
      count++;
    }

    // Generate chunks for components
    for (ComponentEntity component : componentRepository.findByProjectId(projectId)) {
      save(
          projectId,
          "component",
          "knowledge_components",
          component.getComponentId(),
          componentText(component));
      count++;
    }

    // Generate chunks for business rules
    for (BusinessRuleEntity rule : businessRuleRepository.findByProjectId(projectId)) {
      save(projectId, "rule", "knowledge_business_rules", rule.getRuleId(), ruleText(rule));
      count++;
    }

    // Generate chunks for workflows
    for (WorkflowEntity workflow : workflowRepository.findByProjectId(projectId)) {
      save(
          projectId,
          "workflow",
          "knowledge_workflows",
          workflow.getWorkflowId(),
          workflowText(workflow));
      count++;
    }

    // Generate chunks for relationships (including cross-document)
    for (RelationshipEntity relationship : relationshipRepository.findByProjectId(projectId)) {
      save(
          projectId,
          "relationship",
          "knowledge_relationships",
          relationship.getRelationshipId(),
          relationshipText(relationship));
      count++;
    }

    return count;
  }

  /**
   * Saves a knowledge chunk with vector embedding to the database.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Validates content is non-null and non-blank
   *   <li>Generates vector embedding from content using embedding service
   *   <li>Converts embedding to string format for database storage
   *   <li>Persists chunk entity with all metadata
   * </ol>
   *
   * @param projectId the project identifier for this chunk
   * @param chunkType the type of chunk (e.g., "domain", "component", "rule")
   * @param entityType the database table name of the source entity
   * @param entityId the unique identifier of the source entity
   * @param content the text content to chunk and embed
   */
  private void save(
      UUID projectId, String chunkType, String entityType, UUID entityId, String content) {
    // Skip empty or blank content
    if (content == null || content.isBlank()) {
      return;
    }

    // Generate embedding and save chunk
    knowledgeChunkRepository.save(
        KnowledgeChunkEntity.builder()
            .projectId(projectId)
            .chunkType(chunkType)
            .entityType(entityType)
            .entityId(entityId)
            .content(content)
            .embedding(EmbeddingUtils.embeddingToString(embeddingService.embedding(content)))
            .build());
  }

  /**
   * Generates searchable text representation for a domain entity.
   *
   * @param domain the domain entity to convert to text
   * @return formatted text suitable for embedding and search
   */
  private String domainText(DomainEntity domain) {
    return "Domain: "
        + domain.getDomainName()
        + ". "
        + text(domain.getKnowledge(), domain.getDescription());
  }

  /**
   * Generates searchable text representation for a module entity.
   *
   * @param module the module entity to convert to text
   * @return formatted text suitable for embedding and search
   */
  private String moduleText(ModuleEntity module) {
    return "Module: "
        + module.getModuleName()
        + ". Type: "
        + module.getModuleType()
        + ". Responsibility: "
        + text(module.getResponsibility(), module.getKnowledge());
  }

  /**
   * Generates searchable text representation for a component entity.
   *
   * @param component the component entity to convert to text
   * @return formatted text suitable for embedding and search
   */
  private String componentText(ComponentEntity component) {
    return "Component: "
        + component.getComponentName()
        + ". Type: "
        + component.getComponentType()
        + ". Category: "
        + component.getCategory()
        + ". "
        + text(component.getKnowledge(), component.getResponsibility(), component.getCapability());
  }

  /**
   * Generates searchable text representation for a business rule entity.
   *
   * @param rule the business rule entity to convert to text
   * @return formatted text suitable for embedding and search
   */
  private String ruleText(BusinessRuleEntity rule) {
    return "Business rule: "
        + rule.getRuleName()
        + ". Condition: "
        + rule.getConditionText()
        + ". Outcome: "
        + rule.getOutcomeText()
        + ". Validation: "
        + rule.getValidationCriteria();
  }

  /**
   * Generates searchable text representation for a workflow entity.
   *
   * @param workflow the workflow entity to convert to text
   * @return formatted text suitable for embedding and search
   */
  private String workflowText(WorkflowEntity workflow) {
    return "Workflow: "
        + workflow.getWorkflowName()
        + ". Trigger: "
        + workflow.getTriggerText()
        + ". Outcome: "
        + workflow.getOutcomeText()
        + ". Actor: "
        + workflow.getActor();
  }

  /**
   * Generates searchable text representation for a relationship entity.
   *
   * @param relationship the relationship entity to convert to text
   * @return formatted text suitable for embedding and search
   */
  private String relationshipText(RelationshipEntity relationship) {
    return "Relationship: "
        + relationship.getSourceName()
        + " "
        + relationship.getRelationshipType()
        + " "
        + relationship.getTargetName()
        + ". "
        + text(relationship.getRelationshipDefinition(), relationship.getBusinessDescription());
  }

  /**
   * Concatenates non-null, non-blank string values with space separation.
   *
   * <p>Used by text generation methods to safely combine multiple optional text fields into a
   * single searchable string.
   *
   * @param values variable number of string values to concatenate
   * @return concatenated string with space-separated values, or empty string if all values are
   *     null/blank
   */
  private String text(String... values) {
    StringBuilder builder = new StringBuilder();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        if (!builder.isEmpty()) {
          builder.append(' ');
        }
        builder.append(value);
      }
    }
    return builder.toString();
  }
}
