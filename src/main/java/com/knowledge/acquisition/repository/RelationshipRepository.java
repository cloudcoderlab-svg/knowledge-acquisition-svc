package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.RelationshipEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationshipRepository extends JpaRepository<RelationshipEntity, UUID> {
  List<RelationshipEntity> findByProjectId(UUID projectId);
}
