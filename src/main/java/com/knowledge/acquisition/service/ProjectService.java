package com.knowledge.acquisition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.knowledge.acquisition.dto.*;
import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import com.knowledge.acquisition.entity.ProjectEntity;
import com.knowledge.acquisition.entity.ProjectStatus;
import com.knowledge.acquisition.exception.NotFoundException;
import com.knowledge.acquisition.ingestion.service.ai.EmbeddingService;
import com.knowledge.acquisition.ingestion.util.EmbeddingUtils;
import com.knowledge.acquisition.repository.ProcessTrackingRepository;
import com.knowledge.acquisition.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing knowledge ingestion projects and their lifecycle.
 *
 * <p>This service provides comprehensive project management capabilities including:
 *
 * <ul>
 *   <li><b>Project Creation:</b> Creates versioned projects with automatic name normalization and
 *       GCS bucket setup
 *   <li><b>Project Lifecycle:</b> Manages project status transitions (DRAFT → ACTIVE → SUSPENDED)
 *   <li><b>Multi-Version Support:</b> Enables multiple versions of the same project to coexist as
 *       ACTIVE
 *   <li><b>Definition Management:</b> Auto-generates definition.md templates in GCS for AI
 *       extraction context
 *   <li><b>Vector Embeddings:</b> Generates and stores embeddings for project definitions and
 *       summaries
 *   <li><b>Process Coordination:</b> Suspends conflicting processes when new versions are activated
 * </ul>
 *
 * <h3>Project Versioning</h3>
 *
 * Projects are uniquely identified by their normalized name and version number. Version numbers are
 * auto-incremented when not explicitly specified. Multiple successfully ingested versions can
 * coexist as ACTIVE, enabling A/B testing and gradual migration scenarios.
 *
 * <h3>GCS Integration</h3>
 *
 * Each project is associated with a GCS bucket and prefix where source documents are stored. A
 * definition.md file is automatically created to provide AI extraction context including domain
 * information, technology stack, business context, and extraction guidelines.
 *
 * <h3>Status Lifecycle</h3>
 *
 * <pre>
 * DRAFT → INGESTING → ACTIVE
 *                   ↓
 *               SUSPENDED (for conflicting versions)
 * </pre>
 *
 * @see ProjectEntity
 * @see ProjectStatus
 * @see CreateProjectRequest
 * @see ProjectResponse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ProcessTrackingRepository processRepository;
  private final ProjectPathService pathService;
  private final ObjectMapper objectMapper;
  private final Storage storage;
  private final EmbeddingService embeddingService;

  @Value("${gcp.storage.bucket-name}")
  private String defaultBucketName;

  @Transactional
  public ProjectResponse create(CreateProjectRequest request) {
    String normalizedName = pathService.normalize(request.projectName());
    String sourceBucket = resolveSourceBucket(request.sourceBucket());
    String gcsPrefix =
        request.gcsPrefix() != null
            ? request.gcsPrefix()
            : pathService.projectPrefix(normalizedName);

    int version;
    if (request.version() != null) {
      version = request.version();
      projectRepository
          .findByProjectNameAndVersion(normalizedName, version)
          .ifPresent(
              existing -> {
                throw new IllegalArgumentException("Project name and version already exist");
              });
    } else {
      Integer maxVersion = projectRepository.findMaxVersionByProjectName(normalizedName);
      version = (maxVersion == null ? 0 : maxVersion) + 1;
    }

    ProjectEntity project =
        ProjectEntity.builder()
            .projectName(normalizedName)
            .version(version)
            .title(request.title())
            .description(request.description())
            .definition(request.definition())
            .definitionEmbedding(definitionEmbedding(request.definition()))
            .sourceBucket(sourceBucket)
            .gcsPrefix(gcsPrefix)
            .status(ProjectStatus.DRAFT)
            .metadata(toJson(request.metadata()))
            .build();
    ProjectEntity savedProject = projectRepository.save(project);
    createProjectDefinition(
        sourceBucket,
        gcsPrefix,
        normalizedName,
        version,
        request.title(),
        request.description(),
        request.definition());
    return toResponse(savedProject);
  }

  private void suspendRunningProcesses(UUID projectId) {
    List<ProcessTrackingEntity> runningProcesses =
        processRepository.findByProjectIdAndStatus(projectId, "RUNNING");

    for (ProcessTrackingEntity process : runningProcesses) {
      process.setStatus("SUSPENDED");
      process.setFailureCause("Process suspended due to new project version launch");
      process.setCompletedAt(java.time.OffsetDateTime.now());
      processRepository.save(process);
    }
    if (!runningProcesses.isEmpty()) {
      log.info(
          "Suspended {} running process(es) for project: {}", runningProcesses.size(), projectId);
    }
  }

  /**
   * Called when a project's full pipeline completes successfully.
   *
   * <p>This method handles the multi-version lifecycle and should only be called after ALL pipeline
   * phases complete successfully (ingestion → consolidation → planning → project-summary).
   *
   * <ol>
   *   <li>Marks the project as ACTIVE (for both COMPLETED and PARTIAL_SUCCESS pipelines)
   *   <li>Allows multiple successfully processed versions to coexist as ACTIVE
   *   <li>Suspends only in-progress versions (DRAFT or INGESTING status) to prevent concurrent
   *       processing
   * </ol>
   *
   * @param projectId the UUID of the project that completed the full pipeline
   * @param processStatus the final status of the pipeline process (COMPLETED or PARTIAL_SUCCESS)
   */
  @Transactional
  public void onPipelineSuccess(UUID projectId, String processStatus) {
    ProjectEntity project = find(projectId);

    // Mark project as ACTIVE for both COMPLETED and PARTIAL_SUCCESS
    // Multiple versions can coexist as ACTIVE - this supports having v1, v2, v3 all active
    project.setStatus(ProjectStatus.ACTIVE);
    projectRepository.save(project);
    log.info(
        "Project {} (v{}) marked as ACTIVE after {} pipeline completion",
        project.getProjectName(),
        project.getVersion(),
        processStatus);

    // Suspend any in-progress/running previous versions (DRAFT or INGESTING)
    // This prevents multiple versions from processing simultaneously while allowing
    // multiple successfully processed versions to remain ACTIVE
    List<ProjectEntity> inProgressVersions =
        projectRepository.findAll().stream()
            .filter(
                p ->
                    p.getProjectName().equals(project.getProjectName())
                        && !p.getProjectId().equals(projectId)
                        && (p.getStatus() == ProjectStatus.DRAFT
                            || p.getStatus() == ProjectStatus.INGESTING))
            .toList();

    for (ProjectEntity inProgressProject : inProgressVersions) {
      inProgressProject.setStatus(ProjectStatus.SUSPENDED);
      projectRepository.save(inProgressProject);
      suspendRunningProcesses(inProgressProject.getProjectId());
    }

    if (!inProgressVersions.isEmpty()) {
      log.info(
          "Suspended {} in-progress version(s) of project: {}",
          inProgressVersions.size(),
          project.getProjectName());
    }
  }

  public ProjectResponse get(UUID projectId) {
    return toResponse(find(projectId));
  }

  public List<ProjectResponse> list() {
    return projectRepository.findAll().stream().map(this::toResponse).toList();
  }

  public ProjectStatusSummaryResponse getProjectStatus(String projectName) {
    String normalizedName = pathService.normalize(projectName);
    List<ProjectEntity> allVersions =
        projectRepository.findByProjectNameOrderByVersionDesc(normalizedName);

    if (allVersions.isEmpty()) {
      throw new NotFoundException("Project not found: " + projectName);
    }

    List<UUID> projectIds = allVersions.stream().map(ProjectEntity::getProjectId).toList();
    List<ProcessTrackingEntity> allProcesses =
        processRepository.findByProjectIdInOrderByCreatedAtDesc(projectIds);

    List<ProjectVersionStatusResponse> versionStatuses =
        allVersions.stream()
            .map(
                project -> {
                  List<ProcessResponse> runningProcesses =
                      allProcesses.stream()
                          .filter(p -> p.getProjectId().equals(project.getProjectId()))
                          .filter(p -> "RUNNING".equals(p.getStatus()))
                          .map(this::toProcessResponse)
                          .toList();

                  return new ProjectVersionStatusResponse(
                      project.getProjectId(),
                      project.getProjectName(),
                      project.getVersion(),
                      project.getTitle(),
                      project.getStatus(),
                      project.getSourceBucket(),
                      runningProcesses,
                      project.getCreatedAt(),
                      project.getUpdatedAt());
                })
            .toList();

    return new ProjectStatusSummaryResponse(normalizedName, allVersions.size(), versionStatuses);
  }

  private ProcessResponse toProcessResponse(ProcessTrackingEntity process) {
    return new ProcessResponse(
        process.getProcessId(),
        process.getProjectId(),
        process.getProcessType(),
        process.getStatus(),
        process.getTotalFiles(),
        process.getProcessedFiles(),
        process.getFailedFiles(),
        process.getCurrentFile(),
        process.getFailureCause(),
        process.getTotalTokensProcessed(),
        process.getTotalBytesProcessed(),
        process.getStartedAt(),
        process.getCompletedAt(),
        process.getCreatedAt(),
        process.getUpdatedAt());
  }

  /**
   * Finds a project by its unique identifier.
   *
   * @param projectId the project UUID
   * @return the project entity
   * @throws NotFoundException if the project does not exist
   */
  ProjectEntity find(UUID projectId) {
    return projectRepository
        .findById(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
  }

  /**
   * Finds a project by its name and version number.
   *
   * <p>The project name is normalized to kebab-case before lookup, allowing flexible name matching.
   * For example, "My Project Name" is normalized to "my-project-name".
   *
   * @param projectName the project name (will be normalized to kebab-case)
   * @param version the version number
   * @return the project entity
   * @throws NotFoundException if the project version does not exist
   */
  public ProjectEntity findByNameAndVersion(String projectName, Integer version) {
    String normalizedName = pathService.normalize(projectName);
    return projectRepository
        .findByProjectNameAndVersion(normalizedName, version)
        .orElseThrow(
            () ->
                new NotFoundException("Project not found: " + projectName + " version " + version));
  }

  ProjectResponse toResponse(ProjectEntity project) {
    return new ProjectResponse(
        project.getProjectId(),
        project.getProjectName(),
        project.getVersion(),
        project.getTitle(),
        project.getDescription(),
        project.getDefinition(),
        project.getSummary(),
        project.getSourceBucket(),
        project.getGcsPrefix(),
        project.getStatus(),
        project.getMetadata(),
        project.getCreatedAt(),
        project.getUpdatedAt(),
        project.getSummaryGeneratedAt());
  }

  private String resolveSourceBucket(String sourceBucket) {
    return sourceBucket == null || sourceBucket.isBlank() ? defaultBucketName : sourceBucket;
  }

  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize project metadata", e);
      return null;
    }
  }

  /**
   * Creates a definition.md file in GCS for the project to provide AI extraction context.
   *
   * <p>This file serves as a guide for AI-powered knowledge extraction by providing:
   *
   * <ul>
   *   <li>Domain and architecture context
   *   <li>Technology stack information
   *   <li>Business context and workflows
   *   <li>Known components and services
   *   <li>Extraction guidelines and naming conventions
   * </ul>
   *
   * <p>If a custom definition is provided, it is used. Otherwise, a default template is generated
   * with placeholders for manual enrichment.
   *
   * @param bucket the GCS bucket name where the definition file will be stored
   * @param prefix the GCS object prefix (folder path) for the project
   * @param projectName the project name (used in template generation)
   * @param version the project version (used in template generation)
   * @param title the project title (optional, included if provided)
   * @param description the project description (optional, included if provided)
   * @param definition custom definition content (optional, falls back to auto-generated template if
   *     null or blank)
   */
  private void createProjectDefinition(
      String bucket,
      String prefix,
      String projectName,
      int version,
      String title,
      String description,
      String definition) {
    // Use custom definition if provided, otherwise generate default template
    String definitionContent =
        (definition != null && !definition.isBlank())
            ? definition
            : generateDefaultDefinition(projectName, version, title, description);

    // Create blob with metadata indicating management and auto-generation status
    BlobInfo definitionFile =
        BlobInfo.newBuilder(bucket, prefix + "definition.md")
            .setContentType("text/markdown")
            .setMetadata(
                java.util.Map.of(
                    "managed-by", "knowledge_engine_svc",
                    "purpose", "project-definition",
                    "auto-generated", String.valueOf(definition == null || definition.isBlank())))
            .build();

    // Write definition.md to GCS
    storage.create(
        definitionFile, definitionContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    log.info("Created definition.md for project: {} (v{})", projectName, version);
  }

  /**
   * Generates a default definition.md template with placeholders for manual enrichment.
   *
   * <p>The generated template includes sections for:
   *
   * <ul>
   *   <li><b>Domain & Architecture:</b> Business domain, source system, target architecture,
   *       patterns
   *   <li><b>Technologies:</b> Languages, frameworks, databases, integration methods, cloud
   *       platform
   *   <li><b>Business Context:</b> Capabilities, roles, workflows, compliance requirements
   *   <li><b>Known Components & Services:</b> APIs, services, data entities
   *   <li><b>Extraction Guidelines:</b> Naming conventions, business glossary, special
   *       considerations
   * </ul>
   *
   * <p>This template serves as a structured guide for users to provide domain-specific context that
   * improves AI extraction accuracy and entity recognition.
   *
   * @param projectName the project name (used in header)
   * @param version the project version number
   * @param title optional project title (included if not blank)
   * @param description optional project description (included if not blank)
   * @return markdown-formatted definition template as a string
   */
  private String generateDefaultDefinition(
      String projectName, int version, String title, String description) {
    StringBuilder template = new StringBuilder();

    // Header with project name and version
    template.append("# Project Definition: ").append(projectName).append("\n\n");
    template.append("**Version:** ").append(version).append("\n\n");

    if (title != null && !title.isBlank()) {
      template.append("**Title:** ").append(title).append("\n\n");
    }

    if (description != null && !description.isBlank()) {
      template.append("## Description\n\n").append(description).append("\n\n");
    }

    template.append("## Project Context for AI Extraction\n\n");
    template.append("This file provides context to guide the AI knowledge extraction process. ");
    template.append(
        "Update this file with domain-specific information to improve extraction accuracy.\n\n");

    template.append("### Domain & Architecture\n\n");
    template.append(
        "- **Business Domain:** [e.g., Customer Data Management, Order Processing, Financial Services]\n");
    template.append(
        "- **Source System:** [e.g., TIBCO MDM, Legacy Mainframe, Custom Application]\n");
    template.append(
        "- **Target Architecture:** [e.g., Java Microservices, Spring Boot, Event-Driven]\n");
    template.append(
        "- **Key Architectural Patterns:** [e.g., Repository Pattern, CQRS, Event Sourcing]\n\n");

    template.append("### Technologies\n\n");
    template.append("- **Languages:** [e.g., Java 17, Python, JavaScript]\n");
    template.append("- **Frameworks:** [e.g., Spring Boot, Spring Cloud, React]\n");
    template.append("- **Databases:** [e.g., PostgreSQL, MongoDB, Redis]\n");
    template.append("- **Integration:** [e.g., REST APIs, GraphQL, Message Queues]\n");
    template.append("- **Cloud Platform:** [e.g., GCP, AWS, Azure, Hybrid]\n\n");

    template.append("### Business Context\n\n");
    template.append("- **Primary Business Capabilities:** [List key business functions]\n");
    template.append("- **Key Business Roles:** [e.g., Data Steward, Business Analyst, Approver]\n");
    template.append("- **Critical Workflows:** [e.g., Customer Onboarding, Order Fulfillment]\n");
    template.append("- **Compliance Requirements:** [e.g., GDPR, SOX, HIPAA, PCI-DSS]\n\n");

    template.append("### Known Components & Services\n\n");
    template.append("List known components that may be referenced in the documentation:\n\n");
    template.append("- **APIs:** [e.g., Customer API, Order API, Payment Gateway]\n");
    template.append(
        "- **Services:** [e.g., Validation Service, Notification Service, Workflow Engine]\n");
    template.append("- **Data Entities:** [e.g., Customer, Order, Product, Invoice]\n\n");

    template.append("### Extraction Guidelines\n\n");
    template.append("**Entity Naming Conventions:**\n");
    template.append("- Use consistent naming for roles, components, and workflows\n");
    template.append(
        "- Prefix system components with \"System:\" (e.g., \"System: Payment Service\")\n");
    template.append(
        "- Prefix automated processes with \"Automated:\" (e.g., \"Automated: Nightly Batch\")\n\n");

    template.append("**Business Terms & Glossary:**\n");
    template.append("[Define domain-specific terms and their meanings]\n\n");

    template.append("**Special Considerations:**\n");
    template.append(
        "[Any special extraction rules, data sensitivity notes, or migration-specific context]\n\n");

    template.append("---\n\n");
    template.append(
        "*This file is automatically created for each project. Edit it to provide domain context that improves AI extraction accuracy.*\n");

    return template.toString();
  }

  private String definitionEmbedding(String definition) {
    if (definition == null || definition.isBlank()) {
      return null;
    }
    try {
      return EmbeddingUtils.embeddingToString(embeddingService.embedding(definition));
    } catch (Exception e) {
      log.warn("Could not generate project definition embedding", e);
      return null;
    }
  }
}
