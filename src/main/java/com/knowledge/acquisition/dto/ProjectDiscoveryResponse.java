package com.knowledge.acquisition.dto;

import java.util.List;
import java.util.UUID;

public record ProjectDiscoveryResponse(String task, List<ProjectMatch> projects) {

  public record ProjectMatch(
      UUID projectId,
      String projectName,
      Integer version,
      String title,
      String description,
      String definition,
      String summary,
      String sourceBucket,
      String gcsPrefix,
      Double score,
      String matchReason) {}
}
