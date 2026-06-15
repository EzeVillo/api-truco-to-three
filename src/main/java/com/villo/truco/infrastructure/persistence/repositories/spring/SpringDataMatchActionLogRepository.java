package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.MatchActionLogJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMatchActionLogRepository extends
    JpaRepository<MatchActionLogJpaEntity, Long> {

  boolean existsByMatchIdAndStateVersion(UUID matchId, long stateVersion);

}
