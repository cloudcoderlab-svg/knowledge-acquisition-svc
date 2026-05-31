package com.knowledge.acquisition.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeterministicXmlExtractionServiceTest {
  private final DeterministicXmlExtractionService service =
      new DeterministicXmlExtractionService(new XMLPlatformDetector());

  @Test
  void extractsModelsRulesAndWorkflowsFromXml() {
    String xml =
        """
        <Repository name="RAM">
          <Entity name="Affiliation">
            <Field name="AffiliationId" type="String" required="true"/>
          </Entity>
          <Validation name="PrimaryLocationRequired">
            <Condition>primaryLocation != null</Condition>
            <ErrorMessage>Primary location is required</ErrorMessage>
          </Validation>
          <WorkflowTemplate name="CreateAffiliation">
            <WorkflowStep name="Approve" actor="AM"/>
          </WorkflowTemplate>
          <Relationship source="Company" target="Affiliation" type="owns"/>
        </Repository>
        """;

    var result = service.extract(xml, "wfCreateAffiliation.xml");

    assertThat(result.getDataModels()).extracting("modelName").contains("Affiliation");
    assertThat(result.getDataModels().getFirst().getFields())
        .extracting("fieldName")
        .contains("AffiliationId");
    assertThat(result.getBusinessRules())
        .extracting("ruleName")
        .contains("PrimaryLocationRequired");
    assertThat(result.getBusinessFlows()).extracting("flowName").contains("CreateAffiliation");
    assertThat(result.getRelationships()).extracting("sourceName").contains("Company");
  }

  @Test
  void ignoresLeadingNoiseBeforeXmlProlog() {
    String xml =
        "\uFEFF\n\nDownloaded from Tika\n<?xml version=\"1.0\"?><Repository name=\"RAM\"/>";

    var result = service.extract(xml, "repository.xml");

    assertThat(result.getArchitecturalSummary()).contains("root element <Repository>");
  }
}
