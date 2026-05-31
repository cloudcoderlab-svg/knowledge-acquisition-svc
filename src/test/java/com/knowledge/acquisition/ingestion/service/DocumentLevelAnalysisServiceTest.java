package com.knowledge.acquisition.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.acquisition.dto.SourceDocumentMetadata;
import com.knowledge.acquisition.ingestion.service.ai.VertexAIService;
import com.knowledge.acquisition.ingestion.util.PromptLoaderUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentLevelAnalysisServiceTest {
  private final DocumentLevelAnalysisService service =
      new DocumentLevelAnalysisService(
          mock(VertexAIService.class),
          mock(PromptLoaderUtils.class),
          new XMLPlatformDetector(),
          new ObjectMapper());

  @Test
  void detectsCsvAsCsvDocumentNotGenericXml() {
    SourceDocumentMetadata source =
        new SourceDocumentMetadata(
            UUID.randomUUID(),
            "bucket",
            "projects/demo/affiliation-rules.csv",
            1L,
            "checksum",
            "hash",
            "document",
            "csv",
            "affiliation-rules.csv");

    XMLPlatformDetector.XMLPlatform platform =
        ReflectionTestUtils.invokeMethod(
            service, "detectPlatformFromFileType", source, "a,b\n1,2", null);

    assertThat(platform).isEqualTo(XMLPlatformDetector.XMLPlatform.CSV_DOCUMENT);
  }

  @Test
  void detectsXmlPlatformFromRawXmlInsteadOfParsedText() {
    SourceDocumentMetadata source =
        new SourceDocumentMetadata(
            UUID.randomUUID(),
            "bucket",
            "projects/demo/cvAffiliation_MandatoryFields.xml",
            1L,
            "checksum",
            "hash",
            "xml_doc",
            "xml",
            "cvAffiliation_MandatoryFields.xml");

    XMLPlatformDetector.XMLPlatform platform =
        ReflectionTestUtils.invokeMethod(
            service,
            "detectPlatformFromFileType",
            source,
            "Mandatory field checks",
            "<Rulebase name=\"MandatoryFields\"><Rule name=\"RequiredFieldCheck\"/></Rulebase>");

    assertThat(platform).isEqualTo(XMLPlatformDetector.XMLPlatform.TIBCO_MDM);
  }
}
