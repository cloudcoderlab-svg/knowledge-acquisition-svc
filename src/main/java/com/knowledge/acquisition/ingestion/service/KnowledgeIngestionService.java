package com.knowledge.acquisition.ingestion.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.knowledge.acquisition.dto.*;
import com.knowledge.acquisition.entity.ProjectEntity;
import com.knowledge.acquisition.ingestion.parser.DocumentParserOrchestrator;
import com.knowledge.acquisition.ingestion.service.ai.DocumentChunker;
import com.knowledge.acquisition.ingestion.service.ai.EmbeddingService;
import com.knowledge.acquisition.ingestion.service.ai.SemanticClassificationService;
import com.knowledge.acquisition.repository.ProjectRepository;
import com.knowledge.acquisition.service.ProjectPathService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates ingestion of a single project artifact from GCS.
 *
 * <p>The service keeps two views of XML-like files: parsed text from Tika for chunking and LLM
 * prompts, and decoded raw XML for deterministic XML extraction and platform detection. This avoids
 * losing structural cues such as {@code <Rulebase>} when Tika normalizes XML into text.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionService {
  private static final Pattern XML_ENCODING_PATTERN =
      Pattern.compile(
          "<\\?xml\\s+[^>]*encoding\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
  private static final long MAX_DOCUMENT_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

  private final DocumentParserOrchestrator documentParserOrchestrator;
  private final DocumentLevelAnalysisService documentLevelAnalysisService;
  private final EnhancedKnowledgeExtractionService enhancedKnowledgeExtractionService;
  private final DeterministicXmlExtractionService deterministicXmlExtractionService;
  private final KnowledgeGraphService knowledgeGraphService;
  private final SemanticClassificationService classificationService;
  private final EmbeddingService embeddingService;
  private final PostgresStorageService storageService;
  private final DocumentChunker documentChunker;
  private final Storage storage;
  private final ProjectPathService projectPathService;
  private final ProjectRepository projectRepository;

  /**
   * Reads a GCS object, parses it into document content, stores document tracking rows, and
   * extracts/stores knowledge graph artifacts.
   *
   * @param bucketName GCS bucket containing the object
   * @param objectName GCS object path under the project prefix
   * @return the first chunk classification enriched with source document id
   * @throws Exception when the document cannot be read, parsed, chunked, or persisted
   */
  public ClassificationResult ingestFromGcs(String bucketName, String objectName) throws Exception {
    log.info("Ingesting project file from GCS: gs://{}/{}", bucketName, objectName);
    ProjectEntity project = projectFromObjectName(objectName);
    Blob blob = storage.get(BlobId.of(bucketName, objectName));
    if (blob == null) {
      throw new IllegalArgumentException("Blob not found: " + objectName);
    }

    // Validate file size before loading into memory
    Long blobSize = blob.getSize();
    if (blobSize != null && blobSize > MAX_DOCUMENT_SIZE_BYTES) {
      throw new IllegalArgumentException(
          String.format(
              "Document size (%d bytes) exceeds maximum allowed size (%d bytes)",
              blobSize, MAX_DOCUMENT_SIZE_BYTES));
    }

    // Validate object name to prevent path traversal
    validateObjectName(objectName);

    byte[] documentBytes = blob.getContent();
    try (InputStream inputStream = new ByteArrayInputStream(documentBytes)) {
      DocumentContent documentContent =
          documentParserOrchestrator.parseDocument(inputStream, fileType(objectName));
      if (documentContent.getTextContent() == null
          || documentContent.getTextContent().trim().isEmpty()) {
        throw new IllegalArgumentException("Content is empty");
      }

      SourceDocumentMetadata source =
          sourceMetadata(
              project.getProjectId(),
              bucketName,
              objectName,
              blob,
              documentContent.getTextContent());
      return processContent(
          project, source, documentContent, decodeRawText(documentBytes, fileType(objectName)));
    }
  }

  private ClassificationResult processContent(
      ProjectEntity project,
      SourceDocumentMetadata source,
      DocumentContent documentContent,
      String rawContent)
      throws Exception {
    PostgresStorageService.StartedDocument startedDocument = storageService.startDocument(source);
    try {
      DocumentKnowledge docKnowledge =
          documentLevelAnalysisService.analyze(documentContent, source, rawContent);
      KnowledgeExtractionResult deterministicKnowledge =
          isXmlLike(source.fileType())
              ? deterministicXmlExtractionService.extract(
                  firstNonBlank(rawContent, documentContent.getTextContent()), source.objectName())
              : new KnowledgeExtractionResult();
      List<SemanticChunk> chunks =
          project.getDefinition() == null || project.getDefinition().isBlank()
              ? documentChunker.chunk(
                  documentContent.getTextContent(), null, platformContext(docKnowledge))
              : documentChunker.chunk(
                  documentContent.getTextContent(),
                  project.getDefinition(),
                  platformContext(docKnowledge));
      if (chunks.isEmpty()) {
        throw new IllegalArgumentException("Content is empty after chunking");
      }

      List<KnowledgeExtractionResult> allChunkKnowledge = new ArrayList<>();
      for (SemanticChunk chunk : chunks) {
        String content = chunk.getContent();
        chunk.setClassification(classificationService.classify(content));
        chunk.setEmbedding(safeEmbedding(content, source.objectName()));
        KnowledgeExtractionResult chunkKnowledge =
            enhancedKnowledgeExtractionService.extract(content, docKnowledge);
        chunk.setKnowledgeExtraction(chunkKnowledge);
        allChunkKnowledge.add(chunkKnowledge);
      }

      storageService.saveSourceChunks(
          source, startedDocument.sourceDocumentId(), startedDocument.documentId(), chunks);
      allChunkKnowledge.add(0, deterministicKnowledge);
      knowledgeGraphService.buildKnowledgeGraph(
          startedDocument.sourceDocumentId(), source, docKnowledge, allChunkKnowledge);

      ClassificationResult result = chunks.get(0).getClassification();
      result.setSourceDocumentId(startedDocument.sourceDocumentId());
      return result;
    } catch (Exception e) {
      storageService.failDocument(startedDocument.documentId(), e);
      throw e;
    }
  }

  private ProjectEntity projectFromObjectName(String objectName) {
    String slug = projectPathService.projectSlugFromObject(objectName);
    String prefix = "projects/" + slug + "/";
    return projectRepository
        .findByGcsPrefix(prefix)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Project not found for object prefix: "
                        + prefix
                        + ". Create the project before ingestion."));
  }

  private SourceDocumentMetadata sourceMetadata(
      UUID projectId, String bucketName, String objectName, Blob blob, String content) {
    String checksum = firstNonBlank(blob.getCrc32c(), blob.getMd5(), blob.getEtag());
    return new SourceDocumentMetadata(
        projectId,
        bucketName,
        objectName,
        blob.getGeneration(),
        checksum,
        documentChunker.contentHash(content),
        documentType(objectName),
        fileType(objectName),
        title(objectName));
  }

  private String documentType(String objectName) {
    String lowerName = objectName.toLowerCase();
    if (lowerName.endsWith(".xml")) {
      return "xml_doc";
    }
    if (lowerName.contains("diagram") || lowerName.contains("architecture")) {
      return "architecture_diagram";
    }
    if (lowerName.contains("requirement")) {
      return "requirements_doc";
    }
    if (lowerName.contains("estimate") || lowerName.contains("cost")) {
      return "estimate_sheet";
    }
    if (lowerName.endsWith(".md") || lowerName.endsWith(".txt")) {
      return "text_doc";
    }
    return "document";
  }

  private String fileType(String objectName) {
    int lastDot = objectName.lastIndexOf('.');
    return lastDot < 0 || lastDot == objectName.length() - 1
        ? "unknown"
        : objectName.substring(lastDot + 1).toLowerCase();
  }

  private String title(String objectName) {
    int separator = objectName.lastIndexOf('/');
    return separator < 0 ? objectName : objectName.substring(separator + 1);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  /**
   * Decodes raw document bytes for use in detection paths that need original XML structure.
   *
   * <p>XML-like files are decoded using BOMs, UTF-16 byte patterns, or the XML declaration {@code
   * encoding="..."} before falling back to UTF-8. Other text-like files currently use UTF-8 because
   * their parsed text from Tika remains the canonical content for chunking.
   *
   * @param documentBytes raw object bytes from storage
   * @param fileType file extension without dot
   * @return decoded text, or {@code null} when bytes are null
   */
  static String decodeRawText(byte[] documentBytes, String fileType) {
    if (documentBytes == null) {
      return null;
    }
    Charset charset =
        isXmlLikeType(fileType) ? detectXmlCharset(documentBytes) : StandardCharsets.UTF_8;
    return new String(documentBytes, charset);
  }

  /**
   * Determines the character set declared by XML bytes.
   *
   * <p>The XML declaration is probed as ISO-8859-1 because the declaration itself is
   * ASCII-compatible for common single-byte encodings. UTF-16 XML without a BOM is detected from
   * the byte pattern of the {@code <?xml} prefix.
   */
  private static Charset detectXmlCharset(byte[] documentBytes) {
    if (startsWith(documentBytes, 0xEF, 0xBB, 0xBF)) {
      return StandardCharsets.UTF_8;
    }
    if (startsWith(documentBytes, 0xFE, 0xFF)
        || startsWith(documentBytes, 0x00, 0x3C, 0x00, 0x3F)) {
      return StandardCharsets.UTF_16BE;
    }
    if (startsWith(documentBytes, 0xFF, 0xFE)
        || startsWith(documentBytes, 0x3C, 0x00, 0x3F, 0x00)) {
      return StandardCharsets.UTF_16LE;
    }

    int probeLength = Math.min(documentBytes.length, 512);
    String probe = new String(documentBytes, 0, probeLength, StandardCharsets.ISO_8859_1);
    Matcher matcher = XML_ENCODING_PATTERN.matcher(probe);
    if (matcher.find()) {
      String encoding = matcher.group(1);
      try {
        return Charset.forName(encoding);
      } catch (Exception e) {
        log.warn("Unsupported XML encoding '{}', falling back to UTF-8", encoding);
      }
    }
    return StandardCharsets.UTF_8;
  }

  /** Returns true when {@code bytes} begins with the unsigned byte prefix. */
  private static boolean startsWith(byte[] bytes, int... prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if ((bytes[i] & 0xFF) != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Generates an embedding for a source chunk without making embedding availability a hard
   * ingestion dependency.
   *
   * <p>Quota exhaustion and transient embedding failures should not prevent text, facts, rules, and
   * relationships from being persisted. The shared {@link EmbeddingService} performs quota
   * cooldown; this method handles any remaining embedding exception by returning {@code null}.
   */
  private List<Double> safeEmbedding(String content, String objectName) {
    try {
      return embeddingService.embedding(content);
    } catch (Exception e) {
      log.warn(
          "Skipping chunk embedding for {} because embedding service failed: {}",
          objectName,
          e.getMessage());
      return null;
    }
  }

  private String platformContext(DocumentKnowledge docKnowledge) {
    if (docKnowledge == null || docKnowledge.getDetectedPlatform() == null) {
      return null;
    }
    return "Detected XML/platform type: " + docKnowledge.getDetectedPlatform();
  }

  private boolean isXmlLike(String fileType) {
    return isXmlLikeType(fileType);
  }

  private static boolean isXmlLikeType(String fileType) {
    return "xml".equalsIgnoreCase(fileType)
        || "xpdl".equalsIgnoreCase(fileType)
        || "bpmn".equalsIgnoreCase(fileType)
        || "bpel".equalsIgnoreCase(fileType);
  }

  /**
   * Validates object name to prevent path traversal attacks.
   *
   * @param objectName the GCS object name to validate
   * @throws IllegalArgumentException if object name contains path traversal attempts
   */
  private void validateObjectName(String objectName) {
    if (objectName == null || objectName.isBlank()) {
      throw new IllegalArgumentException("Object name cannot be null or blank");
    }
    if (objectName.contains("..") || objectName.contains("//")) {
      throw new IllegalArgumentException("Object name contains invalid path sequences");
    }
    if (!objectName.startsWith("projects/")) {
      throw new IllegalArgumentException("Object name must start with 'projects/' prefix");
    }
  }
}
