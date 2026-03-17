package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public final class InMemoryMatchRepository implements MatchRepository, MatchQueryRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryMatchRepository.class);

  private final Map<MatchId, Match> store = new ConcurrentHashMap<>();

  @Override
  public void save(final Match match) {

    this.store.put(match.getId(), match);
    LOGGER.debug("Match saved in memory: matchId={}, totalStored={}", match.getId(),
        this.store.size());
  }

  @Override
  public Optional<Match> findById(final MatchId id) {

    final var match = this.store.get(id);
    LOGGER.debug("Match lookup: matchId={}, found={}", id, match != null);
    return Optional.ofNullable(match);
  }

  @Override
  public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

    return this.store.values().stream().filter(match -> match.getInviteCode().equals(inviteCode))
        .findFirst();
  }

  @Override
  public boolean hasActiveMatch(final PlayerId playerId) {

    return this.store.values().stream().anyMatch(
        match -> match.getStatus() == MatchStatus.IN_PROGRESS && (
            playerId.equals(match.getPlayerOne()) || playerId.equals(match.getPlayerTwo())));
  }

}
