package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.*;
import com.knowledge.acquisition.entity.*;
import com.knowledge.acquisition.ingestion.util.EmbeddingUtils;
import com.knowledge.acquisition.repository.*;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence layer service for storing knowledge graph entities in PostgreSQL.
 *
 * <p>This service provides methods to persist all types of knowledge entities extracted from
 * enterprise documentation, including:
 *
 * <ul>
 *   <li><b>Source documents and chunks:</b> Raw document metadata and chunked content
 *   <li><b>Document-level entities:</b> Domains, subdomains, modules, capabilities, roles, terms,
 *       policies, decisions
 *   <li><b>Chunk-level entities:</b> Components, APIs, business rules, workflows, data models,
 *       integrations, resources, relationships
 *   <li><b>CSV extraction entities:</b> Structured data from CSV files (mappings, rules, metrics,
 *       etc.)
 * </ul>
 *
 * <h3>Dual Save Method Pattern</h3>
 *
 * Many entity types have TWO save methods to support different extraction contexts:
 *
 * <ul>
 *   <li><b>Simple save methods:</b> Accept basic fields (name, type, confidence) for document-level
 *       extraction where only entity names are identified
 *   <li><b>Detailed save methods:</b> Accept full DTO objects to preserve all extracted fields
 *       (descriptions, definitions, etc.) from chunk-level extraction, especially CSV files
 * </ul>
 *
 * <h3>CSV Extraction Support</h3>
 *
 * CSV extraction results are processed through chunk-level extraction pipeline and use the detailed
 * save methods. This allows:
 *
 * <ul>
 *   <li>Preserving complete field mappings, validation rules, and transformation logic from CSV
 *       rows
 *   <li>Storing business and technical definitions from CSV glossaries
 *   <li>Capturing full decision criteria and policy descriptions from CSV configuration files
 *   <li>Maintaining calculation methods and target values for metrics from CSV KPI files
 * </ul>
 *
 * <p>All save methods use JPA repositories and are transactional to ensure data integrity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresStorageService {
  /**
   * Record containing identifiers for a newly started document ingestion.
   *
   * @param sourceDocumentId the source document UUID
   * @param documentId the ingestion document UUID
   */
  public record StartedDocument(UUID sourceDocumentId, UUID documentId) {}

  private final SourceDocumentRepository sourceDocumentRepository;
  private final IngestionDocumentRepository ingestionDocumentRepository;
  private final SourceChunkRepository sourceChunkRepository;
  private final DomainRepository domainRepository;
  private final SubdomainRepository subdomainRepository;
  private final ComponentRepository componentRepository;
  private final ApiRepository apiRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final WorkflowRepository workflowRepository;
  private final WorkflowStepRepository workflowStepRepository;
  private final DataModelRepository dataModelRepository;
  private final DataFieldRepository dataFieldRepository;
  private final IntegrationRepository integrationRepository;
  private final ResourceRepository resourceRepository;
  private final RelationshipRepository relationshipRepository;
  private final CapabilityRepository capabilityRepository;
  private final RoleRepository roleRepository;
  private final TermRepository termRepository;
  private final PolicyRepository policyRepository;
  private final DecisionRepository decisionRepository;
  private final MetricRepository metricRepository;
  private final NoteRepository noteRepository;
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID saveDocument(SourceDocumentMetadata source, List<SemanticChunk> chunks) {
    StartedDocument document = startDocument(source);
    saveSourceChunks(source, document.sourceDocumentId(), document.documentId(), chunks);
    return document.sourceDocumentId();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public StartedDocument startDocument(SourceDocumentMetadata source) {
    Optional<SourceDocumentEntity> existingSourceDocument =
        sourceDocumentRepository.findByProjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.projectId(), source.bucketName(), source.objectName(), source.contentHash());

    UUID sourceDocumentId;
    if (existingSourceDocument.isPresent()) {
      SourceDocumentEntity document = existingSourceDocument.get();
      sourceDocumentId = document.getSourceDocumentId();
      sourceChunkRepository.deleteBySourceDocumentId(sourceDocumentId);
      ingestionDocumentRepository.deleteBySourceDocumentId(sourceDocumentId);
      document.setUpdatedAt(OffsetDateTime.now());
      entityManager.flush();
      entityManager.clear();
    } else {
      sourceDocumentId = saveSourceDocument(source);
    }

    UUID documentId = saveIngestionDocument(source, sourceDocumentId, 0, null, "PROCESSING");
    return new StartedDocument(sourceDocumentId, documentId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveSourceChunks(
      SourceDocumentMetadata source,
      UUID sourceDocumentId,
      UUID documentId,
      List<SemanticChunk> chunks) {
    ingestionDocumentRepository
        .findById(documentId)
        .ifPresent(
            document -> {
              document.setChunkCount(chunks.size());
              ingestionDocumentRepository.save(document);
            });
    for (int i = 0; i < chunks.size(); i++) {
      saveSourceChunk(source, chunks.get(i), sourceDocumentId, documentId, i);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void failDocument(UUID documentId, Exception exception) {
    ingestionDocumentRepository
        .findById(documentId)
        .ifPresent(
            document -> {
              document.setExtractionStatus("FAILED");
              document.setErrorMessage(shortMessage(exception));
              ingestionDocumentRepository.save(document);
            });
  }

  /**
   * Ensures a domain entity exists for the given project and domain name.
   *
   * <p>If a domain with the same projectId and domainName already exists, returns its ID. Otherwise
   * creates a new domain entity with the provided details.
   *
   * @param projectId the project UUID
   * @param domainName the domain name (required)
   * @param description optional domain description (can be null)
   * @param confidence optional confidence score (can be null)
   * @return the domain UUID (existing or newly created)
   */
  public UUID ensureDomain(
      UUID projectId, String domainName, String description, Double confidence) {
    return domainRepository
        .findByProjectIdAndDomainName(projectId, domainName)
        .map(DomainEntity::getDomainId)
        .orElseGet(
            () ->
                domainRepository
                    .save(
                        DomainEntity.builder()
                            .projectId(projectId)
                            .domainName(domainName)
                            .knowledge(domainName)
                            .description(description)
                            .confidence(confidence)
                            .build())
                    .getDomainId());
  }

  /**
   * Ensures a domain entity exists for the given project and domain name.
   *
   * <p>This is a convenience overload that creates a domain without description and confidence.
   *
   * @param projectId the project UUID
   * @param domainName the domain name
   * @return the domain UUID (existing or newly created)
   */
  public UUID ensureDomain(UUID projectId, String domainName) {
    return ensureDomain(projectId, domainName, null, null);
  }

  public UUID ensureSubdomain(
      UUID projectId, String domainName, UUID domainId, String subdomainName) {
    return subdomainRepository
        .findByDomainIdAndSubdomainName(domainId, subdomainName)
        .map(SubdomainEntity::getSubdomainId)
        .orElseGet(
            () ->
                subdomainRepository
                    .save(
                        SubdomainEntity.builder()
                            .projectId(projectId)
                            .domainId(domainId)
                            .subdomainName(subdomainName)
                            .knowledge(subdomainName)
                            .build())
                    .getSubdomainId());
  }

  public void saveDocumentKnowledge(
      UUID sourceDocumentId, UUID projectId, DocumentKnowledge docKnowledge) {
    ingestionDocumentRepository.findByProjectId(projectId).stream()
        .filter(doc -> sourceDocumentId.equals(doc.getSourceDocumentId()))
        .findFirst()
        .ifPresent(
            doc -> {
              doc.setSummary(docKnowledge.getSystemSummary());
              doc.setDocumentType(docKnowledge.getDetectedPlatform());
              doc.setExtractedMetadata(toJson(docKnowledge));
              doc.setExtractedAt(OffsetDateTime.now());
              doc.setExtractionStatus("EXTRACTED");
              ingestionDocumentRepository.save(doc);
            });
  }

  public UUID saveKnowledgeComponent(KnowledgeComponent component) {
    return componentRepository
        .save(
            ComponentEntity.builder()
                .projectId(component.getProjectId())
                .domainId(component.getDomainId())
                .subdomainId(component.getSubdomainId())
                .componentName(component.getComponentName())
                .componentType(component.getComponentType())
                .category(component.getCategory())
                .knowledge(component.getDescription())
                .responsibility(component.getResponsibility())
                .technology(component.getTechnology())
                .capability(component.getCapability())
                .confidence(component.getConfidence())
                .embedding(EmbeddingUtils.embeddingToString(component.getEmbedding()))
                .metadata(toJson(component.getMetadata()))
                .build())
        .getComponentId();
  }

  public UUID saveKnowledgeAPI(KnowledgeAPI api) {
    return apiRepository
        .save(
            ApiEntity.builder()
                .projectId(api.getProjectId())
                .componentId(api.getComponentId())
                .apiName(api.getApiName())
                .apiType(api.getApiType())
                .httpMethod(api.getHttpMethod())
                .endpointPath(api.getEndpointPath())
                .requestSchema(toJson(api.getRequestSchema()))
                .responseSchema(toJson(api.getResponseSchema()))
                .embedding(EmbeddingUtils.embeddingToString(api.getEmbedding()))
                .build())
        .getApiId();
  }

  public UUID saveKnowledgeBusinessRule(KnowledgeBusinessRule rule) {
    return businessRuleRepository
        .save(
            BusinessRuleEntity.builder()
                .projectId(rule.getProjectId())
                .componentId(rule.getComponentId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .conditionText(rule.getConditionText())
                .outcomeText(rule.getOutcomeText())
                .validationCriteria(rule.getValidationCriteria())
                .priority(rule.getPriority())
                .confidence(rule.getConfidence())
                .embedding(EmbeddingUtils.embeddingToString(rule.getEmbedding()))
                .build())
        .getRuleId();
  }

  public UUID saveKnowledgeWorkflow(KnowledgeWorkflow workflow) {
    return workflowRepository
        .save(
            WorkflowEntity.builder()
                .projectId(workflow.getProjectId())
                .workflowName(workflow.getWorkflowName())
                .triggerText(workflow.getTriggerText())
                .outcomeText(workflow.getOutcomeText())
                .actor(workflow.getOwner())
                .confidence(workflow.getConfidence())
                .embedding(EmbeddingUtils.embeddingToString(workflow.getEmbedding()))
                .build())
        .getWorkflowId();
  }

  /**
   * Persists a workflow step to the database.
   *
   * <p>Converts the DTO to an entity and saves it to the knowledge_workflow_steps table. The
   * projectId field must be set on the DTO before calling this method to satisfy the NOT NULL
   * constraint in the database schema.
   *
   * @param step the workflow step DTO containing all step details including projectId
   * @return the generated UUID of the saved workflow step
   * @throws org.springframework.dao.DataIntegrityViolationException if projectId is null
   */
  public UUID saveKnowledgeWorkflowStep(KnowledgeWorkflowStep step) {
    return workflowStepRepository
        .save(
            WorkflowStepEntity.builder()
                .projectId(step.getProjectId()) // Required: NOT NULL constraint in DB
                .workflowId(step.getWorkflowId())
                .sequenceNumber(step.getSequenceNumber())
                .actor(step.getActor())
                .actionText(step.getActionText())
                .inputParameters(toJson(step.getInputParameters()))
                .outputParameters(toJson(step.getOutputParameters()))
                .embedding(EmbeddingUtils.embeddingToString(step.getEmbedding()))
                .build())
        .getStepId();
  }

  public UUID saveKnowledgeDataModel(KnowledgeDataModel dataModel) {
    return dataModelRepository
        .save(
            DataModelEntity.builder()
                .projectId(dataModel.getProjectId())
                .modelName(dataModel.getModelName())
                .modelType(dataModel.getModelType())
                .schemaDefinition(toJson(dataModel.getSchemaDefinition()))
                .businessDefinition(dataModel.getDescription())
                .embedding(EmbeddingUtils.embeddingToString(dataModel.getEmbedding()))
                .build())
        .getDataModelId();
  }

  /**
   * Persists a data field to the database.
   *
   * <p>Converts the DTO to an entity and saves it to the knowledge_data_fields table. The projectId
   * field must be set on the DTO before calling this method to satisfy the NOT NULL constraint in
   * the database schema.
   *
   * @param field the data field DTO containing all field details including projectId
   * @return the generated UUID of the saved data field
   * @throws org.springframework.dao.DataIntegrityViolationException if projectId is null
   */
  public UUID saveKnowledgeDataField(KnowledgeDataField field) {
    return dataFieldRepository
        .save(
            DataFieldEntity.builder()
                .projectId(field.getProjectId()) // Required: NOT NULL constraint in DB
                .dataModelId(field.getDataModelId())
                .fieldName(field.getFieldName())
                .fieldType(field.getFieldType())
                .businessDefinition(field.getDescription())
                .businessRules(toJson(field.getConstraints()))
                .build())
        .getFieldId();
  }

  public UUID saveKnowledgeIntegration(KnowledgeIntegration integration) {
    return integrationRepository
        .save(
            IntegrationEntity.builder()
                .projectId(integration.getProjectId())
                .sourceSystem(integration.getSourceSystem())
                .targetSystem(integration.getTargetSystem())
                .protocol(integration.getProtocol())
                .description(integration.getDescription())
                .embedding(EmbeddingUtils.embeddingToString(integration.getEmbedding()))
                .build())
        .getIntegrationId();
  }

  public UUID saveKnowledgeResource(KnowledgeResource resource) {
    return resourceRepository
        .save(
            ResourceEntity.builder()
                .projectId(resource.getProjectId())
                .resourceName(resource.getResourceName())
                .resourceType(resource.getResourceType())
                .provider(resource.getProvider())
                .environment(resource.getEnvironment())
                .configs(toJson(resource.getConfigs()))
                .embedding(EmbeddingUtils.embeddingToString(resource.getEmbedding()))
                .build())
        .getResourceId();
  }

  /**
   * Persists a business capability extracted from document-level analysis.
   *
   * <p>Capabilities are saved with high confidence during document-level analysis and used for
   * entity linking during chunk-level extraction to ensure consistent naming across the knowledge
   * graph.
   *
   * @param projectId the project this capability belongs to
   * @param domainId the domain this capability is associated with (nullable)
   * @param subdomainId the subdomain this capability is associated with (nullable)
   * @param capabilityName the name of the capability as extracted from the document
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved capability
   */
  public UUID saveKnowledgeCapability(
      UUID projectId, UUID domainId, UUID subdomainId, String capabilityName, Double confidence) {
    return capabilityRepository
        .save(
            CapabilityEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .capabilityName(capabilityName)
                .confidence(confidence)
                .build())
        .getCapabilityId();
  }

  /**
   * Persists a detailed business capability from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link BusinessCapability} DTO from chunk-level
   * extraction results, preserving capability type, description, business value, and other
   * metadata. Use this when saving capabilities from CSV files or detailed document chunks that
   * contain comprehensive capability information.
   *
   * <p><strong>Capability vs Component:</strong>
   *
   * <ul>
   *   <li><b>Business Capability (this method):</b> WHAT the business does - technology-independent
   *       business functions (e.g., "Order Management", "Customer Onboarding")
   *   <li><b>Solution Component:</b> HOW it's implemented - technical components and services
   *       (e.g., "OrderService", "CustomerAPI")
   * </ul>
   *
   * @param projectId the project this capability belongs to
   * @param domainId the domain this capability is associated with (nullable)
   * @param subdomainId the subdomain this capability is associated with (nullable)
   * @param capability the complete capability DTO from chunk extraction
   * @return the unique identifier of the saved capability
   */
  public UUID saveKnowledgeCapability(
      UUID projectId, UUID domainId, UUID subdomainId, BusinessCapability capability) {
    return capabilityRepository
        .save(
            CapabilityEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .capabilityName(capability.getCapabilityName())
                .capabilityType(capability.getCapabilityType())
                .description(capability.getDescription())
                .businessValue(capability.getBusinessValue())
                .confidence(capability.getConfidence())
                .build())
        .getCapabilityId();
  }

  /**
   * Persists a business role or actor extracted from document-level analysis.
   *
   * <p>Roles are saved with high confidence during document-level analysis and used for entity
   * linking during chunk-level extraction to identify actors in workflows and processes.
   *
   * @param projectId the project this role belongs to
   * @param domainId the domain this role is associated with (nullable)
   * @param subdomainId the subdomain this role is associated with (nullable)
   * @param roleName the name of the role as extracted from the document
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved role
   */
  public UUID saveKnowledgeRole(
      UUID projectId, UUID domainId, UUID subdomainId, String roleName, Double confidence) {
    return roleRepository
        .save(
            RoleEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .roleName(roleName)
                .confidence(confidence)
                .build())
        .getRoleId();
  }

  /**
   * Persists a detailed business role from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link BusinessRole} DTO from chunk-level extraction
   * results, preserving all extracted fields including description, responsibilities, roleType, and
   * other metadata. Use this when saving roles from CSV files or detailed document chunks that
   * contain comprehensive role information.
   *
   * @param projectId the project this role belongs to
   * @param domainId the domain this role is associated with (nullable)
   * @param subdomainId the subdomain this role is associated with (nullable)
   * @param role the complete role DTO from chunk extraction
   * @return the unique identifier of the saved role
   */
  public UUID saveKnowledgeRole(
      UUID projectId, UUID domainId, UUID subdomainId, BusinessRole role) {
    return roleRepository
        .save(
            RoleEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .roleName(role.getRoleName())
                .roleType(role.getRoleType())
                .description(role.getDescription())
                .responsibilities(role.getResponsibilities())
                .confidence(role.getConfidence())
                .build())
        .getRoleId();
  }

  /**
   * Persists a business term or domain vocabulary extracted from document-level analysis.
   *
   * <p>Terms capture the ubiquitous language of the domain and are used for entity linking during
   * chunk-level extraction to maintain consistent terminology across the knowledge graph.
   *
   * @param projectId the project this term belongs to
   * @param domainId the domain this term is associated with (nullable)
   * @param subdomainId the subdomain this term is associated with (nullable)
   * @param termName the name of the term as extracted from the document
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved term
   */
  public UUID saveKnowledgeTerm(
      UUID projectId, UUID domainId, UUID subdomainId, String termName, Double confidence) {
    return termRepository
        .save(
            TermEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .termName(termName)
                .confidence(confidence)
                .build())
        .getTermId();
  }

  /**
   * Persists a detailed business term from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link BusinessTerm} DTO from chunk-level extraction
   * results, preserving business and technical definitions, category, and other metadata. Use this
   * when saving terms from CSV glossary files or detailed document chunks.
   *
   * @param projectId the project this term belongs to
   * @param domainId the domain this term is associated with (nullable)
   * @param subdomainId the subdomain this term is associated with (nullable)
   * @param term the complete term DTO from chunk extraction
   * @return the unique identifier of the saved term
   */
  public UUID saveKnowledgeTerm(
      UUID projectId, UUID domainId, UUID subdomainId, BusinessTerm term) {
    return termRepository
        .save(
            TermEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .termName(term.getTermName())
                .category(term.getCategory())
                .businessDefinition(term.getBusinessDefinition())
                .technicalDefinition(term.getTechnicalDefinition())
                .confidence(term.getConfidence())
                .build())
        .getTermId();
  }

  /**
   * Persists a business policy or compliance requirement extracted from document-level analysis.
   *
   * <p>Policies define rules, constraints, and compliance requirements that govern how the
   * organization operates. These are extracted to understand regulatory and compliance context.
   *
   * @param projectId the project this policy belongs to
   * @param domainId the domain this policy is associated with (nullable)
   * @param subdomainId the subdomain this policy is associated with (nullable)
   * @param policyName the name of the policy as extracted from the document
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved policy
   */
  public UUID saveKnowledgePolicy(
      UUID projectId, UUID domainId, UUID subdomainId, String policyName, Double confidence) {
    return policyRepository
        .save(
            PolicyEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .policyName(policyName)
                .confidence(confidence)
                .build())
        .getPolicyId();
  }

  /**
   * Persists a detailed business policy from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link BusinessPolicy} DTO from chunk-level extraction
   * results, preserving description, business rationale, regulatory requirements, and other
   * metadata. Use this when saving policies from CSV compliance files or detailed document chunks.
   *
   * @param projectId the project this policy belongs to
   * @param domainId the domain this policy is associated with (nullable)
   * @param subdomainId the subdomain this policy is associated with (nullable)
   * @param policy the complete policy DTO from chunk extraction
   * @return the unique identifier of the saved policy
   */
  public UUID saveKnowledgePolicy(
      UUID projectId, UUID domainId, UUID subdomainId, BusinessPolicy policy) {
    return policyRepository
        .save(
            PolicyEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .policyName(policy.getPolicyName())
                .policyType(policy.getPolicyType())
                .description(policy.getDescription())
                .businessRationale(policy.getBusinessRationale())
                .regulatoryRequirement(policy.getRegulatoryRequirement())
                .confidence(policy.getConfidence())
                .build())
        .getPolicyId();
  }

  /**
   * Persists a business decision point extracted from document-level analysis.
   *
   * <p>Decisions capture critical choice points within workflows and processes where actions are
   * determined based on specific criteria. Understanding these decisions is crucial for process
   * automation and business logic implementation.
   *
   * @param projectId the project this decision belongs to
   * @param domainId the domain this decision is associated with (nullable)
   * @param subdomainId the subdomain this decision is associated with (nullable)
   * @param decisionName the name of the decision as extracted from the document
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved decision
   */
  public UUID saveKnowledgeDecision(
      UUID projectId, UUID domainId, UUID subdomainId, String decisionName, Double confidence) {
    return decisionRepository
        .save(
            DecisionEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .decisionName(decisionName)
                .confidence(confidence)
                .build())
        .getDecisionId();
  }

  /**
   * Persists a detailed business decision from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link BusinessDecision} DTO from chunk-level
   * extraction results, preserving decision question, criteria, context, and other metadata. Use
   * this when saving decisions from CSV workflow files or detailed document chunks.
   *
   * @param projectId the project this decision belongs to
   * @param domainId the domain this decision is associated with (nullable)
   * @param subdomainId the subdomain this decision is associated with (nullable)
   * @param decision the complete decision DTO from chunk extraction
   * @return the unique identifier of the saved decision
   */
  public UUID saveKnowledgeDecision(
      UUID projectId, UUID domainId, UUID subdomainId, BusinessDecision decision) {
    return decisionRepository
        .save(
            DecisionEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .decisionName(decision.getDecisionName())
                .decisionQuestion(decision.getDecisionQuestion())
                .decisionCriteria(decision.getDecisionCriteria())
                .decisionContext(decision.getDecisionContext())
                .confidence(decision.getConfidence())
                .build())
        .getDecisionId();
  }

  /**
   * Saves a business metric to the knowledge graph.
   *
   * <p>Stores a KPI or performance indicator extracted from documentation. Metrics can be linked to
   * capabilities, workflows, or components for performance tracking.
   *
   * @param projectId the project this metric belongs to
   * @param domainId the domain this metric is associated with (nullable)
   * @param subdomainId the subdomain this metric is associated with (nullable)
   * @param metricName the name of the metric
   * @param metricType the type of metric (performance, quality, financial, operational, customer)
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved metric
   */
  public UUID saveKnowledgeMetric(
      UUID projectId,
      UUID domainId,
      UUID subdomainId,
      String metricName,
      String metricType,
      Double confidence) {
    return metricRepository
        .save(
            MetricEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .metricName(metricName)
                .metricType(metricType)
                .confidence(confidence)
                .build())
        .getMetricId();
  }

  /**
   * Persists a detailed business metric from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link BusinessMetric} DTO from chunk-level extraction
   * results, preserving description, calculation method, target value, and other metadata. Use this
   * when saving metrics from CSV KPI files or detailed document chunks.
   *
   * @param projectId the project this metric belongs to
   * @param domainId the domain this metric is associated with (nullable)
   * @param subdomainId the subdomain this metric is associated with (nullable)
   * @param metric the complete metric DTO from chunk extraction
   * @return the unique identifier of the saved metric
   */
  public UUID saveKnowledgeMetric(
      UUID projectId, UUID domainId, UUID subdomainId, BusinessMetric metric) {
    return metricRepository
        .save(
            MetricEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .metricName(metric.getMetricName())
                .metricType(metric.getMetricType())
                .description(metric.getDescription())
                .calculationMethod(metric.getCalculationMethod())
                .targetValue(metric.getTargetValue())
                .confidence(metric.getConfidence())
                .build())
        .getMetricId();
  }

  /**
   * Saves a knowledge note to the knowledge graph.
   *
   * <p>Stores miscellaneous insights such as architecture decisions, constraints, assumptions,
   * risks, or recommendations that don't fit into other structured categories.
   *
   * @param projectId the project this note belongs to
   * @param domainId the domain this note is associated with (nullable)
   * @param subdomainId the subdomain this note is associated with (nullable)
   * @param noteType the type of note (architecture, design_decision, constraint, assumption, risk,
   *     recommendation)
   * @param noteText the actual note content
   * @param confidence the AI extraction confidence score (0.0 to 1.0)
   * @return the unique identifier of the saved note
   */
  public UUID saveKnowledgeNote(
      UUID projectId,
      UUID domainId,
      UUID subdomainId,
      String noteType,
      String noteText,
      Double confidence) {
    return noteRepository
        .save(
            NoteEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .noteType(noteType)
                .noteText(noteText)
                .confidence(confidence)
                .build())
        .getNoteId();
  }

  /**
   * Persists a detailed knowledge note from chunk-level extraction (including CSV).
   *
   * <p>This overloaded method accepts a full {@link KnowledgeNote} DTO from chunk-level extraction
   * results. Note: Since KnowledgeNote DTO only contains noteType, noteText, and confidence, this
   * method is functionally equivalent to the simpler version but maintains API consistency.
   *
   * @param projectId the project this note belongs to
   * @param domainId the domain this note is associated with (nullable)
   * @param subdomainId the subdomain this note is associated with (nullable)
   * @param note the complete note DTO from chunk extraction
   * @return the unique identifier of the saved note
   */
  public UUID saveKnowledgeNote(
      UUID projectId, UUID domainId, UUID subdomainId, KnowledgeNote note) {
    return noteRepository
        .save(
            NoteEntity.builder()
                .projectId(projectId)
                .domainId(domainId)
                .subdomainId(subdomainId)
                .noteType(note.getNoteType())
                .noteText(note.getNoteText())
                .confidence(note.getConfidence())
                .build())
        .getNoteId();
  }

  public void saveKnowledgeRelationship(
      UUID sourceDocumentId, UUID projectId, KnowledgeRelationship relationship) {
    relationshipRepository.save(
        RelationshipEntity.builder()
            .projectId(projectId)
            .sourceEntityType(nullToUnknown(relationship.getSourceType()))
            .sourceName(relationship.getSourceName())
            .targetEntityType(nullToUnknown(relationship.getTargetType()))
            .targetName(relationship.getTargetName())
            .relationshipType(nullToUnknown(relationship.getRelationshipType()))
            .relationshipDefinition(relationship.getContext())
            .confidence(relationship.getConfidence())
            .build());
  }

  private UUID saveSourceDocument(SourceDocumentMetadata source) {
    return sourceDocumentRepository
        .save(
            SourceDocumentEntity.builder()
                .projectId(source.projectId())
                .sourceBucket(source.bucketName())
                .sourceObject(source.objectName())
                .sourceGeneration(source.generation())
                .sourceChecksum(source.checksum())
                .contentHash(source.contentHash())
                .documentType(source.documentType())
                .fileType(source.fileType())
                .title(source.title())
                .documentName(source.title())
                .isCurrent(true)
                .build())
        .getSourceDocumentId();
  }

  private UUID saveIngestionDocument(
      SourceDocumentMetadata source,
      UUID sourceDocumentId,
      int chunkCount,
      DocumentKnowledge docKnowledge,
      String status) {
    return ingestionDocumentRepository
        .save(
            IngestionDocumentEntity.builder()
                .projectId(source.projectId())
                .sourceDocumentId(sourceDocumentId)
                .documentName(source.title())
                .documentType(source.fileType())
                .summary(docKnowledge == null ? null : docKnowledge.getSystemSummary())
                .extractedMetadata(docKnowledge == null ? null : toJson(docKnowledge))
                .extractedAt(docKnowledge == null ? null : OffsetDateTime.now())
                .chunkCount(chunkCount)
                .extractionStatus(status)
                .build())
        .getDocumentId();
  }

  private void saveSourceChunk(
      SourceDocumentMetadata source,
      SemanticChunk chunk,
      UUID sourceDocumentId,
      UUID documentId,
      int index) {
    sourceChunkRepository.save(
        SourceChunkEntity.builder()
            .projectId(source.projectId())
            .documentId(documentId)
            .sourceDocumentId(sourceDocumentId)
            .content(chunk.getContent())
            .chunkIndex(index)
            .charStart((long) chunk.getCharStart())
            .charEnd((long) chunk.getCharEnd())
            .contextSummary(
                chunk.getClassification() == null
                    ? null
                    : chunk.getClassification().getDomain()
                        + " / "
                        + chunk.getClassification().getSubdomain())
            .embedding(EmbeddingUtils.embeddingToString(chunk.getEmbedding()))
            .embeddingStatus("EMBEDDED")
            .metadata(
                chunk.getContentHash() == null
                    ? null
                    : "{\"contentHash\":\"" + chunk.getContentHash() + "\"}")
            .build());
  }

  /**
   * Serializes any Java object to a JSON string representation.
   *
   * <p>This method ensures all values, including primitive strings, are properly serialized to
   * valid JSON format. This is critical for PostgreSQL JSONB columns which require properly
   * formatted JSON strings.
   *
   * <p><strong>Important:</strong> String values are serialized as JSON strings (wrapped in
   * quotes), not as plain text. For example:
   *
   * <ul>
   *   <li>Input: {@code "hello"} → Output: {@code "\"hello\""}
   *   <li>Input: {@code Map.of("key", "value")} → Output: {@code "{\"key\":\"value\"}"}
   *   <li>Input: {@code null} → Output: {@code null}
   * </ul>
   *
   * <p><strong>Bug Fix History:</strong> Previously, this method returned plain strings without
   * JSON serialization, causing PostgreSQL errors like "invalid input syntax for type json". The
   * fix ensures all values go through Jackson's {@code ObjectMapper.writeValueAsString()}.
   *
   * @param value the object to serialize (can be null)
   * @return JSON string representation, or null if value is null or serialization fails
   */
  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    // Always serialize to valid JSON, even for primitive strings
    // This prevents PostgreSQL JSONB insertion errors
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize value to JSON", e);
      return null;
    }
  }

  /**
   * Converts null or blank strings to "unknown".
   *
   * <p>Used to ensure database columns that don't allow nulls have a safe default value.
   *
   * @param value the string to check
   * @return the original value if non-null and non-blank, otherwise "unknown"
   */
  private String nullToUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }

  /**
   * Extracts a short error message from an exception, truncated to 2000 characters.
   *
   * <p>If the exception has no message, uses the exception class simple name instead.
   *
   * @param exception the exception to extract message from
   * @return error message truncated to maximum 2000 characters for database storage
   */
  private String shortMessage(Exception exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }
    // Truncate to fit database column size limit
    return message.length() <= 2000 ? message : message.substring(0, 2000);
  }
}
