package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.LeagueJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataLeagueRepository extends JpaRepository<LeagueJpaEntity, UUID> {

  Optional<LeagueJpaEntity> findByInviteCode(String inviteCode);

  @Query("SELECT t FROM LeagueJpaEntity t JOIN t.fixtures f WHERE f.matchId = :matchId")
  Optional<LeagueJpaEntity> findByMatchId(@Param("matchId") UUID matchId);

}
