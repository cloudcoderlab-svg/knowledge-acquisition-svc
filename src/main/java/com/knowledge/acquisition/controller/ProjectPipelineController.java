package com.knowledge.acquisition.controller;

import com.knowledge.acquisition.dto.ProcessResponse;
import com.knowledge.acquisition.dto.ProjectPipelineRequest;
import com.knowledge.acquisition.service.ProjectPipelineService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for orchestrating complete knowledge ingestion pipelines.
 *
 * <p>A pipeline executes the full sequence of processing steps for a project:
 *
 * <ol>
 *   <li><b>Ingestion</b> - Extract knowledge from source documents
 *   <li><b>Consolidation</b> - Analyze cross-document relationships and generate knowledge chunks
 *   <li><b>Planning</b> - Generate planning artifacts from consolidated knowledge
 *   <li><b>Project summary</b> - Generate a discovery profile and vector embedding
 * </ol>
 *
 * <p>Using the pipeline endpoint is the recommended way to process projects, as it ensures all
 * steps are executed in the correct order.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectPipelineController {
  private final ProjectPipelineService projectPipelineService;

  /**
   * Starts a complete knowledge ingestion pipeline for a single project.
   *
   * <p>This executes all processing phases in sequence. The pipeline runs asynchronously and
   * returns immediately with process tracking details.
   *
   * <p>Upon successful completion, the project status is automatically updated to ACTIVE.
   *
   * @param projectId the UUID of the project to process
   * @return process tracking details for monitoring pipeline progress
   * @throws NotFoundException if the project does not exist
   */
  @PostMapping("/{projectId}/processes/pipeline")
  public ProcessResponse startProjectPipeline(@PathVariable UUID projectId) {
    return projectPipelineService.startProjectPipeline(projectId);
  }

  /**
   * Starts knowledge ingestion pipelines for multiple projects concurrently.
   *
   * <p>This is useful for batch processing multiple projects. Each pipeline runs independently and
   * asynchronously.
   *
   * @param request request containing list of project IDs to process
   * @return list of process tracking details, one per project
   */
  @PostMapping("/processes/pipeline")
  public List<ProcessResponse> startProjectPipelines(
      @Valid @RequestBody ProjectPipelineRequest request) {
    return projectPipelineService.startProjectPipelines(request.projectIds());
  }
}
