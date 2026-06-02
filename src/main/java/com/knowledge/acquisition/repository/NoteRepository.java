package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.NoteEntity;
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
public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {}
