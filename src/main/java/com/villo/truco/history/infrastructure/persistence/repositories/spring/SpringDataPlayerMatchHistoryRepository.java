package com.villo.truco.history.infrastructure.persistence.repositories.spring;

import com.villo.truco.history.infrastructure.persistence.entities.PlayerMatchHistoryJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPlayerMatchHistoryRepository extends
    JpaRepository<PlayerMatchHistoryJpaEntity, UUID> {

}
