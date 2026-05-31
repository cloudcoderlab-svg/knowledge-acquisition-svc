package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.KnowledgeChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/**
 * Repository for managing knowledge chunks.
 *
 * <p>Knowledge chunks are semantic text fragments extracted from various knowledge entities
 * (domains, components, business rules, workflows, relationships) to enable efficient semantic
 * search and retrieval using vector embeddings.
 *
 * @author Knowledge Engine Team
 * @since 1.0.0
 */
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, UUID> {

  /**
   * Finds up to 20 knowledge chunks by project ID and content search query.
   *
   * <p>Performs a case-insensitive substring search on chunk content.
   *
   * @param projectId the project identifier
   * @param query the search query string to match against content
   * @return list of matching chunks, limited to 20 results
   */
  List<KnowledgeChunkEntity> findTop20ByProjectIdAndContentContainingIgnoreCase(
      UUID projectId, String query);

  /**
   * Finds all knowledge chunks for a specific project and entity type.
   *
   * @param projectId the project identifier
   * @param entityType the entity type (e.g., "knowledge_components", "knowledge_workflows")
   * @return list of matching chunks
   */
  List<KnowledgeChunkEntity> findByProjectIdAndEntityType(UUID projectId, String entityType);

  /**
   * Finds all knowledge chunks for a specific project.
   *
   * @param projectId the project identifier
   * @return list of all chunks belonging to the project
   */
  List<KnowledgeChunkEntity> findByProjectId(UUID projectId);

  /**
   * Deletes all knowledge chunks for a specific project using a bulk delete operation.
   *
   * <p><strong>Implementation Note:</strong> Uses JPQL bulk delete instead of derived delete method
   * to avoid "unexpected row count" errors that occur when JPA loads entities first then deletes
   * them individually. Bulk delete executes as a single SQL DELETE statement.
   *
   * <p><strong>Transactional:</strong> This method should be called within a transaction context
   * and followed by {@code entityManager.flush()} if necessary.
   *
   * @param projectId the project identifier
   * @return number of chunks deleted
   */
  @Modifying
  @Query("delete from KnowledgeChunkEntity k where k.projectId = :projectId")
  int deleteByProjectId(@Param("projectId") UUID projectId);

  /**
   * Performs semantic similarity search using vector embeddings.
   *
   * <p>Uses PostgreSQL pgvector extension's cosine distance operator ({@code <=>}) to find chunks
   * with embeddings most similar to the query embedding. Results are ordered by similarity (most
   * similar first).
   *
   * <p><strong>Requirements:</strong>
   *
   * <ul>
   *   <li>PostgreSQL with pgvector extension installed
   *   <li>Embedding column must be of type vector(768) or compatible dimension
   *   <li>Query embedding must be a valid vector string representation
   * </ul>
   *
   * @param projectId the project identifier to scope the search
   * @param embedding the query vector embedding as string representation (e.g.,
   *     "[0.1,0.2,0.3,...]")
   * @param limit maximum number of results to return
   * @return list of chunks ordered by similarity to query embedding (most similar first)
   */
  @Query(
      value =
          "select * from knowledge.knowledge_chunks "
              + "where project_id = :projectId and embedding is not null "
              + "order by embedding <=> cast(:embedding as vector) limit :limit",
      nativeQuery = true)
  List<KnowledgeChunkEntity> searchByEmbedding(
      @Param("projectId") UUID projectId,
      @Param("embedding") String embedding,
      @Param("limit") int limit);
}
