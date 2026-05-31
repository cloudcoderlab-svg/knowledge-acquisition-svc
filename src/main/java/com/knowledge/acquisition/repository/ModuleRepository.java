package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.ModuleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID> {
  List<ModuleEntity> findByProjectId(UUID projectId);
}
