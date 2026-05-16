package com.villo.truco.profile.infrastructure.persistence.repositories.spring;

import com.villo.truco.profile.infrastructure.persistence.entities.PlayerStatsJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPlayerStatsRepository extends JpaRepository<PlayerStatsJpaEntity, UUID> {

}
