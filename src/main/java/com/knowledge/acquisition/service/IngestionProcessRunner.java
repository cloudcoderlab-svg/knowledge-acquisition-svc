package com.knowledge.acquisition.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.knowledge.acquisition.entity.ProcessTrackingEntity;
import com.knowledge.acquisition.ingestion.service.KnowledgeIngestionService;
import com.knowledge.acquisition.ingestion.service.ProjectKnowledgeResetService;
import com.knowledge.acquisition.repository.ProcessTrackingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionProcessRunner {
  private final ProcessTrackingRepository processRepository;
  private final ProjectKnowledgeResetService projectKnowledgeResetService;
  private final KnowledgeIngestionService ingestionService;
  private final Executor documentExecutor;
  private final Storage storage;
  private final ProjectService projectService;

  public IngestionProcessRunner(
      ProcessTrackingRepository processRepository,
      ProjectKnowledgeResetService projectKnowledgeResetService,
      KnowledgeIngestionService ingestionService,
      @Qualifier("ingestionDocumentExecutor") Executor documentExecutor,
      Storage storage,
      ProjectService projectService) {
    this.processRepository = processRepository;
    this.projectKnowledgeResetService = projectKnowledgeResetService;
    this.ingestionService = ingestionService;
    this.documentExecutor = documentExecutor;
    this.storage = storage;
    this.projectService = projectService;
  }

  @Async("ingestionProcessExecutor")
  public void runIngestion(UUID processId, UUID projectId, String bucketName, List<String> files) {
    AtomicInteger processed = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    AtomicLong totalBytes = new AtomicLong();
    AtomicLong totalTokens = new AtomicLong();
    try {
      projectKnowledgeResetService.resetIngestionData(projectId);
      updateProcess(
          processId,
          process -> {
            process.setStatus("RUNNING");
            process.setProcessedFiles(0);
            process.setFailedFiles(0);
            process.setFailureCause(null);
            process.setTotalBytesProcessed(0L);
            process.setTotalTokensProcessed(0L);
          });

      List<CompletableFuture<Void>> futures =
          files.stream()
              .map(
                  file ->
                      CompletableFuture.runAsync(
                          () ->
                              processFile(
                                  processId,
                                  bucketName,
                                  file,
                                  processed,
                                  failed,
                                  totalBytes,
                                  totalTokens),
                          documentExecutor))
              .toList();

      // Wait for all document processing tasks to complete
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

      // Determine final process status based on failures
      int failedCount = failed.get();
      long finalBytes = totalBytes.get();
      long finalTokens = totalTokens.get();
      boolean isSuccess = failedCount == 0;

      // COMPLETED: All documents processed successfully
      // PARTIAL_SUCCESS: Some documents failed but process completed
      String processStatus = isSuccess ? "COMPLETED" : "PARTIAL_SUCCESS";

      updateProcess(
          processId,
          process -> {
            process.setProcessedFiles(processed.get());
            process.setFailedFiles(failedCount);
            process.setCurrentFile(null);
            process.setStatus(processStatus);
            process.setTotalBytesProcessed(finalBytes);
            process.setTotalTokensProcessed(finalTokens);
            process.setCompletedAt(OffsetDateTime.now());
          });

      // Activate project for both COMPLETED and PARTIAL_SUCCESS ingestions
      // Even with some failed documents, the project can still be marked ACTIVE
      // Multiple versions can coexist as ACTIVE
      try {
        projectService.onIngestionSuccess(projectId, processStatus);
      } catch (Exception e) {
        log.error("Failed to activate project after ingestion: {}", projectId, e);
      }
    } catch (Exception e) {
      log.error("Ingestion process failed: {}", processId, e);
      updateProcess(
          processId,
          process -> {
            process.setStatus("FAILED");
            process.setFailureCause(shortMessage(e));
            process.setCompletedAt(OffsetDateTime.now());
          });
    }
  }

  private void processFile(
      UUID processId,
      String bucketName,
      String file,
      AtomicInteger processed,
      AtomicInteger failed,
      AtomicLong totalBytes,
      AtomicLong totalTokens) {
    updateProcess(processId, process -> process.setCurrentFile(file));
    try {
      long fileSize = getFileSize(bucketName, file);
      com.knowledge.acquisition.dto.ClassificationResult result =
          ingestionService.ingestFromGcs(bucketName, file);

      // Accumulate tokens consumed for this file
      if (result != null && result.getTokensConsumed() != null) {
        totalTokens.addAndGet(result.getTokensConsumed());
        log.debug("File {} consumed {} tokens", file, result.getTokensConsumed());
      }

      totalBytes.addAndGet(fileSize);
      long currentBytes = totalBytes.get();
      long currentTokens = totalTokens.get();
      int processedCount = processed.incrementAndGet();
      updateProcess(
          processId,
          process -> {
            process.setProcessedFiles(processedCount);
            process.setTotalBytesProcessed(currentBytes);
            process.setTotalTokensProcessed(currentTokens);
          });
    } catch (Exception e) {
      int failedCount = failed.incrementAndGet();
      log.warn("Failed to ingest project file: {}", file, e);
      updateProcess(
          processId,
          process -> {
            process.setFailedFiles(failedCount);
            process.setFailureCause(file + ": " + shortMessage(e));
          });
    }
  }

  private long getFileSize(String bucketName, String objectName) {
    try {
      Blob blob = storage.get(BlobId.of(bucketName, objectName));
      return blob != null && blob.getSize() != null ? blob.getSize() : 0L;
    } catch (Exception e) {
      log.warn("Could not determine file size for {}", objectName, e);
      return 0L;
    }
  }

  private synchronized void updateProcess(
      UUID processId, java.util.function.Consumer<ProcessTrackingEntity> update) {
    processRepository
        .findById(processId)
        .ifPresent(
            process -> {
              update.accept(process);
              processRepository.save(process);
            });
  }

  private String shortMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      message = e.getClass().getSimpleName();
    }
    return message.length() <= 2000 ? message : message.substring(0, 2000);
  }
}
