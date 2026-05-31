package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.DataFieldEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataFieldRepository extends JpaRepository<DataFieldEntity, UUID> {
  List<DataFieldEntity> findByProjectId(UUID projectId);
}
