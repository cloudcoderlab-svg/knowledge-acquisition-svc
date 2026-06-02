package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.DecisionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link DecisionEntity}.
 *
 * <p>Provides CRUD operations and custom query methods for business decision points extracted from
 * enterprise documentation.
 */
public interface DecisionRepository extends JpaRepository<DecisionEntity, UUID> {
  /**
   * Retrieves all decisions associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of decisions belonging to the project
   */
  List<DecisionEntity> findByProjectId(UUID projectId);
}
