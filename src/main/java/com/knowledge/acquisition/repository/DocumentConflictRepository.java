package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.DocumentConflictEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for document conflicts detected during cross-document analysis.
 *
 * <p>Provides data access operations for {@link DocumentConflictEntity}, supporting storage and
 * retrieval of contradictions and inconsistencies found across multiple documents. Enables data
 * quality verification, conflict resolution tracking, and architectural governance.
 */
@Repository
public interface DocumentConflictRepository extends JpaRepository<DocumentConflictEntity, UUID> {
  /**
   * Finds all conflicts for a specific project.
   *
   * @param projectId the project identifier
   * @return list of conflicts for the project
   */
  List<DocumentConflictEntity> findByProjectId(UUID projectId);

  /**
   * Finds unresolved conflicts for a project.
   *
   * <p>Returns conflicts that still need attention and resolution.
   *
   * @param projectId the project identifier
   * @return list of unresolved conflicts
   */
  List<DocumentConflictEntity> findByProjectIdAndResolutionStatus(
      UUID projectId, String resolutionStatus);

  /**
   * Finds conflicts by severity level for prioritization.
   *
   * @param projectId the project identifier
   * @param severity severity level (critical, high, medium, low)
   * @return list of conflicts matching the severity level
   */
  List<DocumentConflictEntity> findByProjectIdAndSeverity(UUID projectId, String severity);
}
