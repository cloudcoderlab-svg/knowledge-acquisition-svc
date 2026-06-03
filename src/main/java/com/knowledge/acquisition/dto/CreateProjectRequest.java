package com.knowledge.acquisition.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

public record CreateProjectRequest(
    @NotBlank(message = "Project name is required")
        @Size(min = 3, max = 200, message = "Project name must be between 3 and 200 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9][a-zA-Z0-9 -_]*$",
            message =
                "Project name must start with alphanumeric and contain only alphanumeric, spaces, hyphens, and underscores")
        String projectName,
    @Positive(message = "Version must be a positive number") Integer version,
    @Size(max = 500, message = "Title must not exceed 500 characters") String title,
    @Size(max = 5000, message = "Description must not exceed 5000 characters") String description,
    @Size(max = 50000, message = "Definition must not exceed 50000 characters") String definition,
    @Size(max = 255, message = "Source bucket name must not exceed 255 characters")
        @Pattern(
            regexp = "^[a-z0-9][a-z0-9-_.]*[a-z0-9]$|^$",
            message = "Invalid GCS bucket name format")
        String sourceBucket,
    @Size(max = 1000, message = "GCS prefix must not exceed 1000 characters") String gcsPrefix,
    JsonNode metadata) {}
