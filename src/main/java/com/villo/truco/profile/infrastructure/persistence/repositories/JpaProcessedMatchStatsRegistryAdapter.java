package com.villo.truco.profile.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.ports.ProcessedMatchStatsRegistry;
import com.villo.truco.profile.infrastructure.persistence.entities.ProcessedMatchStatsId;
import com.villo.truco.profile.infrastructure.persistence.entities.ProcessedMatchStatsJpaEntity;
import com.villo.truco.profile.infrastructure.persistence.repositories.spring.SpringDataProcessedMatchStatsRepository;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaProcessedMatchStatsRegistryAdapter implements ProcessedMatchStatsRegistry {

  private final SpringDataProcessedMatchStatsRepository springDataProcessedMatchStatsRepository;

  public JpaProcessedMatchStatsRegistryAdapter(
      final SpringDataProcessedMatchStatsRepository springDataProcessedMatchStatsRepository) {

    this.springDataProcessedMatchStatsRepository = springDataProcessedMatchStatsRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean tryRegister(final PlayerId playerId, final MatchId matchId) {

    final var id = new ProcessedMatchStatsId(playerId.value(), matchId.value());
    if (this.springDataProcessedMatchStatsRepository.existsById(id)) {
      return false;
    }
    final var entity = new ProcessedMatchStatsJpaEntity();
    entity.setPlayerId(playerId.value());
    entity.setMatchId(matchId.value());
    entity.setProcessedAt(Instant.now());
    this.springDataProcessedMatchStatsRepository.saveAndFlush(entity);
    return true;
  }

}
