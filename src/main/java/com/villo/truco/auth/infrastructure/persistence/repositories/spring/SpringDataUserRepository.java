package com.villo.truco.auth.infrastructure.persistence.repositories.spring;

import com.villo.truco.auth.infrastructure.persistence.entities.UserJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

  Optional<UserJpaEntity> findByUsernameIgnoreCase(String username);

  List<UserUsernameProjection> findByIdIn(Collection<UUID> ids);

  boolean existsByUsernameIgnoreCase(String username);

}
