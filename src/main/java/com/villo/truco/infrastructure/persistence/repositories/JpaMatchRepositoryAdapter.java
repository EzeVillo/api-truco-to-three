package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.JoinCodeRegistryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.MatchJpaEntity;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.persistence.mappers.MatchMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaMatchRepositoryAdapter implements MatchRepository, MatchQueryRepository {

  private final SpringDataMatchRepository springDataRepo;
  private final MatchMapper mapper;
  private final JoinCodeRegistryRepository joinCodeRegistryRepository;

  public JpaMatchRepositoryAdapter(final SpringDataMatchRepository springDataRepo,
      final MatchMapper mapper, final JoinCodeRegistryRepository joinCodeRegistryRepository) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
    this.joinCodeRegistryRepository = joinCodeRegistryRepository;
  }

  private static PublicLobbyCursor decodeCursor(final String encodedCursor) {

    return encodedCursor == null ? null : PublicLobbyCursor.decode(encodedCursor);
  }

  private static String encodeCursor(final MatchJpaEntity entity) {

    return new PublicLobbyCursor(entity.getLastActivityAt(), entity.getId()).encode();
  }

  @Override
  @Transactional
  public void save(final Match match) {

    try {
      final var entity = this.mapper.toEntity(match);
      this.springDataRepo.saveAndFlush(entity);
      this.joinCodeRegistryRepository.save(
          new JoinCodeRegistration(match.getJoinCode(), JoinTargetType.MATCH,
              match.getId().value()));
      match.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException e) {
      throw new StaleAggregateException("Match " + match.getId() + " was modified concurrently", e);
    }
  }

  @Override
  public Optional<Match> findById(final MatchId matchId) {

    return this.springDataRepo.findById(matchId.value()).map(this.mapper::toDomain);
  }

  @Override
  public boolean hasActiveMatch(final PlayerId playerId) {

    return this.springDataRepo.hasActiveMatch(playerId.value());
  }

  @Override
  public boolean hasUnfinishedMatch(final PlayerId playerId) {

    return this.springDataRepo.hasUnfinishedMatch(playerId.value());
  }

  @Override
  public List<MatchId> findIdleMatchIds(final Instant idleSince) {

    return this.springDataRepo.findIdleMatchIds(idleSince).stream().map(MatchId::new).toList();
  }

  @Override
  public List<Match> findPublicWaiting() {

    return this.springDataRepo.findPublicWaiting().stream().map(this.mapper::toDomain).toList();
  }

  @Override
  public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

    final var afterCursor = decodeCursor(pageQuery.after());
    final var pageable = PageRequest.of(0, pageQuery.limit() + 1);
    final List<MatchJpaEntity> entities;

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
