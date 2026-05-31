package com.knowledge.acquisition.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.entity.*;
import com.knowledge.acquisition.repository.*;
import java.util.HashMap;
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
  private final ModuleRepository moduleRepository;
  private final WorkflowRepository workflowRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final ComponentRepository componentRepository;
  private final KnowledgeFactRepository knowledgeFactRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public int generate(UUID projectId) {
    List<ModuleEntity> modules = moduleRepository.findByProjectId(projectId);
    List<WorkflowEntity> workflows = workflowRepository.findByProjectId(projectId);
    List<BusinessRuleEntity> rules = businessRuleRepository.findByProjectId(projectId);
    Map<UUID, UUID> componentModuleIds = componentModuleIds(projectId);

    int created = 0;
    for (ModuleEntity module : modules) {
      KnowledgeFactEntity epic =
          knowledgeFactRepository.save(
              factBuilder(projectId, "EPIC", "Deliver " + module.getModuleName())
                  .factKey("module:" + module.getModuleId())
                  .summary(module.getResponsibility())
                  .content(module.getKnowledge())
                  .priority("MEDIUM")
                  .sourceEntityType("MODULE")
                  .sourceEntityId(module.getModuleId())
                  .attributes(
                      toJson(
                          Map.of(
                              "problemStatement", nullToEmpty(module.getResponsibility()),
                              "businessValue", nullToEmpty(module.getKnowledge()),
                              "scope", nullToEmpty(module.getKnowledge()))))
                  .build());
      created++;

      KnowledgeFactEntity feature =
          knowledgeFactRepository.save(
              factBuilder(projectId, "FEATURE", module.getModuleName() + " Capability")
                  .parentFactId(epic.getFactId())
                  .rootFactId(epic.getFactId())
                  .factKey("module-capability:" + module.getModuleId())
                  .summary(module.getResponsibility())
                  .content(module.getModuleType())
                  .priority("MEDIUM")
                  .sourceEntityType("MODULE")
                  .sourceEntityId(module.getModuleId())
                  .attributes(
                      toJson(
                          Map.of(
                              "description", nullToEmpty(module.getResponsibility()),
                              "capability", nullToEmpty(module.getModuleType()))))
                  .build());
      created++;

      for (WorkflowEntity workflow : workflows) {
        if (workflow.getModuleId() != null
            && !workflow.getModuleId().equals(module.getModuleId())) {
          continue;
        }
        KnowledgeFactEntity story =
            knowledgeFactRepository.save(
                factBuilder(projectId, "USER_STORY", workflow.getWorkflowName())
                    .parentFactId(feature.getFactId())
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
        created++;
        created +=
            addRuleDrivenValidation(
                projectId,
                epic.getFactId(),
                story.getFactId(),
                relevantRules(module, workflow, rules, componentModuleIds));
      }
    }
    return created;
  }

  private Map<UUID, UUID> componentModuleIds(UUID projectId) {
    Map<UUID, UUID> componentModuleIds = new HashMap<>();
    for (ComponentEntity component : componentRepository.findByProjectId(projectId)) {
      if (component.getModuleId() != null) {
        componentModuleIds.put(component.getComponentId(), component.getModuleId());
      }
    }
    return componentModuleIds;
  }

  private List<BusinessRuleEntity> relevantRules(
      ModuleEntity module,
      WorkflowEntity workflow,
      List<BusinessRuleEntity> rules,
      Map<UUID, UUID> componentModuleIds) {
    return rules.stream()
        .filter(rule -> isRelevantRule(module, workflow, rule, componentModuleIds))
        .toList();
  }

  private boolean isRelevantRule(
      ModuleEntity module,
      WorkflowEntity workflow,
      BusinessRuleEntity rule,
      Map<UUID, UUID> componentModuleIds) {
    if (rule.getModuleId() != null) {
      return rule.getModuleId().equals(module.getModuleId());
    }
    if (rule.getComponentId() != null) {
      return module.getModuleId().equals(componentModuleIds.get(rule.getComponentId()));
    }
    return hasMeaningfulOverlap(ruleText(rule), workflowText(module, workflow));
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

  private String workflowText(ModuleEntity module, WorkflowEntity workflow) {
    return String.join(
        " ",
        nullToEmpty(module.getModuleName()),
        nullToEmpty(module.getModuleType()),
        nullToEmpty(module.getResponsibility()),
        nullToEmpty(module.getKnowledge()),
        nullToEmpty(workflow.getWorkflowName()),
        nullToEmpty(workflow.getActor()),
        nullToEmpty(workflow.getTriggerText()),
        nullToEmpty(workflow.getOutcomeText()));
  }

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
