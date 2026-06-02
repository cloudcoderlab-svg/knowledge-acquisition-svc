package com.knowledge.acquisition.ingestion.service.ai;

import com.knowledge.acquisition.dto.SemanticChunk;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for chunking documents into semantic chunks with configurable size and overlap.
 *
 * <p>This service implements an intelligent chunking strategy that:
 *
 * <ul>
 *   <li>Splits documents at natural boundaries (paragraphs, sentences, words)
 *   <li>Maintains configurable overlap between chunks for context continuity
 *   <li>Generates unique content hashes (SHA-256) for each chunk
 *   <li>Supports context injection (project and platform context) for enhanced semantic
 *       understanding
 *   <li>Preserves character positions for traceability to source content
 * </ul>
 *
 * <h3>Chunking Strategy</h3>
 *
 * The chunker attempts to split at natural boundaries in this order of preference:
 *
 * <ol>
 *   <li>Paragraph boundary (double newline "\n\n")
 *   <li>Sentence boundary (period followed by space ". " or newline "\n")
 *   <li>Word boundary (space character)
 *   <li>Hard character limit (if no natural boundary found)
 * </ol>
 *
 * <h3>Overlap Strategy</h3>
 *
 * Overlap ensures that semantic context is preserved across chunk boundaries. For example, with
 * maxChunkChars=3000 and overlapChars=300:
 *
 * <pre>
 * Chunk 1: chars 0-3000
 * Chunk 2: chars 2700-5700 (300 chars overlap from chunk 1)
 * Chunk 3: chars 5400-8400 (300 chars overlap from chunk 2)
 * </pre>
 *
 * <h3>Context Injection</h3>
 *
 * When project or platform context is provided, it is prepended to the document content to enhance
 * semantic understanding during embedding and retrieval:
 *
 * <pre>
 * === PROJECT CONTEXT ===
 * [Project definition, domain, technology stack]
 *
 * === PLATFORM CONTEXT ===
 * [Platform-specific information]
 *
 * === DOCUMENT CONTENT ===
 * [Actual document content]
 * </pre>
 *
 * <h3>Configuration</h3>
 *
 * <ul>
 *   <li><b>knowledge-engine.chunk.max-chars:</b> Maximum characters per chunk (default: 3000)
 *   <li><b>knowledge-engine.chunk.overlap-chars:</b> Overlap characters between chunks (default:
 *       300)
 * </ul>
 *
 * @see SemanticChunk
 */
@Service
public class DocumentChunker {

  private final int maxChunkChars;
  private final int overlapChars;

  /**
   * Constructs a DocumentChunker with configurable chunk size and overlap.
   *
   * @param maxChunkChars maximum characters per chunk (must be positive)
   * @param overlapChars overlap characters between consecutive chunks (must be non-negative and
   *     less than maxChunkChars)
   * @throws IllegalArgumentException if maxChunkChars is not positive, or if overlapChars is
   *     negative or >= maxChunkChars
   */
  public DocumentChunker(
      @Value("${knowledge-engine.chunk.max-chars:3000}") int maxChunkChars,
      @Value("${knowledge-engine.chunk.overlap-chars:300}") int overlapChars) {
    if (maxChunkChars <= 0) {
      throw new IllegalArgumentException("maxChunkChars must be positive");
    }
    if (overlapChars < 0 || overlapChars >= maxChunkChars) {
      throw new IllegalArgumentException(
          "overlapChars must be non-negative and smaller than maxChunkChars");
    }
    this.maxChunkChars = maxChunkChars;
    this.overlapChars = overlapChars;
  }

  /**
   * Chunks document content into semantic chunks with natural boundary detection and overlap.
   *
   * <p>The chunking process:
   *
   * <ol>
   *   <li>Normalizes content (strips leading/trailing whitespace)
   *   <li>Splits content at natural boundaries (paragraphs → sentences → words)
   *   <li>Creates overlapping chunks for context continuity
   *   <li>Generates SHA-256 content hash for each chunk
   *   <li>Tracks character positions (start/end) for traceability
   * </ol>
   *
   * @param content the document content to chunk (may be empty or whitespace-only)
   * @return list of semantic chunks with metadata (empty list if content is blank)
   */
  public List<SemanticChunk> chunk(String content) {
    String normalized = content.strip();
    if (normalized.isEmpty()) {
      return List.of();
    }

    List<Range> ranges = ranges(normalized);
    List<SemanticChunk> chunks = new ArrayList<>();
    int total = ranges.size();
    for (int i = 0; i < total; i++) {
      Range range = ranges.get(i);
      String chunkContent = normalized.substring(range.start(), range.end()).strip();
      chunks.add(
          SemanticChunk.builder()
              .chunkIndex(i)
              .totalChunks(total)
              .charStart(range.start())
              .charEnd(range.end())
              .content(chunkContent)
              .contentHash(sha256(chunkContent))
              .build());
    }
    return chunks;
  }

  /**
   * Chunks content with project context prepended for enhanced semantic understanding.
   *
   * <p>The project context (from definition.md) is prepended to the document content before
   * chunking, allowing embeddings and retrieval to leverage domain-specific context.
   *
   * @param content the document content to chunk
   * @param projectContext the project context from the project definition (may be null/empty)
   * @return list of semantic chunks with project context prepended
   */
  public List<SemanticChunk> chunk(String content, String projectContext) {
    return chunk(content, projectContext, null);
  }

