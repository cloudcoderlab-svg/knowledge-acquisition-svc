package com.knowledge.acquisition.ingestion.util;

public final class JsonResponseUtils {

  private JsonResponseUtils() {}

  public static String object(String response) {
    return extract(response, '{', '}');
  }

  public static String array(String response) {
    return extract(response, '[', ']');
  }

  private static String extract(String response, char open, char close) {
    if (response == null || response.isBlank()) {
      throw new IllegalArgumentException("Response is empty");
    }

    int start = response.indexOf(open);
    int end = response.lastIndexOf(close);
    if (start < 0 || end <= start) {
      throw new IllegalArgumentException("Response does not contain JSON: " + response);
    }

    return response.substring(start, end + 1);
  }
}
