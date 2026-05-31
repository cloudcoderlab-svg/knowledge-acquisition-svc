package com.knowledge.acquisition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProjectPathServiceTest {
  private final ProjectPathService service = new ProjectPathService();

  @Test
  void createsProjectObjectPrefix() {
    assertThat(service.projectPrefix("Customer Onboarding Platform"))
        .isEqualTo("projects/customer-onboarding-platform/");
  }

  @Test
  void parsesProjectSlugFromObjectPath() {
    assertThat(service.projectSlugFromObject("projects/customer-onboarding-platform/docs/spec.pdf"))
        .isEqualTo("customer-onboarding-platform");
  }

  @Test
  void rejectsNonProjectObjectPath() {
    assertThatThrownBy(() -> service.projectSlugFromObject("legacy/spec.pdf"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
