package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.DocumentGapEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for document gaps detected during cross-document analysis.
 *
 * <p>Provides data access operations for {@link DocumentGapEntity}, supporting storage and
 * retrieval of missing or incomplete information identified across documents. Enables documentation
 * completeness assessment, missing implementation tracking, and quality improvement.
 */
@Repository
public interface DocumentGapRepository extends JpaRepository<DocumentGapEntity, UUID> {
  /**
   * Finds all gaps for a specific project.
   *
   * @param projectId the project identifier
   * @return list of gaps for the project
   */
  List<DocumentGapEntity> findByProjectId(UUID projectId);

  /**
   * Finds gaps by resolution status.
   *
   * <p>Useful for tracking open gaps that need attention versus those already addressed.
   *
   * @param projectId the project identifier
   * @param resolutionStatus resolution status (OPEN, IN_PROGRESS, CLOSED)
   * @return list of gaps matching the resolution status
   */
  List<DocumentGapEntity> findByProjectIdAndResolutionStatus(
      UUID projectId, String resolutionStatus);

  /**
   * Finds gaps by gap type for categorization and prioritization.
   *
   * @param projectId the project identifier
   * @param gapType gap type (referenced_not_defined, missing_implementation, etc.)
   * @return list of gaps matching the gap type
   */
  List<DocumentGapEntity> findByProjectIdAndGapType(UUID projectId, String gapType);
}
