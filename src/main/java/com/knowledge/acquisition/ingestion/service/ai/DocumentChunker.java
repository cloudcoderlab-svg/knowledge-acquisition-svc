package com.knowledge.acquisition.ingestion.service.ai;

import com.knowledge.acquisition.dto.SemanticChunk;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentChunker {

  private final int maxChunkChars;
  private final int overlapChars;

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
   * @param content The document content to chunk
   * @param projectContext The project context from the project definition
   * @return List of source chunks with context
   */
  public List<SemanticChunk> chunk(String content, String projectContext) {
    return chunk(content, projectContext, null);
  }

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

  public String contentHash(String content) {
    return sha256(content.strip());
  }

  private List<Range> ranges(String content) {
    List<Range> ranges = new ArrayList<>();
    int start = 0;
    while (start < content.length()) {
      int hardEnd = Math.min(start + maxChunkChars, content.length());
      int end = boundary(content, start, hardEnd);
      if (end <= start || end - start <= overlapChars) {
        end = hardEnd;
      }
      ranges.add(new Range(start, end));
      if (end == content.length()) {
        break;
      }
      int nextStart = Math.max(0, end - overlapChars);
      start = nextStart > start ? nextStart : end;
      while (start < content.length() && Character.isWhitespace(content.charAt(start))) {
        start++;
      }
    }
    return ranges;
  }

  private int boundary(String content, int start, int hardEnd) {
    if (hardEnd == content.length()) {
      return hardEnd;
    }

    int paragraph = content.lastIndexOf("\n\n", hardEnd);
    if (paragraph > start) {
      return paragraph;
    }

    int sentence = Math.max(content.lastIndexOf(". ", hardEnd), content.lastIndexOf("\n", hardEnd));
    if (sentence > start) {
      return sentence + 1;
    }

    int space = content.lastIndexOf(' ', hardEnd);
    return space > start ? space : hardEnd;
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to compute SHA-256 hash", e);
    }
  }

  private record Range(int start, int end) {}
}
