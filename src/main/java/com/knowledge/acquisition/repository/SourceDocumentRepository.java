package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.SourceDocumentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceDocumentRepository extends JpaRepository<SourceDocumentEntity, UUID> {
  List<SourceDocumentEntity> findByProjectId(UUID projectId);

  Optional<SourceDocumentEntity> findByProjectIdAndSourceBucketAndSourceObjectAndContentHash(
      UUID projectId, String sourceBucket, String sourceObject, String contentHash);

  int deleteByProjectId(UUID projectId);
}
