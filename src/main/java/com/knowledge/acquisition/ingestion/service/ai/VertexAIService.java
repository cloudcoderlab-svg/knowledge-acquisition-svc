package com.knowledge.acquisition.ingestion.service.ai;

import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VertexAIService {

  @Value("${vertex.project-id}")
  private String projectId;

  @Value("${vertex.location}")
  private String location;

  @Value("${vertex.classification-model-name:${vertex.model-name}}")
  private String classificationModelName;

  @Value("${vertex.embedding-model-name}")
  private String embeddingModelName;

  @Value("${vertex.retry.max-attempts:4}")
  private int maxAttempts;

  @Value("${vertex.retry.initial-backoff-ms:2000}")
  private long initialBackoffMs;

  private VertexAI vertexAI;
  private GenerativeModel classificationModel;
  private PredictionServiceClient predictionServiceClient;

  @PostConstruct
  public void init() throws Exception {
    this.vertexAI = new VertexAI(projectId, location);
    this.classificationModel = new GenerativeModel(classificationModelName, vertexAI);
    this.predictionServiceClient =
        PredictionServiceClient.create(
            PredictionServiceSettings.newBuilder().setEndpoint(apiEndpoint()).build());
  }

  @PreDestroy
  public void close() {
    if (predictionServiceClient != null) {
      predictionServiceClient.close();
    }
  }

  @CircuitBreaker(name = "vertexai", fallbackMethod = "generateFallback")
  @Retry(name = "vertexai")
  public String generate(String prompt) throws Exception {
    log.debug("Calling Vertex AI for content generation");
    GenerateContentResponse response = classificationModel.generateContent(prompt);
    return response.getCandidates(0).getContent().getParts(0).getText();
  }

  private String generateFallback(String prompt, CallNotPermittedException ex) {
    log.error("Circuit breaker is OPEN for Vertex AI generation. Service is unavailable.");
    throw new RuntimeException(
        "Vertex AI service is currently unavailable. Please try again later.", ex);
  }

  private String generateFallback(String prompt, Exception ex) {
    log.error("Failed to generate content with Vertex AI after retries", ex);
    throw new RuntimeException("Failed to generate content with Vertex AI", ex);
  }

  /**
   * Generates content using both text and image/document input. This is used for multimodal
   * document analysis (PDFs, images with diagrams, etc.)
   *
   * @param prompt the text prompt
   * @param imageData the document/image binary data
   * @param mimeType the MIME type of the data (e.g., "application/pdf", "image/png")
   * @return the generated response text
   */
  @CircuitBreaker(name = "vertexai", fallbackMethod = "generateWithImageFallback")
  @Retry(name = "vertexai")
  public String generateWithImage(String prompt, byte[] imageData, String mimeType)
      throws Exception {
    log.debug("Calling Vertex AI for multimodal content generation");
    Content content =
        ContentMaker.fromMultiModalData(prompt, PartMaker.fromMimeTypeAndData(mimeType, imageData));

    GenerateContentResponse response = classificationModel.generateContent(content);
    return ResponseHandler.getText(response);
  }

  private String generateWithImageFallback(
      String prompt, byte[] imageData, String mimeType, CallNotPermittedException ex) {
    log.error(
        "Circuit breaker is OPEN for Vertex AI multimodal generation. Service is unavailable.");
    throw new RuntimeException(
        "Vertex AI multimodal service is currently unavailable. Please try again later.", ex);
  }

  private String generateWithImageFallback(
      String prompt, byte[] imageData, String mimeType, Exception ex) {
    log.error("Failed to generate multimodal content with Vertex AI after retries", ex);
    throw new RuntimeException("Failed to generate multimodal content with Vertex AI", ex);
  }

  @CircuitBreaker(name = "vertexai-embedding", fallbackMethod = "embeddingFallback")
  @Retry(name = "vertexai-embedding")
  public List<Float> embedding(String text) throws Exception {
    log.debug("Calling Vertex AI for embedding generation");
    com.google.protobuf.Value instance =
        com.google.protobuf.Value.newBuilder()
            .setStructValue(
                com.google.protobuf.Struct.newBuilder()
                    .putFields(
                        "content",
                        com.google.protobuf.Value.newBuilder().setStringValue(text).build()))
            .build();

    PredictResponse response =
        predictionServiceClient.predict(
            modelResourceName(), List.of(instance), com.google.protobuf.Value.newBuilder().build());

    return response
        .getPredictions(0)
        .getStructValue()
        .getFieldsOrThrow("embeddings")
        .getStructValue()
        .getFieldsOrThrow("values")
        .getListValue()
        .getValuesList()
        .stream()
        .map(value -> (float) value.getNumberValue())
        .toList();
  }

  private List<Float> embeddingFallback(String text, CallNotPermittedException ex) {
    log.warn(
        "Circuit breaker is OPEN for Vertex AI embedding. Returning null to allow graceful degradation.");
    // Return null to allow ingestion to continue without embeddings
    return null;
  }

  private List<Float> embeddingFallback(String text, Exception ex) {
    log.error("Failed to generate embedding with Vertex AI after retries", ex);
    // Return null to allow ingestion to continue without embeddings
    return null;
  }

  private String apiEndpoint() {
    if ("global".equals(location)) {
      return "aiplatform.googleapis.com:443";
    }
    return location + "-aiplatform.googleapis.com:443";
  }

  private String modelResourceName() {
    return String.format(
        "projects/%s/locations/%s/publishers/google/models/%s",
        projectId, location, embeddingModelName);
  }
}
