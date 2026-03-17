package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.infrastructure.persistence.mappers.TournamentMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataTournamentRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaTournamentRepositoryAdapter implements TournamentRepository,
    TournamentQueryRepository {

  private final SpringDataTournamentRepository springDataRepo;
  private final TournamentMapper mapper;

  public JpaTournamentRepositoryAdapter(final SpringDataTournamentRepository springDataRepo,
      final TournamentMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void save(final Tournament tournament) {

    try {
      final var entity = this.mapper.toEntity(tournament);
      this.springDataRepo.saveAndFlush(entity);
      tournament.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException e) {
      throw new StaleAggregateException(
          "Tournament " + tournament.getId() + " was modified concurrently", e);
    }
  }

  @Override
  public Optional<Tournament> findById(final TournamentId tournamentId) {

    return this.springDataRepo.findById(tournamentId.value()).map(this.mapper::toDomain);
  }

  @Override
  public Optional<Tournament> findByInviteCode(final InviteCode inviteCode) {

    return this.springDataRepo.findByInviteCode(inviteCode.value()).map(this.mapper::toDomain);
  }

}
