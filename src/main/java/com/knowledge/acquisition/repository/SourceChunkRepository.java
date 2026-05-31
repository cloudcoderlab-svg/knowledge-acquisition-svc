package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.SourceChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SourceChunkRepository extends JpaRepository<SourceChunkEntity, UUID> {
  List<SourceChunkEntity> findTop20ByProjectIdAndContentContainingIgnoreCase(
      UUID projectId, String query);

  int deleteBySourceDocumentId(UUID sourceDocumentId);

  @Query(
      value =
          "select * from knowledge.knowledge_source_chunks "
              + "where project_id = :projectId and embedding is not null "
              + "order by embedding <=> cast(:embedding as vector) limit :limit",
      nativeQuery = true)
  List<SourceChunkEntity> searchByEmbedding(
      @Param("projectId") UUID projectId,
      @Param("embedding") String embedding,
      @Param("limit") int limit);
}
