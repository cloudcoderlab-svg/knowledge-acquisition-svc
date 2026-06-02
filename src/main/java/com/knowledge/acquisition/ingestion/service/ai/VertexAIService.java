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
import com.knowledge.acquisition.dto.AIResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with Google Cloud Vertex AI generative models and embeddings.
 *
 * <p>This service provides a resilient client for Vertex AI with circuit breaker and retry patterns
 * to handle transient failures and rate limiting.
 *
 * <h3>Capabilities</h3>
 *
 * <ul>
 *   <li><b>Text Generation:</b> Uses generative models (Gemini) for classification, extraction, and
 *       analysis tasks
 *   <li><b>Multimodal Generation:</b> Supports PDF and image analysis with combined text/image
 *       inputs
 *   <li><b>Embeddings:</b> Generates vector embeddings for semantic search and similarity matching
 * </ul>
 *
 * <h3>Resilience Patterns</h3>
 *
 * <ul>
 *   <li><b>Retry:</b> Automatic retry with exponential backoff for transient failures (rate limits,
 *       network errors)
 *   <li><b>Circuit Breaker:</b> Fails fast when service is consistently unavailable to prevent
 *       cascading failures
 *   <li><b>Graceful Degradation:</b> Embedding failures return null, allowing ingestion to continue
 *       without embeddings
 * </ul>
 *
 * <h3>Configuration</h3>
 *
 * <ul>
 *   <li><b>vertex.project-id:</b> GCP project ID
 *   <li><b>vertex.location:</b> Vertex AI location (e.g., us-central1, global)
 *   <li><b>vertex.classification-model-name:</b> Model for generation tasks (e.g.,
 *       gemini-1.5-flash-002)
 *   <li><b>vertex.embedding-model-name:</b> Model for embeddings (e.g., text-embedding-005)
 *   <li><b>vertex.retry.max-attempts:</b> Maximum retry attempts (default: 4)
 *   <li><b>vertex.retry.initial-backoff-ms:</b> Initial backoff milliseconds (default: 2000)
 * </ul>
 *
 * <h3>Token Usage Tracking</h3>
 *
 * All API calls track token usage:
 *
 * <ul>
 *   <li>Prompt tokens (input)
 *   <li>Response tokens (output)
 *   <li>Total tokens
 * </ul>
 *
 * This enables cost tracking and optimization.
 *
 * @see AIResponse
 * @see EmbeddingService
 */
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

  /**
   * Initializes Vertex AI clients after bean construction.
   *
   * <p>Creates:
   *
   * <ul>
   *   <li>VertexAI client for the configured project and location
   *   <li>GenerativeModel for text/multimodal generation
   *   <li>PredictionServiceClient for embedding generation
   * </ul>
   *
   * @throws Exception if client initialization fails
   */
  @PostConstruct
  public void init() throws Exception {
    this.vertexAI = new VertexAI(projectId, location);
    this.classificationModel = new GenerativeModel(classificationModelName, vertexAI);
    this.predictionServiceClient =
        PredictionServiceClient.create(
            PredictionServiceSettings.newBuilder().setEndpoint(apiEndpoint()).build());
  }

  /**
   * Closes prediction service client on bean destruction.
   *
   * <p>Releases gRPC connections and resources gracefully during application shutdown.
   */
  @PreDestroy
  public void close() {
    if (predictionServiceClient != null) {
      predictionServiceClient.close();
    }
  }

  /**
   * Generates content using a text prompt with Vertex AI generative model.
   *
   * <p>This method is used for:
   *
   * <ul>
   *   <li>Document classification (domain, subdomain, capabilities)
   *   <li>Entity extraction (workflows, APIs, business rules)
   *   <li>Knowledge consolidation
   *   <li>Planning artifact generation
   * </ul>
   *
   * <h3>Resilience</h3>
   *
   * <ul>
   *   <li>Automatically retries on transient failures (rate limits, network errors)
   *   <li>Circuit breaker opens after repeated failures to prevent cascade
   *   <li>Tracks token usage for cost monitoring
   * </ul>
   *
   * @param prompt the text prompt for generation
   * @return {@link AIResponse} with generated text and token usage statistics
   * @throws RuntimeException if generation fails after all retries or circuit breaker is open
   */
  @CircuitBreaker(name = "vertexai", fallbackMethod = "generateFallback")
  @Retry(name = "vertexai")
  public AIResponse generate(String prompt) throws Exception {
    log.debug("Calling Vertex AI for content generation");
    GenerateContentResponse response = classificationModel.generateContent(prompt);
    String content = response.getCandidates(0).getContent().getParts(0).getText();

    // Extract token usage metadata
    long totalTokens = 0;
    long promptTokens = 0;
    long responseTokens = 0;

    if (response.hasUsageMetadata()) {
      totalTokens = response.getUsageMetadata().getTotalTokenCount();
      promptTokens = response.getUsageMetadata().getPromptTokenCount();
      responseTokens = response.getUsageMetadata().getCandidatesTokenCount();
      log.debug(
          "Token usage - Total: {}, Prompt: {}, Response: {}",
          totalTokens,
          promptTokens,
          responseTokens);
    } else {
      log.warn("No usage metadata available in Vertex AI response");
    }

    return AIResponse.of(content, totalTokens, promptTokens, responseTokens);
  }

  private AIResponse generateFallback(String prompt, CallNotPermittedException ex) {
    log.error("Circuit breaker is OPEN for Vertex AI generation. Service is unavailable.");
    throw new RuntimeException(
        "Vertex AI service is currently unavailable. Please try again later.", ex);
  }

  private AIResponse generateFallback(String prompt, Exception ex) {
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
   * @return AIResponse with the generated text and token usage
   */
  @CircuitBreaker(name = "vertexai", fallbackMethod = "generateWithImageFallback")
  @Retry(name = "vertexai")
  public AIResponse generateWithImage(String prompt, byte[] imageData, String mimeType)
      throws Exception {
    log.debug("Calling Vertex AI for multimodal content generation");
    Content content =
        ContentMaker.fromMultiModalData(prompt, PartMaker.fromMimeTypeAndData(mimeType, imageData));

    GenerateContentResponse response = classificationModel.generateContent(content);
    String generatedText = ResponseHandler.getText(response);

    // Extract token usage metadata
    long totalTokens = 0;
    long promptTokens = 0;
    long responseTokens = 0;

    if (response.hasUsageMetadata()) {
      totalTokens = response.getUsageMetadata().getTotalTokenCount();
      promptTokens = response.getUsageMetadata().getPromptTokenCount();
      responseTokens = response.getUsageMetadata().getCandidatesTokenCount();
      log.debug(
          "Multimodal token usage - Total: {}, Prompt: {}, Response: {}",
          totalTokens,
          promptTokens,
          responseTokens);
    } else {
      log.warn("No usage metadata available in Vertex AI multimodal response");
    }

    return AIResponse.of(generatedText, totalTokens, promptTokens, responseTokens);
  }

  private AIResponse generateWithImageFallback(
      String prompt, byte[] imageData, String mimeType, CallNotPermittedException ex) {
    log.error(
        "Circuit breaker is OPEN for Vertex AI multimodal generation. Service is unavailable.");
    throw new RuntimeException(
        "Vertex AI multimodal service is currently unavailable. Please try again later.", ex);
  }

  private AIResponse generateWithImageFallback(
      String prompt, byte[] imageData, String mimeType, Exception ex) {
    log.error("Failed to generate multimodal content with Vertex AI after retries", ex);
    throw new RuntimeException("Failed to generate multimodal content with Vertex AI", ex);
  }

  /**
   * Generates a vector embedding for text using Vertex AI embedding model.
   *
   * <p>Embeddings are used for:
   *
   * <ul>
   *   <li>Semantic search and similarity matching
   *   <li>Project discovery by definition
   *   <li>Chunk retrieval for RAG (Retrieval Augmented Generation)
   *   <li>Cross-document relationship detection
   * </ul>
   *
   * <h3>Graceful Degradation</h3>
   *
   * If embedding generation fails after retries or circuit breaker is open, this method returns
   * null to allow ingestion to continue without embeddings. This prevents embedding failures from
   * blocking the entire ingestion process.
   *
   * @param text the text to embed (typically document chunks, project definitions, or summaries)
   * @return list of float values representing the embedding vector, or null if generation fails
   * @throws Exception if API call fails (captured by retry/circuit breaker)
   */
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

  /**
   * Constructs the Vertex AI API endpoint based on location.
   *
   * @return the API endpoint URL
   */
  private String apiEndpoint() {
    if ("global".equals(location)) {
      return "aiplatform.googleapis.com:443";
    }
    return location + "-aiplatform.googleapis.com:443";
  }

  /**
   * Constructs the full resource name for the embedding model.
   *
   * @return the model resource name in format
   *     projects/{project}/locations/{location}/publishers/google/models/{model}
   */
  private String modelResourceName() {
    return String.format(
        "projects/%s/locations/%s/publishers/google/models/%s",
        projectId, location, embeddingModelName);
  }
}
