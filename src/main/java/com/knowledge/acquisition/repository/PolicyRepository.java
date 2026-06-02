package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.PolicyEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link PolicyEntity}.
 *
 * <p>Provides CRUD operations and custom query methods for business policies and compliance
 * requirements extracted from enterprise documentation.
 */
public interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {
  /**
   * Retrieves all policies associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of policies belonging to the project
   */
  List<PolicyEntity> findByProjectId(UUID projectId);
}
