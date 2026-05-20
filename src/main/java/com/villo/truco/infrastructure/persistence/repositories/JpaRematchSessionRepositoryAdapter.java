package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.mappers.RematchSessionMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataRematchSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
  public List<RematchSession> findExpiredCandidates(final Instant now, final int batchSize) {

    return springDataRepo.findExpiredCandidates(now, PageRequest.of(0, batchSize)).stream()
        .map(mapper::toDomain).toList();
  }

  @Override
  @Transactional
  public void save(final RematchSession session) {

    final var entity = mapper.toEntity(session);
    springDataRepo.saveAndFlush(entity);
    session.setVersion(entity.getVersion());
  }

}
