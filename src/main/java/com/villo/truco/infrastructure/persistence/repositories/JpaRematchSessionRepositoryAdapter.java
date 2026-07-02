package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.ports.RematchSessionTimeoutEntry;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.mappers.RematchSessionMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataRematchSessionRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaRematchSessionRepositoryAdapter implements RematchSessionRepository {

  private final SpringDataRematchSessionRepository springDataRepo;
  private final RematchSessionMapper mapper;

  public JpaRematchSessionRepositoryAdapter(final SpringDataRematchSessionRepository springDataRepo,
      final RematchSessionMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  private static void addIfQueried(final Set<PlayerId> target, final Set<PlayerId> queried,
      final UUID candidate) {

    if (candidate == null) {
      return;
    }
    final var playerId = new PlayerId(candidate);
    if (queried.contains(playerId)) {
      target.add(playerId);
    }
  }

  @Override
  public Optional<RematchSession> findById(final RematchSessionId id) {

    return springDataRepo.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<RematchSession> findByOriginMatchId(final MatchId matchId) {

    return springDataRepo.findByOriginMatchId(matchId.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<RematchSession> findOpenByPlayer(final PlayerId playerId) {

    return springDataRepo.findOpenByPlayer(playerId.value()).map(mapper::toDomain);
  }

  @Override
  public Set<PlayerId> findPlayersWithOpenRematch(final Set<PlayerId> playerIds) {

    if (playerIds.isEmpty()) {
      return Set.of();
    }

    final var ids = playerIds.stream().map(PlayerId::value).collect(Collectors.toSet());
    final var blocked = new HashSet<PlayerId>();
    for (final var entity : this.springDataRepo.findOpenByPlayers(ids)) {
      addIfQueried(blocked, playerIds, entity.getPlayerOneId());
      addIfQueried(blocked, playerIds, entity.getPlayerTwoId());
    }
    return blocked;
  }

  @Override
  public List<RematchSession> findExpiredCandidates(final Instant now, final int batchSize) {

    return springDataRepo.findExpiredCandidates(now, PageRequest.of(0, batchSize)).stream()
        .map(mapper::toDomain).toList();
  }

  @Override
  public Stream<RematchSessionTimeoutEntry> findActiveWithExpiration() {

    return this.springDataRepo.findOpenSessionsWithExpiration().stream().map(
        row -> new RematchSessionTimeoutEntry(new RematchSessionId(row.getId()),
            row.getExpiresAt()));
  }

  @Override
  @Transactional
  public void save(final RematchSession session) {

    final var entity = mapper.toEntity(session);
    springDataRepo.saveAndFlush(entity);
    session.setVersion(entity.getVersion());
  }

}
