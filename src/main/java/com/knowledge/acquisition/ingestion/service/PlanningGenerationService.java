package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.entity.*;
import com.knowledge.acquisition.repository.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningGenerationService {
  private final DomainRepository domainRepository;
  private final SubdomainRepository subdomainRepository;
  private final WorkflowRepository workflowRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final ComponentRepository componentRepository;
  private final RelationshipRepository relationshipRepository;
  private final KnowledgeFactRepository knowledgeFactRepository;
  private final ObjectMapper objectMapper;

  /**
   * Generates planning artifacts (EPICs, FEATUREs, USER_STORYs) from extracted knowledge.
   *
   * <p>Uses domain-based planning hierarchy:
   *
   * <ul>
   *   <li>Domain → EPIC (business initiative)
   *   <li>Subdomain → FEATURE (capability grouping)
   *   <li>Workflow → USER_STORY (user-facing functionality)
   *   <li>Business Rule → ACCEPTANCE_CRITERIA + TEST_SCENARIO
   * </ul>
   *
   * @param projectId the project to generate planning artifacts for
   * @return count of facts created (EPICs + FEATUREs + STORYs + CRITERIA)
   */
  @Transactional
  public int generate(UUID projectId) {
    log.info("Starting domain-based planning generation for project {}", projectId);

    List<DomainEntity> domains = domainRepository.findByProjectId(projectId);
    List<SubdomainEntity> subdomains = subdomainRepository.findByProjectId(projectId);
    List<WorkflowEntity> workflows = workflowRepository.findByProjectId(projectId);
    List<BusinessRuleEntity> rules = businessRuleRepository.findByProjectId(projectId);

    if (domains.isEmpty()) {
      log.warn("No domains found for project {} - cannot generate planning artifacts", projectId);
      return 0;
    }

    int created = 0;

    // Generate EPIC for each domain
    for (DomainEntity domain : domains) {
      KnowledgeFactEntity epic = createEpicForDomain(projectId, domain);
      created++;

      // Get subdomains for this domain
      List<SubdomainEntity> domainSubdomains =
          subdomains.stream().filter(s -> s.getDomainId().equals(domain.getDomainId())).toList();

      if (domainSubdomains.isEmpty()) {
        // No subdomains - create stories directly under epic
        created += createStoriesForDomain(projectId, epic, domain, workflows, rules);
      } else {
        // Create FEATURE for each subdomain
        for (SubdomainEntity subdomain : domainSubdomains) {
          KnowledgeFactEntity feature = createFeatureForSubdomain(projectId, epic, subdomain);
          created++;

          // Create USER_STORYs for workflows in this subdomain
          created +=
              createStoriesForSubdomain(projectId, epic, feature, subdomain, workflows, rules);
        }
      }
    }

    log.info("Generated {} planning facts for project {}", created, projectId);
    return created;
  }

  /**
   * Creates an EPIC for a business domain.
   *
   * @param projectId the project ID
   * @param domain the domain entity
   * @return the created EPIC fact
   */
  private KnowledgeFactEntity createEpicForDomain(UUID projectId, DomainEntity domain) {
    return knowledgeFactRepository.save(
        factBuilder(projectId, "EPIC", "Deliver " + domain.getDomainName())
            .factKey("domain:" + domain.getDomainId())
            .summary(domain.getDescription())
            .content(domain.getKnowledge())
            .priority("HIGH")
            .sourceEntityType("DOMAIN")
            .sourceEntityId(domain.getDomainId())
            .attributes(
                toJson(
                    Map.of(
                        "problemStatement", nullToEmpty(domain.getDescription()),
                        "businessValue", nullToEmpty(domain.getKnowledge()),
                        "scope", "All " + domain.getDomainName() + " capabilities")))
            .build());
  }

  /**
   * Creates a FEATURE for a business subdomain.
   *
   * @param projectId the project ID
   * @param epic the parent EPIC
   * @param subdomain the subdomain entity
   * @return the created FEATURE fact
   */
  private KnowledgeFactEntity createFeatureForSubdomain(
      UUID projectId, KnowledgeFactEntity epic, SubdomainEntity subdomain) {
    return knowledgeFactRepository.save(
        factBuilder(projectId, "FEATURE", subdomain.getSubdomainName() + " Capability")
            .parentFactId(epic.getFactId())
            .rootFactId(epic.getFactId())
            .factKey("subdomain:" + subdomain.getSubdomainId())
            .summary(subdomain.getDescription())
            .content(subdomain.getKnowledge())
            .priority("MEDIUM")
            .sourceEntityType("SUBDOMAIN")
            .sourceEntityId(subdomain.getSubdomainId())
            .attributes(
                toJson(
                    Map.of(
                        "description", nullToEmpty(subdomain.getDescription()),
                        "capability", nullToEmpty(subdomain.getKnowledge()))))
            .build());
  }

  /** Creates USER_STORYs for workflows belonging to a domain (no subdomain). */
  private int createStoriesForDomain(
      UUID projectId,
      KnowledgeFactEntity epic,
      DomainEntity domain,
      List<WorkflowEntity> allWorkflows,
      List<BusinessRuleEntity> allRules) {

    int created = 0;
    List<WorkflowEntity> domainWorkflows = findWorkflowsForDomain(domain, allWorkflows);

    for (WorkflowEntity workflow : domainWorkflows) {
      KnowledgeFactEntity story = createUserStory(projectId, epic, epic, workflow);
      created++;

      List<BusinessRuleEntity> relevantRules = findRelevantRules(workflow, domain, null, allRules);
      created +=
          addRuleDrivenValidation(projectId, epic.getFactId(), story.getFactId(), relevantRules);
    }

    return created;
  }

  /** Creates USER_STORYs for workflows belonging to a subdomain. */
  private int createStoriesForSubdomain(
      UUID projectId,
      KnowledgeFactEntity epic,
      KnowledgeFactEntity feature,
      SubdomainEntity subdomain,
      List<WorkflowEntity> allWorkflows,
      List<BusinessRuleEntity> allRules) {

    int created = 0;
    List<WorkflowEntity> subdomainWorkflows = findWorkflowsForSubdomain(subdomain, allWorkflows);

    for (WorkflowEntity workflow : subdomainWorkflows) {
      KnowledgeFactEntity story = createUserStory(projectId, epic, feature, workflow);
      created++;

      List<BusinessRuleEntity> relevantRules =
          findRelevantRules(workflow, null, subdomain, allRules);
      created +=
          addRuleDrivenValidation(projectId, epic.getFactId(), story.getFactId(), relevantRules);
    }

    return created;
  }

  /** Creates a USER_STORY from a workflow. */
  private KnowledgeFactEntity createUserStory(
      UUID projectId,
      KnowledgeFactEntity epic,
      KnowledgeFactEntity parentFeature,
      WorkflowEntity workflow) {

    return knowledgeFactRepository.save(
        factBuilder(projectId, "USER_STORY", workflow.getWorkflowName())
            .parentFactId(parentFeature.getFactId())
            .rootFactId(epic.getFactId())
            .factKey("workflow:" + workflow.getWorkflowId())
            .summary(workflow.getOutcomeText())
            .content(workflow.getTriggerText())
            .priority("MEDIUM")
            .sourceEntityType("WORKFLOW")
            .sourceEntityId(workflow.getWorkflowId())
            .attributes(
                toJson(
                    Map.of(
                        "asA", nullToEmpty(workflow.getActor()),
                        "iWant", nullToEmpty(workflow.getTriggerText()),
                        "soThat", nullToEmpty(workflow.getOutcomeText()),
                        "description", nullToEmpty(workflow.getOutcomeText()))))
            .build());
  }

  /** Finds workflows that belong to a domain (via relationships or keyword matching). */
  private List<WorkflowEntity> findWorkflowsForDomain(
      DomainEntity domain, List<WorkflowEntity> allWorkflows) {

    // Get components in this domain
    List<UUID> domainComponentIds =
        componentRepository.findByProjectId(domain.getProjectId()).stream()
            .filter(c -> domain.getDomainId().equals(c.getDomainId()))
            .map(ComponentEntity::getComponentId)
            .toList();

    // Find workflows linked to domain components via relationships
    List<UUID> linkedWorkflowIds =
        relationshipRepository.findByProjectId(domain.getProjectId()).stream()
            .filter(
                r ->
                    "workflow".equalsIgnoreCase(r.getSourceEntityType())
                        || "business_flow".equalsIgnoreCase(r.getSourceEntityType()))
            .filter(
                r ->
                    domainComponentIds.contains(r.getTargetEntityId())
                        || domainComponentIds.contains(r.getSourceEntityId()))
            .map(
                r ->
                    "workflow".equalsIgnoreCase(r.getSourceEntityType())
                        ? r.getSourceEntityId()
                        : r.getTargetEntityId())
            .distinct()
            .toList();

    // Match workflows by ID or keyword similarity
    String domainText = domainText(domain);
    return allWorkflows.stream()
        .filter(
            w ->
                linkedWorkflowIds.contains(w.getWorkflowId())
                    || hasMeaningfulOverlap(workflowText(w), domainText))
        .toList();
  }

  /** Finds workflows that belong to a subdomain (via relationships or keyword matching). */
  private List<WorkflowEntity> findWorkflowsForSubdomain(
      SubdomainEntity subdomain, List<WorkflowEntity> allWorkflows) {

    // Get components in this subdomain
    List<UUID> subdomainComponentIds =
        componentRepository.findByProjectId(subdomain.getProjectId()).stream()
            .filter(c -> subdomain.getSubdomainId().equals(c.getSubdomainId()))
            .map(ComponentEntity::getComponentId)
            .toList();

    // Find workflows linked to subdomain components via relationships
    List<UUID> linkedWorkflowIds =
        relationshipRepository.findByProjectId(subdomain.getProjectId()).stream()
            .filter(
                r ->
                    "workflow".equalsIgnoreCase(r.getSourceEntityType())
                        || "business_flow".equalsIgnoreCase(r.getSourceEntityType()))
            .filter(
                r ->
                    subdomainComponentIds.contains(r.getTargetEntityId())
                        || subdomainComponentIds.contains(r.getSourceEntityId()))
            .map(
                r ->
                    "workflow".equalsIgnoreCase(r.getSourceEntityType())
                        ? r.getSourceEntityId()
                        : r.getTargetEntityId())
            .distinct()
            .toList();

    // Match workflows by ID or keyword similarity
    String subdomainText = subdomainText(subdomain);
    return allWorkflows.stream()
        .filter(
            w ->
                linkedWorkflowIds.contains(w.getWorkflowId())
                    || hasMeaningfulOverlap(workflowText(w), subdomainText))
        .toList();
  }

  /** Finds business rules relevant to a workflow. */
  private List<BusinessRuleEntity> findRelevantRules(
      WorkflowEntity workflow,
      DomainEntity domain,
      SubdomainEntity subdomain,
      List<BusinessRuleEntity> allRules) {

    // Get component IDs for the domain/subdomain
    List<UUID> scopeComponentIds =
        componentRepository.findByProjectId(workflow.getProjectId()).stream()
            .filter(
                c ->
                    (domain != null && domain.getDomainId().equals(c.getDomainId()))
                        || (subdomain != null
                            && subdomain.getSubdomainId().equals(c.getSubdomainId())))
            .map(ComponentEntity::getComponentId)
            .toList();

    String workflowTextContent = workflowText(workflow);

    return allRules.stream()
        .filter(rule -> isRuleRelevantToWorkflow(rule, scopeComponentIds, workflowTextContent))
        .toList();
  }

  /** Checks if a business rule is relevant to a workflow. */
  private boolean isRuleRelevantToWorkflow(
      BusinessRuleEntity rule, List<UUID> scopeComponentIds, String workflowText) {

    // Rule belongs to a component in scope
    if (rule.getComponentId() != null && scopeComponentIds.contains(rule.getComponentId())) {
      return true;
    }

    // Rule text matches workflow text via keywords
    return hasMeaningfulOverlap(ruleText(rule), workflowText);
  }

  private String domainText(DomainEntity domain) {
    return String.join(
        " ",
        nullToEmpty(domain.getDomainName()),
        nullToEmpty(domain.getDescription()),
        nullToEmpty(domain.getKnowledge()));
  }

  private String subdomainText(SubdomainEntity subdomain) {
    return String.join(
        " ",
        nullToEmpty(subdomain.getSubdomainName()),
        nullToEmpty(subdomain.getDescription()),
        nullToEmpty(subdomain.getKnowledge()));
  }

  private String workflowText(WorkflowEntity workflow) {
    return String.join(
        " ",
        nullToEmpty(workflow.getWorkflowName()),
        nullToEmpty(workflow.getActor()),
        nullToEmpty(workflow.getTriggerText()),
        nullToEmpty(workflow.getOutcomeText()));
  }

  private boolean hasMeaningfulOverlap(String source, String target) {
    Set<String> targetTerms = keywords(target);
    int matches = 0;
    for (String term : keywords(source)) {
      if (targetTerms.contains(term)) {
        matches++;
      }
      if (matches >= 2 || (matches == 1 && term.length() >= 8)) {
        return true;
      }
    }
    return false;
  }

  private Set<String> keywords(String value) {
    Set<String> stopWords =
        Set.of(
            "the",
            "and",
            "for",
            "with",
            "from",
            "that",
            "this",
            "when",
            "then",
            "into",
            "related",
            "workflow",
            "rule",
            "rules",
            "validation",
            "execute",
            "expected");
    return java.util.Arrays.stream(nullToEmpty(value).toLowerCase().replace('-', ' ').split("\\W+"))
        .filter(term -> term.length() > 3)
        .filter(term -> !stopWords.contains(term))
        .collect(java.util.stream.Collectors.toSet());
  }

  private String ruleText(BusinessRuleEntity rule) {
    return String.join(
        " ",
        nullToEmpty(rule.getRuleName()),
        nullToEmpty(rule.getRuleType()),
        nullToEmpty(rule.getConditionText()),
        nullToEmpty(rule.getOutcomeText()),
        nullToEmpty(rule.getExceptionText()),
        nullToEmpty(rule.getValidationCriteria()));
  }

  // Removed: workflowText() - module support disabled

  private int addRuleDrivenValidation(
      UUID projectId, UUID rootFactId, UUID storyFactId, List<BusinessRuleEntity> rules) {
    int created = 0;
    for (BusinessRuleEntity rule : rules) {
      knowledgeFactRepository.save(
          factBuilder(
                  projectId,
                  "ACCEPTANCE_CRITERIA",
                  firstNonBlank(rule.getValidationCriteria(), rule.getRuleName()))
              .parentFactId(storyFactId)
              .rootFactId(rootFactId)
              .factKey("rule-criteria:" + rule.getRuleId())
              .summary(rule.getValidationCriteria())
              .content(rule.getOutcomeText())
              .sourceEntityType("BUSINESS_RULE")
              .sourceEntityId(rule.getRuleId())
              .sourceRuleId(rule.getRuleId())
              .attributes(
                  toJson(
                      Map.of(
                          "criteriaText", nullToEmpty(rule.getValidationCriteria()),
                          "givenText", nullToEmpty(rule.getConditionText()),
                          "whenText", "the related workflow is executed",
                          "thenText", nullToEmpty(rule.getOutcomeText()))))
              .build());
      knowledgeFactRepository.save(
          factBuilder(projectId, "TEST_SCENARIO", "Validate " + rule.getRuleName())
              .parentFactId(storyFactId)
              .rootFactId(rootFactId)
              .factKey("rule-scenario:" + rule.getRuleId())
              .summary(rule.getOutcomeText())
              .content(rule.getOutcomeText())
              .sourceEntityType("BUSINESS_RULE")
              .sourceEntityId(rule.getRuleId())
              .sourceRuleId(rule.getRuleId())
              .attributes(
                  toJson(
                      Map.of(
                          "scenarioType",
                          "rule",
                          "steps",
                          List.of(
                              "Arrange rule condition",
                              "Execute workflow",
                              "Assert expected outcome"),
                          "expectedResult",
                          nullToEmpty(rule.getOutcomeText()))))
              .build());
      created += 2;
    }
    return created;
  }

  private KnowledgeFactEntity.KnowledgeFactEntityBuilder factBuilder(
      UUID projectId, String factType, String title) {
    return KnowledgeFactEntity.builder()
        .projectId(projectId)
        .factType(factType)
        .title(firstNonBlank(title, factType))
        .priority("MEDIUM");
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize planning fact attributes", e);
      return "{}";
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
