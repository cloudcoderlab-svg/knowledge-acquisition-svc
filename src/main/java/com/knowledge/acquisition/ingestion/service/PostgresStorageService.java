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

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresStorageService {
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

  public UUID ensureDomain(UUID projectId, String domainName) {
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
                            .build())
                    .getDomainId());
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

  public UUID saveKnowledgeWorkflowStep(KnowledgeWorkflowStep step) {
    return workflowStepRepository
        .save(
            WorkflowStepEntity.builder()
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

  public UUID saveKnowledgeDataField(KnowledgeDataField field) {
    return dataFieldRepository
        .save(
            DataFieldEntity.builder()
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
