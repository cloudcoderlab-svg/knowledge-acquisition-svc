package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.ResourceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID> {
  List<ResourceEntity> findByProjectId(UUID projectId);
}
