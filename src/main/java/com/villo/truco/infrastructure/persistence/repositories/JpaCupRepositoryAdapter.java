package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.mappers.CupMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataCupRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaCupRepositoryAdapter implements CupRepository, CupQueryRepository {

  private final SpringDataCupRepository springDataRepo;
  private final CupMapper mapper;

  public JpaCupRepositoryAdapter(final SpringDataCupRepository springDataRepo,
      final CupMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void save(final Cup cup) {

    try {
      final var entity = this.mapper.toEntity(cup);
      this.springDataRepo.saveAndFlush(entity);
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
  public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

    return this.springDataRepo.findByInviteCode(inviteCode.value()).map(this.mapper::toDomain);
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
  public List<CupId> findIdleCupIds(final Instant idleSince) {

    return this.springDataRepo.findIdleCupIds(idleSince).stream().map(CupId::new).toList();
  }

}
