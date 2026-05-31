package com.knowledge.acquisition.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a single step within a workflow.
 *
 * <p>A workflow step represents an atomic action or decision point in a business process. Each step
 * can have inputs, outputs, business rules, and is associated with an actor who performs the
 * action. Steps are ordered by sequence number within their parent workflow.
 *
 * @see KnowledgeWorkflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeWorkflowStep {
  /** Unique identifier for this workflow step */
  private UUID stepId;

  /**
   * Project identifier that owns this workflow step. Required for database persistence to satisfy
   * NOT NULL constraint. Inherited from parent workflow during save operation.
   */
  private UUID projectId;

  /** Reference to the parent workflow containing this step */
  private UUID workflowId;

  /** Order of this step within the workflow (1-based) */
  private Integer sequenceNumber;

  /** Human-readable name of the step */
  private String stepName;

  /** Person, role, or system responsible for executing this step */
  private String actor;

  /** Description of the action performed in this step */
  private String actionText;

  /** Data or parameters consumed by this step */
  private String inputData;

  /** Data or results produced by this step */
  private String outputData;

  /** Identifier or name of the subsequent step in the workflow */
  private String nextStep;

  /** Vector embedding for semantic search and similarity matching */
  private List<Double> embedding;

  /** Technical implementation details or code snippets */
  private String technicalDetails;

  /** JSON string containing structured input parameters */
  private String inputParameters;

  /** JSON string containing structured output parameters */
  private String outputParameters;

  /** Timestamp when this step was created */
  private OffsetDateTime createdAt;
}
