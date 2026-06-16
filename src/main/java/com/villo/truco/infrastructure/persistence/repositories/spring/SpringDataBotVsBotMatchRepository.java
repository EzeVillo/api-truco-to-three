package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.BotVsBotMatchJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataBotVsBotMatchRepository extends
    JpaRepository<BotVsBotMatchJpaEntity, UUID> {

  @Query(value = "SELECT b.match_id FROM bot_vs_bot_matches b "
      + "JOIN matches m ON m.id = b.match_id "
      + "WHERE b.owner_id = :ownerId AND m.status NOT IN ('FINISHED', 'CANCELLED') "
      + "LIMIT 1", nativeQuery = true)
  Optional<UUID> findActiveOwnedMatchId(@Param("ownerId") UUID ownerId);

}
