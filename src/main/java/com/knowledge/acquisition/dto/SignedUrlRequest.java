package com.knowledge.acquisition.dto;

import jakarta.validation.constraints.*;

public record SignedUrlRequest(
    @NotBlank(message = "File name is required")
        @Size(min = 1, max = 1024, message = "File name must be between 1 and 1024 characters")
        @Pattern(
            regexp = "^[^<>:\"|?*\\x00-\\x1F]+$",
            message = "File name contains invalid characters")
        String fileName,
    @Size(max = 255, message = "Content type must not exceed 255 characters") String contentType,
    @Min(value = 1, message = "Expiration must be at least 1 minute")
        @Max(value = 10080, message = "Expiration must not exceed 7 days (10080 minutes)")
        Integer expirationMinutes) {}
