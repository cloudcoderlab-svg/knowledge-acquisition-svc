package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.ApiEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiRepository extends JpaRepository<ApiEntity, UUID> {
  List<ApiEntity> findByProjectId(UUID projectId);
}
