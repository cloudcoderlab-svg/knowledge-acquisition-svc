package com.knowledge.acquisition.ingestion.util;

import java.util.Collection;

public final class StringUtils {

  private StringUtils() {}

  public static String safeString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  public static String safeListToString(Collection<?> values, String fallback) {
    if (values == null || values.isEmpty()) {
      return fallback;
    }
    return String.join(", ", values.stream().map(String::valueOf).toList());
  }
}
