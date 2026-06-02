package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.MetricEntity;
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
public interface MetricRepository extends JpaRepository<MetricEntity, UUID> {}
