package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.JoinCodeRegistryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.LeagueJpaEntity;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.persistence.mappers.LeagueMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataLeagueRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaLeagueRepositoryAdapter implements LeagueRepository, LeagueQueryRepository {

  private final SpringDataLeagueRepository springDataRepo;
  private final LeagueMapper mapper;
  private final JoinCodeRegistryRepository joinCodeRegistryRepository;

  public JpaLeagueRepositoryAdapter(final SpringDataLeagueRepository springDataRepo,
      final LeagueMapper mapper, final JoinCodeRegistryRepository joinCodeRegistryRepository) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
    this.joinCodeRegistryRepository = joinCodeRegistryRepository;
  }

  private static PublicLobbyCursor decodeCursor(final String encodedCursor) {

    return encodedCursor == null ? null : PublicLobbyCursor.decode(encodedCursor);
  }

  private static String encodeCursor(final LeagueJpaEntity entity) {

    return new PublicLobbyCursor(entity.getLastActivityAt(), entity.getId()).encode();
  }

  @Override
  @Transactional
  public void save(final League league) {

    try {
      final var entity = this.mapper.toEntity(league);
      this.springDataRepo.saveAndFlush(entity);
      this.joinCodeRegistryRepository.save(
          new JoinCodeRegistration(league.getJoinCode(), JoinTargetType.LEAGUE,
              league.getId().value()));
      league.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException e) {
      throw new StaleAggregateException("League " + league.getId() + " was modified concurrently",
          e);
    }
  }

  @Override
  public Optional<League> findById(final LeagueId leagueId) {

    return this.springDataRepo.findById(leagueId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<League> findByMatchId(final MatchId matchId) {

    return this.springDataRepo.findByMatchId(matchId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

    return this.springDataRepo.findInProgressByPlayer(playerId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

    return this.springDataRepo.findWaitingByPlayer(playerId.value()).map(this.mapper::toDomain);
  }

  @Override
  public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

    return this.springDataRepo.findIdleLeagueIds(idleSince).stream().map(LeagueId::new).toList();
  }

  @Override
  public List<League> findPublicWaiting() {

    return this.springDataRepo.findPublicWaiting().stream().map(this.mapper::toDomain).toList();
  }

  @Override
  public CursorPageResult<League> findPublicWaiting(final CursorPageQuery pageQuery) {

    final var afterCursor = decodeCursor(pageQuery.after());
    final var pageable = PageRequest.of(0, pageQuery.limit() + 1);
    final List<LeagueJpaEntity> entities;

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
