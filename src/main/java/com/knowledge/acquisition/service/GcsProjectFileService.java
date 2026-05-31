package com.knowledge.acquisition.service;

import com.google.cloud.storage.*;
import com.knowledge.acquisition.dto.SignedUrlRequest;
import com.knowledge.acquisition.dto.SignedUrlResponse;
import com.knowledge.acquisition.entity.ProjectEntity;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GcsProjectFileService {
  private final Storage storage;
  private final ProjectService projectService;

  @Value("${gcp.storage.bucket-name}")
  private String defaultBucket;

  public SignedUrlResponse uploadUrl(java.util.UUID projectId, SignedUrlRequest request) {
    ProjectEntity project = projectService.find(projectId);
    String bucket = project.getSourceBucket() == null ? defaultBucket : project.getSourceBucket();
    int minutes = request.expirationMinutes() == null ? 30 : request.expirationMinutes();
    String objectName = project.getGcsPrefix() + request.fileName();
    BlobInfo blobInfo =
        BlobInfo.newBuilder(bucket, objectName).setContentType(request.contentType()).build();
    URL url =
        storage.signUrl(
            blobInfo,
            minutes,
            TimeUnit.MINUTES,
            Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
            Storage.SignUrlOption.withV4Signature());
    return new SignedUrlResponse(
        url.toString(), bucket, objectName, OffsetDateTime.now().plusMinutes(minutes));
  }

  public List<String> listFiles(java.util.UUID projectId) {
    ProjectEntity project = projectService.find(projectId);
    String bucket = project.getSourceBucket() == null ? defaultBucket : project.getSourceBucket();
    return storage
        .list(bucket, Storage.BlobListOption.prefix(project.getGcsPrefix()))
        .streamAll()
        .filter(blob -> !blob.isDirectory())
        .map(BlobInfo::getName)
        .toList();
  }
}
