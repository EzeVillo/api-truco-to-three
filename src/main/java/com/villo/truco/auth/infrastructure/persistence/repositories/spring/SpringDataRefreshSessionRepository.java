package com.villo.truco.auth.infrastructure.persistence.repositories.spring;

import com.villo.truco.auth.infrastructure.persistence.entities.RefreshSessionJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRefreshSessionRepository extends
    JpaRepository<RefreshSessionJpaEntity, UUID> {

  Optional<RefreshSessionJpaEntity> findByTokenHash(String tokenHash);

  Optional<RefreshSessionJpaEntity> findByReplacedBySessionId(UUID replacedBySessionId);

}
