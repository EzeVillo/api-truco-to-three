package com.villo.truco.auth.infrastructure.persistence.repositories;

import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.auth.infrastructure.persistence.entities.RefreshSessionJpaEntity;
import com.villo.truco.auth.infrastructure.persistence.mappers.UserSessionMapper;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.SpringDataRefreshSessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaUserSessionRepositoryAdapter implements UserSessionRepository {

  private final SpringDataRefreshSessionRepository springDataRepo;
  private final UserSessionMapper mapper;

  public JpaUserSessionRepositoryAdapter(final SpringDataRefreshSessionRepository springDataRepo,
      final UserSessionMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void save(final UserSession session) {

    this.springDataRepo.saveAllAndFlush(this.mapper.toEntities(session));
  }

  @Override
  public Optional<UserSession> findById(final UserSessionId id) {

    return this.springDataRepo.findById(id.value()).map(this::hydrateSessionFromAnyToken);
  }

  @Override
  public Optional<UserSession> findByRefreshTokenHash(final String tokenHash) {

    return this.springDataRepo.findByTokenHash(tokenHash).map(this::hydrateSessionFromAnyToken);
  }

  private UserSession hydrateSessionFromAnyToken(final RefreshSessionJpaEntity tokenEntity) {

    final var rootEntity = this.resolveRootEntity(tokenEntity);
    return this.mapper.toDomain(this.collectChain(rootEntity));
  }

  private RefreshSessionJpaEntity resolveRootEntity(final RefreshSessionJpaEntity tokenEntity) {

    var current = tokenEntity;
    Optional<RefreshSessionJpaEntity> predecessor;
    do {
      predecessor = this.springDataRepo.findByReplacedBySessionId(current.getId());
      if (predecessor.isPresent()) {
        current = predecessor.orElseThrow();
      }
    } while (predecessor.isPresent());
    return current;
  }

  private List<RefreshSessionJpaEntity> collectChain(final RefreshSessionJpaEntity rootEntity) {

    final var chain = new ArrayList<RefreshSessionJpaEntity>();
    var current = rootEntity;
    while (current != null) {
      chain.add(current);
      current = current.getReplacedBySessionId() == null ? null
          : this.springDataRepo.findById(current.getReplacedBySessionId()).orElseThrow(
              () -> new IllegalStateException(
                  "Refresh token chain is broken for user session " + rootEntity.getId()));
    }
    return chain;
  }

}
