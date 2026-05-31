package com.knowledge.acquisition.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class KnowledgeIngestionServiceTest {

  @Test
  void decodesXmlUsingDeclaredSingleByteEncoding() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><Repository name=\"Affiliati\u00f6n\"/>";

    String decoded =
        KnowledgeIngestionService.decodeRawText(xml.getBytes(StandardCharsets.ISO_8859_1), "xml");

    assertThat(decoded).contains("Affiliati\u00f6n");
  }

  @Test
  void decodesUtf16XmlWithoutRelyingOnUtf8Fallback() {
    String xml = "<?xml version=\"1.0\"?><Repository name=\"RAM\"/>";

    String decoded =
        KnowledgeIngestionService.decodeRawText(xml.getBytes(StandardCharsets.UTF_16LE), "xml");

    assertThat(decoded).contains("<Repository name=\"RAM\"");
  }
}
