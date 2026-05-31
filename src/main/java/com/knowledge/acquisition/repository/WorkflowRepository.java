package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.WorkflowEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, UUID> {
  List<WorkflowEntity> findByProjectId(UUID projectId);
}
