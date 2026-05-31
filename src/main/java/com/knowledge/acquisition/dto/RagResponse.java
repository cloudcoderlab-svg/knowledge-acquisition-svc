package com.knowledge.acquisition.dto;

import java.util.List;
import java.util.UUID;

public record RagResponse(UUID projectId, String query, String answer, List<SearchHit> evidence) {}
