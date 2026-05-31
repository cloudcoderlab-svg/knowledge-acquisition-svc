package com.knowledge.acquisition.repository;

import java.util.UUID;

public interface ProjectDiscoveryProjection {
  UUID getProjectId();

  String getProjectName();

  Integer getVersion();

  String getTitle();

  String getDescription();

  String getDefinition();

  String getSummary();

  String getSourceBucket();

  String getGcsPrefix();

  String getMetadata();

  Double getScore();
}
