package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.MetricEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for business metrics and KPIs.
 *
 * <p>Provides data access operations for {@link MetricEntity}, supporting storage and retrieval of
 * performance indicators extracted from enterprise documentation.
 */
@Repository
public interface MetricRepository extends JpaRepository<MetricEntity, UUID> {
  /**
   * Retrieves all metrics associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of metrics belonging to the project
   */
  List<MetricEntity> findByProjectId(UUID projectId);
}
