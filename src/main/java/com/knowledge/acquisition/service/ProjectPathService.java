package com.knowledge.acquisition.service;

import java.text.Normalizer;
import org.springframework.stereotype.Service;

@Service
public class ProjectPathService {

  public String projectPrefix(String projectName) {
    return "projects/" + normalize(projectName) + "/";
  }

  public boolean isProjectObject(String objectName) {
    return objectName != null
        && objectName.startsWith("projects/")
        && objectName.split("/").length >= 3;
  }

  public String projectSlugFromObject(String objectName) {
    if (!isProjectObject(objectName)) {
      throw new IllegalArgumentException("Object must use projects/{projectName}/... layout");
    }
    return objectName.split("/")[1];
  }

  String normalize(String value) {
    String normalized = Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD);
    return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }
}
