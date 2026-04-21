package com.villo.truco.profile.infrastructure.persistence.repositories.spring;

import com.villo.truco.profile.infrastructure.persistence.entities.PlayerProfileJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPlayerProfileRepository extends JpaRepository<PlayerProfileJpaEntity, UUID> {

}
