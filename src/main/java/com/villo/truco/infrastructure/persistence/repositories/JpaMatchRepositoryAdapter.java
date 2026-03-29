package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.mappers.MatchMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaMatchRepositoryAdapter implements MatchRepository, MatchQueryRepository {

  private final SpringDataMatchRepository springDataRepo;
  private final MatchMapper mapper;

  public JpaMatchRepositoryAdapter(final SpringDataMatchRepository springDataRepo,
      final MatchMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void save(final Match match) {

    try {
      final var entity = this.mapper.toEntity(match);
      this.springDataRepo.saveAndFlush(entity);
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
  public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

    return this.springDataRepo.findByInviteCode(inviteCode.value()).map(this.mapper::toDomain);
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

}
