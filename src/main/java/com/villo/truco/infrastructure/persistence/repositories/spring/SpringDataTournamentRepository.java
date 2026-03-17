package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.TournamentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTournamentRepository extends JpaRepository<TournamentJpaEntity, UUID> {

  Optional<TournamentJpaEntity> findByInviteCode(String inviteCode);

}
