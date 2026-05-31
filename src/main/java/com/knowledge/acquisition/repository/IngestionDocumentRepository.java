package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.IngestionDocumentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionDocumentRepository extends JpaRepository<IngestionDocumentEntity, UUID> {
  List<IngestionDocumentEntity> findByProjectId(UUID projectId);

  int deleteBySourceDocumentId(UUID sourceDocumentId);
}
