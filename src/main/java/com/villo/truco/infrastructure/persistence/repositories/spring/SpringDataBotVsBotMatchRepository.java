package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.BotVsBotMatchJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataBotVsBotMatchRepository extends
    JpaRepository<BotVsBotMatchJpaEntity, UUID> {

  @Query("SELECT b.matchId FROM BotVsBotMatchJpaEntity b, MatchJpaEntity m "
      + "WHERE m.id = b.matchId AND b.ownerId = :ownerId "
      + "AND m.status NOT IN ('FINISHED', 'CANCELLED')")
  List<UUID> findActiveOwnedMatchIds(@Param("ownerId") UUID ownerId);

  @Query("SELECT DISTINCT b.ownerId FROM BotVsBotMatchJpaEntity b, MatchJpaEntity m "
      + "WHERE m.id = b.matchId AND b.ownerId IN :ownerIds "
      + "AND m.status NOT IN ('FINISHED', 'CANCELLED')")
  List<UUID> findOwnersWithActiveMatch(@Param("ownerIds") Collection<UUID> ownerIds);

}
