package com.knowledge.acquisition.controller;

import com.knowledge.acquisition.dto.*;
import com.knowledge.acquisition.service.GcsProjectFileService;
import com.knowledge.acquisition.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing knowledge ingestion projects.
 *
 * <p>A project represents a collection of documents to be ingested, processed, and made available
 * for knowledge retrieval. Projects support versioning, allowing multiple versions of the same
 * project to coexist.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Create projects with automatic version management
 *   <li>Upload source documents via signed GCS URLs
 *   <li>Track project status across all versions
 *   <li>Manage project metadata and definitions
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
  private final ProjectService projectService;
  private final GcsProjectFileService fileService;

  /**
   * Creates a new knowledge ingestion project.
   *
   * <p>The project name is automatically normalized to kebab-case. If no version is specified, the
   * version is auto-incremented from the highest existing version of the same project name.
   *
   * <p>The project is created in DRAFT status and must be ingested before it becomes ACTIVE.
   *
   * @param request project creation request containing name, title, description, and optional
   *     definition
   * @return the created project with assigned ID and version
   */
  @PostMapping
  public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
  }

  /**
   * Retrieves a project by its unique identifier.
   *
   * @param projectId the project UUID
   * @return project details
   * @throws NotFoundException if the project does not exist
   */
  @GetMapping("/{projectId}")
  public ProjectResponse get(@PathVariable UUID projectId) {
    return projectService.get(projectId);
  }

  /**
   * Lists all projects across all versions.
   *
   * @return list of all projects
   */
  @GetMapping
  public List<ProjectResponse> list() {
    return projectService.list();
  }

  @PostMapping("/discovery")
  public ProjectDiscoveryResponse discover(@Valid @RequestBody ProjectDiscoveryRequest request) {
    return projectService.discover(request);
  }

  /**
   * Generates a signed URL for uploading files to the project's GCS bucket.
   *
   * <p>The signed URL allows direct upload to Google Cloud Storage without requiring GCS
   * credentials. The URL is valid for a limited time (typically 15 minutes).
   *
   * @param projectId the project UUID
   * @param request upload request containing file name and content type
   * @return signed URL and metadata for uploading
   * @throws NotFoundException if the project does not exist
   */
  @PostMapping("/{projectId}/upload-url")
  public SignedUrlResponse uploadUrl(
      @PathVariable UUID projectId, @Valid @RequestBody SignedUrlRequest request) {
    return fileService.uploadUrl(projectId, request);
  }

  /**
   * Lists all files in the project's GCS bucket.
   *
   * @param projectId the project UUID
   * @return list of file paths relative to the project's GCS prefix
   * @throws NotFoundException if the project does not exist
   */
  @GetMapping("/{projectId}/files")
  public List<String> files(@PathVariable UUID projectId) {
    return fileService.listFiles(projectId);
  }

  /**
   * Gets the status summary for all versions of a project by name.
   *
   * <p>This endpoint provides an overview of all versions of a project, including their status,
   * running processes, and timestamps. The project name is normalized to kebab-case for lookup.
   *
   * @param projectName the project name (will be normalized)
   * @return status summary including all versions and their running processes
   * @throws NotFoundException if no versions of the project exist
   */
  @GetMapping("/status/{projectName}")
  public ProjectStatusSummaryResponse getProjectStatus(@PathVariable String projectName) {
    return projectService.getProjectStatus(projectName);
  }
}
