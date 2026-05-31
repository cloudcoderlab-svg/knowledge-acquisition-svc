package com.knowledge.acquisition.ingestion.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProjectKnowledgeResetService {
  @PersistenceContext private EntityManager entityManager;

  @Transactional
  public void resetIngestionData(UUID projectId) {
    log.info("Resetting ingestion data for project: {}", projectId);
    execute("delete from knowledge.knowledge_chunks where project_id = :projectId", projectId);
    execute(
        "delete from knowledge.knowledge_relationships where project_id = :projectId", projectId);
    execute(
        "delete from knowledge.knowledge_business_rules where project_id = :projectId", projectId);
    execute("delete from knowledge.knowledge_apis where project_id = :projectId", projectId);
    execute(
        "delete from knowledge.knowledge_workflow_steps where workflow_id in "
            + "(select workflow_id from knowledge.knowledge_workflows where project_id = :projectId)",
        projectId);
    execute("delete from knowledge.knowledge_workflows where project_id = :projectId", projectId);
    execute(
        "delete from knowledge.knowledge_data_fields where data_model_id in "
            + "(select data_model_id from knowledge.knowledge_data_models where project_id = :projectId)",
        projectId);
    execute("delete from knowledge.knowledge_data_models where project_id = :projectId", projectId);
    execute(
        "delete from knowledge.knowledge_integrations where project_id = :projectId", projectId);
    execute("delete from knowledge.knowledge_resources where project_id = :projectId", projectId);
    execute("delete from knowledge.knowledge_components where project_id = :projectId", projectId);
    execute("delete from knowledge.knowledge_modules where project_id = :projectId", projectId);
    execute("delete from knowledge.knowledge_subdomains where project_id = :projectId", projectId);
    execute("delete from knowledge.knowledge_domains where project_id = :projectId", projectId);
    execute(
        "delete from knowledge.knowledge_source_chunks where project_id = :projectId", projectId);
    execute("delete from knowledge.ingestion_documents where project_id = :projectId", projectId);
    execute("delete from knowledge.documents where project_id = :projectId", projectId);
  }

  @Transactional
  public void resetPlanningData(UUID projectId) {
    log.info("Resetting planning data for project: {}", projectId);
    execute("delete from knowledge.knowledge_facts where project_id = :projectId", projectId);
  }

  private void execute(String sql, UUID projectId) {
    entityManager.createNativeQuery(sql).setParameter("projectId", projectId).executeUpdate();
  }
}
