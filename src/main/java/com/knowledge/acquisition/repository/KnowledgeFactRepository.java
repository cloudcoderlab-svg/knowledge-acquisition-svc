package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.KnowledgeFactEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KnowledgeFactRepository extends JpaRepository<KnowledgeFactEntity, UUID> {
  List<KnowledgeFactEntity> findByProjectId(UUID projectId);

  List<KnowledgeFactEntity> findTop20ByProjectIdAndFactTypeIgnoreCase(
      UUID projectId, String factType);

  @Query(
      value =
          "select * from knowledge.knowledge_facts "
              + "where project_id = :projectId and ("
              + "lower(coalesce(title, '')) like lower(concat('%', :query, '%')) or "
              + "lower(coalesce(summary, '')) like lower(concat('%', :query, '%')) or "
              + "lower(coalesce(content, '')) like lower(concat('%', :query, '%')) or "
              + "lower(coalesce(attributes::text, '')) like lower(concat('%', :query, '%')) or "
              + "lower(coalesce(fact_type, '')) like lower(concat('%', :query, '%'))"
              + ") order by created_at limit :limit",
      nativeQuery = true)
  List<KnowledgeFactEntity> searchFacts(
      @Param("projectId") UUID projectId, @Param("query") String query, @Param("limit") int limit);

  int deleteByProjectId(UUID projectId);
}
