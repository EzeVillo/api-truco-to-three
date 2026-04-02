package com.villo.truco.auth.infrastructure.persistence.repositories.spring;

import com.villo.truco.auth.infrastructure.persistence.entities.UserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

  Optional<UserJpaEntity> findByUsername(String username);

  boolean existsByUsername(String username);

}
