package com.knowledge.acquisition.dto;

import java.util.UUID;

public record SearchHit(
    UUID id,
    String resultType,
    String entityType,
    UUID entityId,
    String content,
    String metadata) {}
