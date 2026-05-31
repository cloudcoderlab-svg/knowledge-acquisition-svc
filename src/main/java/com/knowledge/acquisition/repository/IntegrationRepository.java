package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.IntegrationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<IntegrationEntity, UUID> {
  List<IntegrationEntity> findByProjectId(UUID projectId);
}
