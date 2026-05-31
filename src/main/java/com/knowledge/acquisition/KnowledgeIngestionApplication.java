package com.knowledge.acquisition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KnowledgeIngestionApplication {

  public static void main(String[] args) {
    SpringApplication.run(KnowledgeIngestionApplication.class, args);
  }
}
