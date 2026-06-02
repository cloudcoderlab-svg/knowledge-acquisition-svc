package com.knowledge.acquisition.controller;

import com.knowledge.acquisition.entity.*;
import com.knowledge.acquisition.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for analyzing extracted knowledge for downstream consumption.
 *
 * <p>Provides detailed analysis endpoints to evaluate how well the extracted knowledge supports
 * creation of epics, user stories, high-level design documents, and test cases.
 *
 * @author Knowledge Acquisition Service
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class KnowledgeAnalysisController {

  private final DomainRepository domainRepository;
  private final WorkflowRepository workflowRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final DataModelRepository dataModelRepository;
  private final ComponentRepository componentRepository;
  private final RelationshipRepository relationshipRepository;

  /**
   * Analyzes knowledge completeness for agile artifact generation.
   *
   * <p>Evaluates how well the extracted knowledge supports creation of epics, user stories, and
   * acceptance criteria.
   *
   * @param projectId the project UUID
   * @return analysis report with completeness metrics
   */
  @GetMapping("/projects/{projectId}/agile-readiness")
  public AgileReadinessReport analyzeAgileReadiness(@PathVariable UUID projectId) {
    List<WorkflowEntity> workflows = workflowRepository.findByProjectId(projectId);
    List<BusinessRuleEntity> rules = businessRuleRepository.findByProjectId(projectId);
    List<DomainEntity> domains = domainRepository.findByProjectId(projectId);

    // Analyze workflow completeness
    int workflowsWithTriggers =
        (int)
            workflows.stream()
                .filter(w -> w.getTriggerText() != null && !w.getTriggerText().isEmpty())
                .count();
    int workflowsWithOutcomes =
        (int)
            workflows.stream()
                .filter(w -> w.getOutcomeText() != null && !w.getOutcomeText().isEmpty())
                .count();
    int workflowsWithActors =
        (int)
            workflows.stream().filter(w -> w.getActor() != null && !w.getActor().isEmpty()).count();

    // Analyze business rule completeness
    int rulesWithConditions =
        (int)
            rules.stream()
                .filter(r -> r.getConditionText() != null && !r.getConditionText().isEmpty())
                .count();
    int rulesWithValidation =
        (int)
            rules.stream()
                .filter(
                    r -> r.getValidationCriteria() != null && !r.getValidationCriteria().isEmpty())
                .count();
    int rulesWithOutcomes =
        (int)
            rules.stream()
                .filter(r -> r.getOutcomeText() != null && !r.getOutcomeText().isEmpty())
                .count();

    // Analyze domain completeness
    int domainsWithDescriptions =
        (int)
            domains.stream()
                .filter(d -> d.getDescription() != null && !d.getDescription().isEmpty())
                .count();

    // Calculate completeness scores
    double workflowCompleteness =
        workflows.isEmpty()
            ? 0.0
            : ((workflowsWithTriggers + workflowsWithOutcomes + workflowsWithActors)
                    / (workflows.size() * 3.0))
                * 100;
    double ruleCompleteness =
        rules.isEmpty()
            ? 0.0
            : ((rulesWithConditions + rulesWithValidation + rulesWithOutcomes)
                    / (rules.size() * 3.0))
                * 100;
    double domainCompleteness =
        domains.isEmpty() ? 0.0 : (domainsWithDescriptions / (double) domains.size()) * 100;

    // Calculate overall agile readiness score
    double overallReadiness = (workflowCompleteness + ruleCompleteness + domainCompleteness) / 3.0;

    return new AgileReadinessReport(
        projectId,
        workflows.size(),
        workflowsWithTriggers,
        workflowsWithOutcomes,
        workflowsWithActors,
        workflowCompleteness,
        rules.size(),
        rulesWithConditions,
        rulesWithValidation,
        rulesWithOutcomes,
        ruleCompleteness,
        domains.size(),
        domainsWithDescriptions,
        domainCompleteness,
        overallReadiness,
        generateAgileRecommendations(
            overallReadiness, workflowCompleteness, ruleCompleteness, domainCompleteness));
  }

  /**
   * Analyzes knowledge completeness for High-Level Design (HLD) documentation.
   *
   * @param projectId the project UUID
   * @return HLD readiness report
   */
  @GetMapping("/projects/{projectId}/hld-readiness")
  public HLDReadinessReport analyzeHLDReadiness(@PathVariable UUID projectId) {
    List<DataModelEntity> dataModels = dataModelRepository.findByProjectId(projectId);
    List<ComponentEntity> components = componentRepository.findByProjectId(projectId);
    List<RelationshipEntity> relationships = relationshipRepository.findByProjectId(projectId);
    List<WorkflowEntity> workflows = workflowRepository.findByProjectId(projectId);

    // Analyze data model completeness
    int modelsWithBusinessDef =
        (int)
            dataModels.stream()
                .filter(
                    dm ->
                        dm.getBusinessDefinition() != null && !dm.getBusinessDefinition().isEmpty())
                .count();
    int modelsWithType =
        (int)
            dataModels.stream()
                .filter(dm -> dm.getModelType() != null && !dm.getModelType().isEmpty())
                .count();
    int modelsWithSchema =
        (int)
            dataModels.stream()
                .filter(
                    dm -> dm.getSchemaDefinition() != null && !dm.getSchemaDefinition().isEmpty())
                .count();

    // Analyze component completeness
    int componentsWithResponsibility =
        (int)
            components.stream()
                .filter(c -> c.getResponsibility() != null && !c.getResponsibility().isEmpty())
                .count();
    int componentsWithType =
        (int)
            components.stream()
                .filter(c -> c.getComponentType() != null && !c.getComponentType().isEmpty())
                .count();

    // Calculate completeness
    double dataModelCompleteness =
        dataModels.isEmpty()
            ? 0.0
            : ((modelsWithBusinessDef + modelsWithType + modelsWithSchema)
                    / (dataModels.size() * 3.0))
                * 100;
    double componentCompleteness =
        components.isEmpty()
            ? 0.0
            : ((componentsWithResponsibility + componentsWithType) / (components.size() * 2.0))
                * 100;
    double relationshipDensity =
        (dataModels.isEmpty() || components.isEmpty())
            ? 0.0
            : (relationships.size()
                    / (double) (dataModels.size() + components.size() + workflows.size()))
                * 100;

    double overallReadiness =
        (dataModelCompleteness + componentCompleteness + Math.min(relationshipDensity, 100)) / 3.0;

    return new HLDReadinessReport(
        projectId,
        dataModels.size(),
        modelsWithBusinessDef,
        modelsWithSchema,
        dataModelCompleteness,
        components.size(),
        componentsWithResponsibility,
        componentCompleteness,
        relationships.size(),
        relationshipDensity,
        overallReadiness,
        generateHLDRecommendations(overallReadiness, dataModelCompleteness, componentCompleteness));
  }

  /**
   * Analyzes knowledge completeness for test case generation.
   *
   * @param projectId the project UUID
   * @return test case readiness report
   */
  @GetMapping("/projects/{projectId}/test-readiness")
  public TestReadinessReport analyzeTestReadiness(@PathVariable UUID projectId) {
    List<WorkflowEntity> workflows = workflowRepository.findByProjectId(projectId);
    List<BusinessRuleEntity> rules = businessRuleRepository.findByProjectId(projectId);

    // Test cases need: triggers (given), conditions (when), outcomes (then)
    int workflowsTestable =
        (int)
            workflows.stream()
                .filter(
                    w ->
                        (w.getTriggerText() != null && !w.getTriggerText().isEmpty())
                            || (w.getOutcomeText() != null && !w.getOutcomeText().isEmpty()))
                .count();

    int rulesTestable =
        (int)
            rules.stream()
                .filter(
                    r ->
                        (r.getConditionText() != null && !r.getConditionText().isEmpty())
                            || (r.getValidationCriteria() != null
                                && !r.getValidationCriteria().isEmpty())
                            || (r.getOutcomeText() != null && !r.getOutcomeText().isEmpty()))
                .count();

    double workflowTestability =
        workflows.isEmpty() ? 0.0 : (workflowsTestable / (double) workflows.size()) * 100;
    double ruleTestability = rules.isEmpty() ? 0.0 : (rulesTestable / (double) rules.size()) * 100;
    double overallTestability = (workflowTestability + ruleTestability) / 2.0;

    int estimatedTestCases =
        workflowsTestable * 3 + rulesTestable * 2; // 3 tests per workflow, 2 per rule

    return new TestReadinessReport(
        projectId,
        workflows.size(),
        workflowsTestable,
        workflowTestability,
        rules.size(),
        rulesTestable,
        ruleTestability,
        estimatedTestCases,
        overallTestability,
        generateTestRecommendations(overallTestability, workflowTestability, ruleTestability));
  }

  /**
   * Generates actionable recommendations based on agile readiness scores.
   *
   * <p>Provides specific feedback on knowledge extraction quality and guidance on whether the
   * extracted knowledge is sufficient for creating agile artifacts (epics, user stories, acceptance
   * criteria).
   *
   * @param overall overall agile readiness score (0-100)
   * @param workflow workflow completeness percentage
   * @param rule business rule completeness percentage
   * @param domain domain description completeness percentage
   * @return list of recommendations for improving agile artifact readiness
   */
  private List<String> generateAgileRecommendations(
      double overall, double workflow, double rule, double domain) {
    List<String> recommendations = new ArrayList<>();

    if (overall < 30) {
      recommendations.add(
          "CRITICAL: Knowledge extraction is insufficient for agile artifact creation. Consider re-processing with enhanced extraction.");
    } else if (overall < 60) {
      recommendations.add(
          "WARNING: Knowledge has significant gaps. Manual enrichment recommended before creating epics/stories.");
    } else if (overall < 80) {
      recommendations.add(
          "GOOD: Knowledge is suitable for agile artifacts with minor manual enrichment.");
    } else {
      recommendations.add("EXCELLENT: Knowledge is ready for automated epic and story generation.");
    }

    if (workflow < 50) {
      recommendations.add(
          "Workflows missing trigger/outcome details - difficult to write user stories in 'As a...When...Then' format");
    }
    if (rule < 50) {
      recommendations.add(
          "Business rules missing conditions/criteria - acceptance criteria will need manual definition");
    }
    if (domain < 50) {
      recommendations.add(
          "Domain descriptions missing - epic categorization will require manual effort");
    }

    return recommendations;
  }

  /**
   * Generates actionable recommendations based on HLD readiness scores.
   *
   * <p>Provides specific feedback on knowledge extraction quality for architecture documentation
   * and high-level design generation.
   *
   * @param overall overall HLD readiness score (0-100)
   * @param dataModel data model completeness percentage
   * @param component component completeness percentage
   * @return list of recommendations for improving HLD generation readiness
   */
  private List<String> generateHLDRecommendations(
      double overall, double dataModel, double component) {
    List<String> recommendations = new ArrayList<>();

    if (overall < 30) {
      recommendations.add(
          "CRITICAL: Insufficient data for HLD generation. Architecture diagrams will require extensive manual work.");
    } else if (overall < 60) {
      recommendations.add(
          "WARNING: HLD generation possible but requires significant manual enrichment.");
    } else if (overall < 80) {
      recommendations.add("GOOD: HLD can be generated with minor manual review and enhancement.");
    } else {
      recommendations.add(
          "EXCELLENT: Ready for automated HLD and architecture diagram generation.");
    }

    if (dataModel < 50) {
      recommendations.add(
          "Data models lack schema definitions - entity relationship diagrams will be incomplete");
    }
    if (component < 50) {
      recommendations.add(
          "Components missing descriptions - component diagrams need manual enrichment");
    }

    return recommendations;
  }

  /**
   * Generates actionable recommendations based on test case generation readiness scores.
   *
   * <p>Provides specific feedback on knowledge extraction quality for automated test case
   * generation using Given-When-Then format.
   *
   * @param overall overall test readiness score (0-100)
   * @param workflow workflow testability percentage
   * @param rule business rule testability percentage
   * @return list of recommendations for improving test case generation readiness
   */
  private List<String> generateTestRecommendations(double overall, double workflow, double rule) {
    List<String> recommendations = new ArrayList<>();

    if (overall < 30) {
      recommendations.add(
          "CRITICAL: Test case generation not feasible. Manual test design required.");
    } else if (overall < 60) {
      recommendations.add(
          "WARNING: Limited test case generation possible. Requires manual test scenario definition.");
    } else if (overall < 80) {
      recommendations.add("GOOD: Can generate test scenarios with manual review and enhancement.");
    } else {
      recommendations.add(
          "EXCELLENT: Ready for automated test case generation with given-when-then format.");
    }

    if (workflow < 50) {
      recommendations.add(
          "Workflow testing limited - missing triggers/outcomes for integration test scenarios");
    }
    if (rule < 50) {
      recommendations.add(
          "Business rule testing limited - missing validation criteria for unit test cases");
    }

    return recommendations;
  }

  /**
   * Report analyzing readiness for agile artifact generation (epics, user stories, acceptance
   * criteria).
   *
   * <p>Evaluates the completeness of extracted workflows, business rules, and domains to determine
   * if sufficient knowledge exists to automatically generate agile artifacts. The overall score
   * ranges from 0-100, with:
   *
   * <ul>
   *   <li><b>0-30:</b> CRITICAL - Insufficient for artifact generation, requires re-extraction
   *   <li><b>30-60:</b> WARNING - Significant gaps, manual enrichment needed
   *   <li><b>60-80:</b> GOOD - Suitable with minor manual enrichment
   *   <li><b>80-100:</b> EXCELLENT - Ready for automated generation
   * </ul>
   *
   * @param projectId the project identifier
   * @param totalWorkflows total number of extracted workflows
   * @param workflowsWithTriggers workflows with defined triggers (for "Given" statements)
   * @param workflowsWithOutcomes workflows with defined outcomes (for "Then" statements)
   * @param workflowsWithActors workflows with identified actors (for "As a" statements)
   * @param workflowCompletenessPercent workflow completeness score (0-100)
   * @param totalBusinessRules total number of extracted business rules
   * @param rulesWithConditions rules with condition definitions (for acceptance criteria)
   * @param rulesWithValidation rules with validation criteria (for test scenarios)
   * @param rulesWithOutcomes rules with outcome definitions (for expected results)
   * @param ruleCompletenessPercent business rule completeness score (0-100)
   * @param totalDomains total number of identified domains
   * @param domainsWithDescriptions domains with descriptions (for epic categorization)
   * @param domainCompletenessPercent domain completeness score (0-100)
   * @param overallAgileReadinessScore overall agile readiness score (0-100)
   * @param recommendations actionable recommendations for improving readiness
   */
  public record AgileReadinessReport(
      UUID projectId,
      int totalWorkflows,
      int workflowsWithTriggers,
      int workflowsWithOutcomes,
      int workflowsWithActors,
      double workflowCompletenessPercent,
      int totalBusinessRules,
      int rulesWithConditions,
      int rulesWithValidation,
      int rulesWithOutcomes,
      double ruleCompletenessPercent,
      int totalDomains,
      int domainsWithDescriptions,
      double domainCompletenessPercent,
      double overallAgileReadinessScore,
      List<String> recommendations) {}

  /**
   * Report analyzing readiness for High-Level Design (HLD) document generation.
   *
   * <p>Evaluates the completeness of extracted data models, components, and relationships to
   * determine if sufficient architectural knowledge exists to automatically generate HLD documents,
   * architecture diagrams, and component diagrams. The overall score ranges from 0-100, with:
   *
   * <ul>
   *   <li><b>0-30:</b> CRITICAL - Insufficient for HLD, extensive manual work required
   *   <li><b>30-60:</b> WARNING - HLD possible but requires significant enrichment
   *   <li><b>60-80:</b> GOOD - Can generate HLD with minor manual review
   *   <li><b>80-100:</b> EXCELLENT - Ready for automated HLD and diagram generation
   * </ul>
   *
   * @param projectId the project identifier
   * @param totalDataModels total number of extracted data models
   * @param modelsWithBusinessDefinition data models with business definitions
   * @param modelsWithSchema data models with schema definitions
   * @param dataModelCompletenessPercent data model completeness score (0-100)
   * @param totalComponents total number of extracted components
   * @param componentsWithResponsibilityription components with responsibility descriptions
   * @param componentCompletenessPercent component completeness score (0-100)
   * @param totalRelationships total number of identified relationships
   * @param relationshipDensityPercent relationship density score (0-100+)
   * @param overallHLDReadinessScore overall HLD readiness score (0-100)
   * @param recommendations actionable recommendations for improving HLD readiness
   */
  public record HLDReadinessReport(
      UUID projectId,
      int totalDataModels,
      int modelsWithBusinessDefinition,
      int modelsWithSchema,
      double dataModelCompletenessPercent,
      int totalComponents,
      int componentsWithResponsibilityription,
      double componentCompletenessPercent,
      int totalRelationships,
      double relationshipDensityPercent,
      double overallHLDReadinessScore,
      List<String> recommendations) {}

  /**
   * Report analyzing readiness for automated test case generation.
   *
   * <p>Evaluates the completeness of extracted workflows and business rules to determine if
   * sufficient knowledge exists to automatically generate test cases in Given-When-Then format. The
   * overall score ranges from 0-100, with:
   *
   * <ul>
   *   <li><b>0-30:</b> CRITICAL - Test generation not feasible, manual design required
   *   <li><b>30-60:</b> WARNING - Limited test generation, requires manual scenarios
   *   <li><b>60-80:</b> GOOD - Can generate tests with manual review
   *   <li><b>80-100:</b> EXCELLENT - Ready for automated test case generation
   * </ul>
   *
   * <p>Test case estimation assumes 3 test scenarios per workflow (happy path, edge case, error
   * case) and 2 test scenarios per business rule (valid input, invalid input).
   *
   * @param projectId the project identifier
   * @param totalWorkflows total number of extracted workflows
   * @param testableWorkflows workflows with sufficient detail for test generation
   * @param workflowTestabilityPercent workflow testability score (0-100)
   * @param totalBusinessRules total number of extracted business rules
   * @param testableBusinessRules rules with sufficient detail for test generation
   * @param ruleTestabilityPercent business rule testability score (0-100)
   * @param estimatedTestCases estimated number of test cases that can be generated
   * @param overallTestReadinessScore overall test readiness score (0-100)
   * @param recommendations actionable recommendations for improving test generation readiness
   */
  public record TestReadinessReport(
      UUID projectId,
      int totalWorkflows,
      int testableWorkflows,
      double workflowTestabilityPercent,
      int totalBusinessRules,
      int testableBusinessRules,
      double ruleTestabilityPercent,
      int estimatedTestCases,
      double overallTestReadinessScore,
      List<String> recommendations) {}
}
