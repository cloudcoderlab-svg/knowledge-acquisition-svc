package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.SharedEntityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for shared entities identified across multiple documents.
 *
 * <p>Provides data access operations for {@link SharedEntityEntity}, supporting storage and
 * retrieval of entities that appear in multiple documents across the knowledge base. Enables entity
 * deduplication, consistency verification, and traceability.
 */
@Repository
public interface SharedEntityRepository extends JpaRepository<SharedEntityEntity, UUID> {
  /**
   * Finds all shared entities for a specific project.
   *
   * @param projectId the project identifier
   * @return list of shared entities for the project
   */
  List<SharedEntityEntity> findByProjectId(UUID projectId);

  /**
   * Finds shared entities with consistency scores below a threshold.
   *
   * <p>Useful for identifying entities with conflicting definitions across documents that require
   * manual review.
   *
   * @param projectId the project identifier
   * @param maxScore maximum consistency score (exclusive)
   * @return list of shared entities with consistency issues
   */
  List<SharedEntityEntity> findByProjectIdAndConsistencyScoreLessThan(
      UUID projectId, Double maxScore);
}
