package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.DataModelEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataModelRepository extends JpaRepository<DataModelEntity, UUID> {
  List<DataModelEntity> findByProjectId(UUID projectId);
}
