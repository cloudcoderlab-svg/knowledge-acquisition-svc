package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.RoleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link RoleEntity}.
 *
 * <p>Provides CRUD operations and custom query methods for business roles and actors extracted from
 * enterprise documentation.
 */
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
  /**
   * Retrieves all roles associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of roles belonging to the project
   */
  List<RoleEntity> findByProjectId(UUID projectId);
}
