package com.knowledge.acquisition.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class XMLPlatformDetectorTest {
  private final XMLPlatformDetector detector = new XMLPlatformDetector();

  @Test
  void detectsTibcoMdmFromRepositoryContent() {
    String xml =
        """
        <Repository name="RAM">
          <Entity name="Affiliation">
            <Field name="AffiliationId" type="String"/>
          </Entity>
        </Repository>
        """;

    assertThat(detector.detectPlatform(xml, "catalogvalidation.xml"))
        .isEqualTo(XMLPlatformDetector.XMLPlatform.TIBCO_MDM);
  }

  @Test
  void detectsTibcoMdmFromRamFileNameAndRuleContent() {
    String xml =
        """
        <Rulebase name="rbCheckApprovalRequired">
          <Rule name="CheckApprovalRequired"/>
        </Rulebase>
        """;

    assertThat(detector.detectPlatform(xml, "rbCheckApprovalRequired.xml"))
        .isEqualTo(XMLPlatformDetector.XMLPlatform.TIBCO_MDM);
  }

  @Test
  void detectsTibcoMdmFromCvAndFmRulebaseFiles() {
    String xml =
        """
        <Rulebase name="MandatoryFields">
          <Rule name="RequiredFieldCheck"/>
        </Rulebase>
        """;

    assertThat(detector.detectPlatform(xml, "cvAffiliation_MandatoryFields.xml"))
        .isEqualTo(XMLPlatformDetector.XMLPlatform.TIBCO_MDM);
    assertThat(detector.detectPlatform(xml, "fm26ca.xml"))
        .isEqualTo(XMLPlatformDetector.XMLPlatform.TIBCO_MDM);
  }

  @Test
  void keepsUnknownXmlGeneric() {
    String xml = "<settings><entry key=\"timeout\">30</entry></settings>";

    assertThat(detector.detectPlatform(xml, "settings.xml"))
        .isEqualTo(XMLPlatformDetector.XMLPlatform.GENERIC_XML);
  }
}
