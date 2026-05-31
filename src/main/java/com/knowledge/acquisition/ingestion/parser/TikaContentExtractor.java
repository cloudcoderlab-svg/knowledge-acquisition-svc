package com.knowledge.acquisition.ingestion.parser;

import com.knowledge.acquisition.dto.DocumentContent;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Tika-based content extractor for text-only documents. Uses Apache Tika to extract plain text
 * content.
 */
@Component
@RequiredArgsConstructor
public class TikaContentExtractor implements DocumentContentExtractor {

  private final DocumentParser documentParser;

  @Override
  public DocumentContent extract(InputStream inputStream, String fileType) throws Exception {
    String textContent = documentParser.parse(inputStream);

    return DocumentContent.builder()
        .textContent(textContent)
        .diagrams(Collections.emptyList())
        .tables(Collections.emptyList())
        .metadata(new HashMap<>())
        .build();
  }
}
