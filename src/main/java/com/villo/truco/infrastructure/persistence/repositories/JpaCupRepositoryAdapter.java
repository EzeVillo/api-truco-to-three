package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.CupTimeoutEntry;
import com.villo.truco.domain.ports.JoinCodeRegistryRepository;
import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.CupJpaEntity;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.persistence.mappers.CupMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataCupRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaCupRepositoryAdapter implements CupRepository, CupQueryRepository {

  private final SpringDataCupRepository springDataRepo;
  private final CupMapper mapper;
  private final JoinCodeRegistryRepository joinCodeRegistryRepository;

  public JpaCupRepositoryAdapter(final SpringDataCupRepository springDataRepo,
      final CupMapper mapper, final JoinCodeRegistryRepository joinCodeRegistryRepository) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
    this.joinCodeRegistryRepository = joinCodeRegistryRepository;
  }

  private static PublicLobbyCursor decodeCursor(final String encodedCursor) {

    return encodedCursor == null ? null : PublicLobbyCursor.decode(encodedCursor);
  }

  private static String encodeCursor(final CupJpaEntity entity) {

    return new PublicLobbyCursor(entity.getLastActivityAt(), entity.getId()).encode();
  }

  @Override
  @Transactional
  public void save(final Cup cup) {

    try {
      final var entity = this.mapper.toEntity(cup);
      this.springDataRepo.saveAndFlush(entity);
      this.joinCodeRegistryRepository.save(
          new JoinCodeRegistration(cup.getJoinCode(), JoinTargetType.CUP, cup.getId().value()));
      cup.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException e) {
      throw new StaleAggregateException("Cup " + cup.getId() + " was modified concurrently", e);
    }
  }

  @Override
  public Optional<Cup> findById(final CupId cupId) {

    return this.springDataRepo.findById(cupId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<Cup> findByMatchId(final MatchId matchId) {

    return this.springDataRepo.findByMatchId(matchId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

    return this.springDataRepo.findInProgressByPlayer(playerId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

    return this.springDataRepo.findWaitingByPlayer(playerId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Map<PlayerId, Cup> findInProgressByPlayers(final Set<PlayerId> playerIds) {

    if (playerIds.isEmpty()) {
      return Map.of();
    }

    final var ids = playerIds.stream().map(PlayerId::value).collect(Collectors.toSet());
    final var byPlayer = new HashMap<PlayerId, Cup>();
    for (final var entity : this.springDataRepo.findInProgressByPlayers(ids)) {
      final var cup = this.mapper.toDomain(entity);
      for (final var participant : cup.getParticipants()) {
        if (playerIds.contains(participant)) {
          byPlayer.putIfAbsent(participant, cup);
        }
      }
    }
    return byPlayer;
  }

  @Override
  public Set<PlayerId> findPlayersWaitingInCup(final Set<PlayerId> playerIds) {

    if (playerIds.isEmpty()) {
      return Set.of();
    }

    final var ids = playerIds.stream().map(PlayerId::value).collect(Collectors.toSet());
    return this.springDataRepo.findPlayersWaitingInCup(ids).stream().map(PlayerId::new)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public List<CupId> findIdleCupIds(final Instant idleSince) {

    return this.springDataRepo.findIdleCupIds(idleSince).stream().map(CupId::new).toList();
  }

  @Override
  public Stream<CupTimeoutEntry> findActiveWithTimeoutDeadline() {

    return this.springDataRepo.findActiveCupsWithLastActivity().stream().map(
        row -> new CupTimeoutEntry(new CupId(row.getId()), row.getLastActivityAt(),
            CupStatus.valueOf(row.getStatus())));
  }

  @Override
  public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

    final var afterCursor = decodeCursor(pageQuery.after());
    final var pageable = PageRequest.of(0, pageQuery.limit() + 1);
    final List<CupJpaEntity> entities;

    if (afterCursor == null) {
      entities = this.springDataRepo.findInitialPublicWaitingPage(pageable);
    } else {
      entities = this.springDataRepo.findPublicWaitingPage(afterCursor.lastActivityAt(),
          afterCursor.resourceId(), pageable);
    }

    final var hasMore = entities.size() > pageQuery.limit();
    final var pageItems = hasMore ? entities.subList(0, pageQuery.limit()) : entities;
    final var nextCursor = hasMore ? encodeCursor(pageItems.getLast()) : null;

    return new CursorPageResult<>(pageItems.stream().map(this.mapper::toDomain).toList(),
        nextCursor);
  }

}
