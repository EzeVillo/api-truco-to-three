package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.infrastructure.persistence.mappers.LeagueMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataLeagueRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaLeagueRepositoryAdapter implements LeagueRepository, LeagueQueryRepository {

  private final SpringDataLeagueRepository springDataRepo;
  private final LeagueMapper mapper;

  public JpaLeagueRepositoryAdapter(final SpringDataLeagueRepository springDataRepo,
      final LeagueMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void save(final League league) {

    try {
      final var entity = this.mapper.toEntity(league);
      this.springDataRepo.saveAndFlush(entity);
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
  public Optional<League> findByInviteCode(final InviteCode inviteCode) {

    return this.springDataRepo.findByInviteCode(inviteCode.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<League> findByMatchId(final MatchId matchId) {

    return this.springDataRepo.findByMatchId(matchId.value()).map(this.mapper::toDomain);
  }

}
