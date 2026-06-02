package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.ProjectEntity;
import com.knowledge.acquisition.entity.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
  Optional<ProjectEntity> findByProjectNameAndVersion(String projectName, Integer version);

  @Query(
      "SELECT p FROM ProjectEntity p WHERE p.gcsPrefix = :gcsPrefix "
          + "ORDER BY p.version DESC, p.createdAt DESC LIMIT 1")
  Optional<ProjectEntity> findByGcsPrefix(@Param("gcsPrefix") String gcsPrefix);

  List<ProjectEntity> findByProjectNameOrderByVersionDesc(String projectName);

  @Query(
      "SELECT COALESCE(MAX(p.version), 0) FROM ProjectEntity p WHERE p.projectName = :projectName")
  Integer findMaxVersionByProjectName(@Param("projectName") String projectName);

  List<ProjectEntity> findByProjectNameAndStatusNot(String projectName, ProjectStatus status);
}
