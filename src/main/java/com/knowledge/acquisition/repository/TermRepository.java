package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.TermEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link TermEntity}.
 *
 * <p>Provides CRUD operations and custom query methods for business terms and domain vocabulary
 * extracted from enterprise documentation.
 */
public interface TermRepository extends JpaRepository<TermEntity, UUID> {
  /**
   * Retrieves all terms associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of terms belonging to the project
   */
  List<TermEntity> findByProjectId(UUID projectId);
}
