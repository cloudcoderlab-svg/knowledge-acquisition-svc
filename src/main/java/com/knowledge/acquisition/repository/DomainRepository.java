package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.DomainEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {
  List<DomainEntity> findByProjectId(UUID projectId);

  Optional<DomainEntity> findByProjectIdAndDomainName(UUID projectId, String domainName);
}
