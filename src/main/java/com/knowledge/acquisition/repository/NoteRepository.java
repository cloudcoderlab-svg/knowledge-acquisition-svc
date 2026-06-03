package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.NoteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for knowledge notes.
 *
 * <p>Provides data access operations for {@link NoteEntity}, supporting storage and retrieval of
 * miscellaneous knowledge insights, decisions, constraints, and recommendations extracted from
 * enterprise documentation.
 */
@Repository
public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {
  /**
   * Retrieves all notes associated with a specific project.
   *
   * @param projectId the unique identifier of the project
   * @return list of notes belonging to the project
   */
  List<NoteEntity> findByProjectId(UUID projectId);
}
