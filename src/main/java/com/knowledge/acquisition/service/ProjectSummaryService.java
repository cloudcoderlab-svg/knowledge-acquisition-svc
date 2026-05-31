package com.knowledge.acquisition.service;

import com.knowledge.acquisition.entity.IngestionDocumentEntity;
import com.knowledge.acquisition.entity.KnowledgeChunkEntity;
import com.knowledge.acquisition.entity.KnowledgeFactEntity;
import com.knowledge.acquisition.entity.ProjectEntity;
import com.knowledge.acquisition.ingestion.service.ai.EmbeddingService;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.EmbeddingUtils;
import com.knowledge.acquisition.repository.IngestionDocumentRepository;
import com.knowledge.acquisition.repository.KnowledgeChunkRepository;
import com.knowledge.acquisition.repository.KnowledgeFactRepository;
import com.knowledge.acquisition.repository.ProjectRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectSummaryService {
  private final ProjectRepository projectRepository;
  private final IngestionDocumentRepository ingestionDocumentRepository;
  private final KnowledgeChunkRepository knowledgeChunkRepository;
  private final KnowledgeFactRepository knowledgeFactRepository;
  private final VertexAIService vertexAIService;
  private final EmbeddingService embeddingService;

  @Value("${knowledge-engine.project-summary.max-input-chars:50000}")
  private int maxInputChars;

  @Value("${knowledge-engine.project-summary.max-documents:30}")
  private int maxDocuments;

  @Value("${knowledge-engine.project-summary.max-knowledge-chunks:80}")
  private int maxKnowledgeChunks;

  @Value("${knowledge-engine.project-summary.max-facts:80}")
  private int maxFacts;

  @Transactional
  public ProjectEntity refreshSummary(UUID projectId) {
    ProjectEntity project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

    String profileInput = buildProfileInput(project);
    String summary = generateSummary(project, profileInput);
    String embeddingText = discoveryEmbeddingText(project, summary);

    project.setSummary(summary);
    project.setSummaryEmbedding(safeEmbedding(embeddingText, projectId));
    project.setSummaryGeneratedAt(OffsetDateTime.now());
    return projectRepository.save(project);
  }

  private String buildProfileInput(ProjectEntity project) {
    StringBuilder builder = new StringBuilder();
    appendLine(builder, "Project name: " + project.getProjectName());
    appendLine(builder, "Version: " + project.getVersion());
    appendLine(builder, "Title: " + project.getTitle());
    appendLine(builder, "Description: " + project.getDescription());
    appendSection(builder, "Project definition", project.getDefinition());

    List<IngestionDocumentEntity> documents =
        ingestionDocumentRepository.findByProjectId(project.getProjectId()).stream()
            .limit(Math.max(0, maxDocuments))
            .toList();
    appendLine(builder, "Documents:");
    for (IngestionDocumentEntity document : documents) {
      appendLine(
          builder,
          "- "
              + text(document.getDocumentName())
              + " ["
              + text(document.getDocumentType())
              + "]: "
              + text(document.getSummary()));
    }

    List<KnowledgeChunkEntity> chunks =
        knowledgeChunkRepository.findByProjectId(project.getProjectId()).stream()
            .limit(Math.max(0, maxKnowledgeChunks))
            .toList();
    appendLine(builder, "Knowledge chunks:");
    for (KnowledgeChunkEntity chunk : chunks) {
      appendLine(
          builder,
          "- "
              + text(chunk.getChunkType())
              + " / "
              + text(chunk.getEntityType())
              + ": "
              + text(chunk.getContent()));
    }

    List<KnowledgeFactEntity> facts =
        knowledgeFactRepository.findByProjectId(project.getProjectId()).stream()
            .limit(Math.max(0, maxFacts))
            .toList();
    appendLine(builder, "Planning facts:");
    for (KnowledgeFactEntity fact : facts) {
      appendLine(
          builder,
          "- "
              + text(fact.getFactType())
              + ": "
              + text(fact.getTitle())
              + ". "
              + text(fact.getSummary(), fact.getContent()));
    }

    return truncate(builder.toString(), Math.max(1000, maxInputChars));
  }

  private String generateSummary(ProjectEntity project, String profileInput) {
    if (profileInput.isBlank()) {
      return fallbackSummary(project, profileInput);
    }
    String prompt =
        """
        Generate a project discovery profile for another service named knowledge-discovery-svc.
        The profile must be concise but semantically rich for vector search and downstream knowledge extraction.

        Include:
        - business purpose and domain
        - major capabilities, modules, workflows, and business rules
        - important integrations, data models, technologies, and source systems
        - distinctive terminology and use cases that should match future discovery tasks

        Return plain text only. Avoid tables.

        Project evidence:
        """
            + profileInput;
    try {
      String summary = vertexAIService.generate(prompt);
      if (summary != null && !summary.isBlank()) {
        return summary.trim();
      }
    } catch (Exception e) {
      log.warn("Could not generate AI project summary for {}", project.getProjectId(), e);
    }
    return fallbackSummary(project, profileInput);
  }

  private String fallbackSummary(ProjectEntity project, String profileInput) {
    String seed = text(project.getTitle(), project.getDescription(), project.getDefinition());
    if (seed.isBlank()) {
      seed = profileInput;
    }
    if (seed.isBlank()) {
      seed = "Project " + project.getProjectName() + " version " + project.getVersion();
    }
    return truncate(seed, 4000);
  }

  private String discoveryEmbeddingText(ProjectEntity project, String summary) {
    return text(
        project.getProjectName(),
        project.getTitle(),
        project.getDescription(),
        project.getDefinition(),
        summary);
  }

  private String safeEmbedding(String text, UUID projectId) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return EmbeddingUtils.embeddingToString(embeddingService.embedding(text));
    } catch (Exception e) {
      log.warn("Could not generate project summary embedding for {}", projectId, e);
      return null;
    }
  }

  private void appendSection(StringBuilder builder, String label, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    appendLine(builder, label + ":");
    appendLine(builder, value);
  }

  private void appendLine(StringBuilder builder, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    builder.append(value).append('\n');
  }

  private String text(String... values) {
    StringBuilder builder = new StringBuilder();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        if (!builder.isEmpty()) {
          builder.append(' ');
        }
        builder.append(value.strip());
      }
    }
    return builder.toString();
  }

  private String truncate(String value, int maxChars) {
    if (value == null || value.length() <= maxChars) {
      return value == null ? "" : value;
    }
    return value.substring(0, maxChars);
  }
}
