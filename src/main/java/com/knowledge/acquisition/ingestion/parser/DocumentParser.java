package com.knowledge.acquisition.ingestion.parser;

import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class DocumentParser {

  private final Tika tika = new Tika();

  public String parse(InputStream inputStream) throws Exception {
    return tika.parseToString(inputStream);
  }
}
