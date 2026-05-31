package com.knowledge.acquisition.ingestion.parser;

import static org.mockito.Mockito.*;

import com.knowledge.acquisition.dto.DocumentContent;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentParserOrchestratorTest {
  private final GeminiMultimodalExtractor gemini = mock(GeminiMultimodalExtractor.class);
  private final TikaContentExtractor tika = mock(TikaContentExtractor.class);
  private final DocumentParserOrchestrator orchestrator =
      new DocumentParserOrchestrator(gemini, tika);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(orchestrator, "useMultimodal", true);
    ReflectionTestUtils.setField(
        orchestrator,
        "multimodalTypesConfig",
        "pdf,png,jpg,jpeg,gif,bmp,webp,doc,docx,ppt,pptx,xls,xlsx");
  }

  @Test
  void routesCsvAndXmlToTika() throws Exception {
    when(tika.extract(any(), any()))
        .thenReturn(DocumentContent.builder().textContent("ok").build());

    orchestrator.parseDocument(new ByteArrayInputStream("a,b".getBytes()), "csv");
    orchestrator.parseDocument(new ByteArrayInputStream("<x/>".getBytes()), "xml");

    verify(tika).extract(any(), eq("csv"));
    verify(tika).extract(any(), eq("xml"));
    verifyNoInteractions(gemini);
  }

  @Test
  void routesPdfImagesAndOfficeToMultimodalLlm() throws Exception {
    when(gemini.extract(any(), any()))
        .thenReturn(DocumentContent.builder().textContent("ok").build());

    orchestrator.parseDocument(new ByteArrayInputStream("pdf".getBytes()), "pdf");
    orchestrator.parseDocument(new ByteArrayInputStream("png".getBytes()), "png");
    orchestrator.parseDocument(new ByteArrayInputStream("docx".getBytes()), "docx");
    orchestrator.parseDocument(new ByteArrayInputStream("xlsx".getBytes()), "xlsx");

    verify(gemini).extract(any(), eq("pdf"));
    verify(gemini).extract(any(), eq("png"));
    verify(gemini).extract(any(), eq("docx"));
    verify(gemini).extract(any(), eq("xlsx"));
    verifyNoInteractions(tika);
  }

  @Test
  void routesUnknownTypeToTikaFallback() throws Exception {
    when(tika.extract(any(), any()))
        .thenReturn(DocumentContent.builder().textContent("ok").build());

    orchestrator.parseDocument(new ByteArrayInputStream("data".getBytes()), "dat");

    verify(tika).extract(any(), eq("dat"));
    verifyNoInteractions(gemini);
  }
}
