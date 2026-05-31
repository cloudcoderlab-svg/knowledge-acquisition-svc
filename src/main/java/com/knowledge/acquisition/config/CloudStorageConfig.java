package com.knowledge.acquisition.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudStorageConfig {
  @Bean
  Storage storage() {
    return StorageOptions.getDefaultInstance().getService();
  }
}
