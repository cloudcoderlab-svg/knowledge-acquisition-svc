package com.knowledge.acquisition.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

  @Bean(name = "ingestionProcessExecutor")
  public Executor ingestionProcessExecutor(
      @Value("${knowledge-engine.ingestion.project-parallelism:5}") int parallelism) {
    int workers = Math.max(1, parallelism);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ingestion-process-");
    executor.setCorePoolSize(workers);
    executor.setMaxPoolSize(workers);
    executor.setQueueCapacity(20);
    executor.initialize();
    return executor;
  }

  @Bean(name = "projectPipelineExecutor")
  public Executor projectPipelineExecutor(
      @Value("${knowledge-engine.project-pipeline.parallelism:5}") int parallelism) {
    int workers = Math.max(1, parallelism);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("project-pipeline-");
    executor.setCorePoolSize(workers);
    executor.setMaxPoolSize(workers);
    executor.setQueueCapacity(100);
    executor.initialize();
    return executor;
  }

  @Bean(name = "ingestionDocumentExecutor")
  public Executor ingestionDocumentExecutor(
      @Value("${knowledge-engine.ingestion.parallelism:2}") int parallelism) {
    int workers = Math.max(1, parallelism);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ingestion-document-");
    executor.setCorePoolSize(workers);
    executor.setMaxPoolSize(workers);
    executor.setQueueCapacity(200);
    executor.initialize();
    return executor;
  }
}
