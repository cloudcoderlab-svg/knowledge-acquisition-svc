package com.knowledge.acquisition.dto;

import java.util.List;

public record ProjectStatusSummaryResponse(
    String projectName, Integer totalVersions, List<ProjectVersionStatusResponse> versions) {}
