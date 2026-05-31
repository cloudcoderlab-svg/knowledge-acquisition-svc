package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessTrackingRepository extends JpaRepository<ProcessTrackingEntity, UUID> {
  List<ProcessTrackingEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  List<ProcessTrackingEntity> findByProjectIdAndStatus(UUID projectId, String status);

  List<ProcessTrackingEntity> findByProjectIdInOrderByCreatedAtDesc(List<UUID> projectIds);
}
