package com.knowledge.acquisition.repository;

import com.knowledge.acquisition.entity.SubdomainEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubdomainRepository extends JpaRepository<SubdomainEntity, UUID> {
  List<SubdomainEntity> findByProjectId(UUID projectId);

  Optional<SubdomainEntity> findByDomainIdAndSubdomainName(UUID domainId, String subdomainName);
}
