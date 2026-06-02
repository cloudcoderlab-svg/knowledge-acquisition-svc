package com.knowledge.acquisition.ingestion.util;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Utility component for loading prompt templates from the classpath.
 *
 * <p>This utility simplifies loading text-based prompt templates (typically stored in
 * src/main/resources/prompt/) for use with AI services. Prompts are loaded as UTF-8 encoded
 * strings.
 *
 * <h3>Usage Example</h3>
 *
 * <pre>
 * &#64;Autowired
 * private PromptLoaderUtils promptLoader;
 *
 * String prompt = promptLoader.load("prompt/document-level-analysis-prompt.txt");
 * </pre>
 *
 * <h3>Common Prompt Locations</h3>
 *
 * <ul>
 *   <li>prompt/document-level-analysis-prompt.txt - Document classification and analysis
 *   <li>prompt/enhanced-chunk-extraction-prompt.txt - Chunk-level entity extraction
 *   <li>prompt/tibco-mdm-extraction-prompt.txt - TIBCO MDM specific extraction
 *   <li>prompt/domain-based-planning-prompt.txt - Planning artifact generation
 * </ul>
 *
 * @see ClassPathResource
 */
@Component
public class PromptLoaderUtils {

  /**
   * Loads a prompt template from the classpath as a UTF-8 string.
   *
   * @param path the classpath resource path (e.g., "prompt/my-prompt.txt")
   * @return the prompt content as a string
   * @throws Exception if the resource cannot be found or read
   */
  public String load(String path) throws Exception {
    return new String(
        new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
