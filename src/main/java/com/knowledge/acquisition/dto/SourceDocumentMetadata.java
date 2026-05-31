package com.knowledge.acquisition.dto;

import java.util.UUID;

public record SourceDocumentMetadata(
    UUID projectId,
    String bucketName,
    String objectName,
    Long generation,
    String checksum,
    String contentHash,
    String documentType,
    String fileType,
    String title) {}