  /**
   * Chunks content with project and platform context prepended.
   *
   * <p>This method prepends both project-specific context (domain, technology stack, business
   * context) and platform-specific context (common patterns, shared services) to enhance semantic
   * understanding during embedding and retrieval.
   *
   * <p>Context prepending format:
   *
   * <pre>
   * === PROJECT CONTEXT ===
   * [projectContext content]
   *
   * === PLATFORM CONTEXT ===
   * [platformContext content]
   *
   * === DOCUMENT CONTENT ===
   * [content]
   * </pre>
   *
   * @param content the document content to chunk
   * @param projectContext the project-specific context (may be null/empty)
   * @param platformContext the platform-wide context (may be null/empty)
   * @return list of semantic chunks with context prepended
   */
  public List<SemanticChunk> chunk(String content, String projectContext, String platformContext) {
    if (projectContext == null || projectContext.isEmpty()) {
      if (platformContext == null || platformContext.isEmpty()) {
        return chunk(content);
      }
      String contextualContent =
          "=== PLATFORM CONTEXT ===\n"
              + platformContext
              + "\n\n=== DOCUMENT CONTENT ===\n"
              + content;
      return chunk(contextualContent);
    }

    if (platformContext == null || platformContext.isEmpty()) {
      String contextualContent =
          "=== PROJECT CONTEXT ===\n" + projectContext + "\n\n=== DOCUMENT CONTENT ===\n" + content;
      return chunk(contextualContent);
    }

    String contextualContent =
        "=== PROJECT CONTEXT ===\n"
            + projectContext
            + "\n\n=== PLATFORM CONTEXT ===\n"
            + platformContext
            + "\n\n=== DOCUMENT CONTENT ===\n"
            + content;

    return chunk(contextualContent);
  }

  /**
   * Computes a SHA-256 hash of the content for deduplication and change detection.
   *
   * <p>The content is stripped of leading/trailing whitespace before hashing to ensure consistent
   * hashes regardless of formatting differences.
   *
   * @param content the content to hash
   * @return hexadecimal SHA-256 hash string
   */
  public String contentHash(String content) {
    return sha256(content.strip());
  }

  /**
   * Computes character ranges for chunking with overlap.
   *
   * <p>This method implements the core chunking algorithm:
   *
   * <ol>
   *   <li>Starts at position 0
   *   <li>Finds the next chunk boundary (up to maxChunkChars ahead)
   *   <li>Attempts to split at natural boundary (paragraph → sentence → word)
   *   <li>Creates overlap by backing up overlapChars from the end position
   *   <li>Skips whitespace at the start of the next chunk
   *   <li>Repeats until all content is chunked
   * </ol>
   *
   * @param content the normalized content to chunk
   * @return list of character ranges representing chunk boundaries
   */
  private List<Range> ranges(String content) {
    List<Range> ranges = new ArrayList<>();
    int start = 0;
    while (start < content.length()) {
      // Determine hard end position (max chunk size or end of content)
      int hardEnd = Math.min(start + maxChunkChars, content.length());

      // Try to find natural boundary before hard end
      int end = boundary(content, start, hardEnd);

      // If boundary is too close to start or would create chunk smaller than overlap, use hard end
      if (end <= start || end - start <= overlapChars) {
        end = hardEnd;
      }

      ranges.add(new Range(start, end));

      // Stop if we've reached the end of content
      if (end == content.length()) {
        break;
      }

      // Calculate next start position with overlap
      int nextStart = Math.max(0, end - overlapChars);
      start = nextStart > start ? nextStart : end;

      // Skip leading whitespace in next chunk
      while (start < content.length() && Character.isWhitespace(content.charAt(start))) {
        start++;
      }
    }
    return ranges;
  }

  /**
   * Finds the best natural boundary for splitting content.
   *
   * <p>Attempts to split at boundaries in order of preference:
   *
   * <ol>
   *   <li>Paragraph boundary (double newline "\n\n")
   *   <li>Sentence boundary (". " or "\n")
   *   <li>Word boundary (space " ")
   *   <li>Hard character limit (if no natural boundary found)
   * </ol>
   *
   * @param content the content to analyze
   * @param start the start position of the current chunk
   * @param hardEnd the maximum allowed end position
   * @return the best boundary position before or at hardEnd
   */
  private int boundary(String content, int start, int hardEnd) {
    // If at end of content, no need to find boundary
    if (hardEnd == content.length()) {
      return hardEnd;
    }

    // Preference 1: Split at paragraph boundary (double newline)
    int paragraph = content.lastIndexOf("\n\n", hardEnd);
    if (paragraph > start) {
      return paragraph;
    }

    // Preference 2: Split at sentence boundary (period+space or newline)
    int sentence = Math.max(content.lastIndexOf(". ", hardEnd), content.lastIndexOf("\n", hardEnd));
    if (sentence > start) {
      return sentence + 1; // Include the period or newline
    }

    // Preference 3: Split at word boundary (space)
    int space = content.lastIndexOf(' ', hardEnd);
    return space > start ? space : hardEnd;
  }

  /**
   * Computes SHA-256 hash of the given value.
   *
   * @param value the string to hash
   * @return hexadecimal representation of the SHA-256 hash
   * @throws IllegalStateException if SHA-256 algorithm is not available
   */
  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to compute SHA-256 hash", e);
    }
  }

  /**
   * Represents a character range within content.
   *
   * @param start the starting character index (inclusive)
   * @param end the ending character index (exclusive)
   */
  private record Range(int start, int end) {}
}
