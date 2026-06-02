package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.CapabilityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link CapabilityEntity}.
 *
 * <p>Provides CRUD operations and custom query methods for business capabilities extracted from
 * enterprise documentation.
 */
public interface CapabilityRepository extends JpaRepository<CapabilityEntity, UUID> {
  /**
   * Retrieves all capabilities associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of capabilities belonging to the project
   */
  List<CapabilityEntity> findByProjectId(UUID projectId);
}
