package com.villo.truco.auth.infrastructure.persistence.repositories;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.auth.infrastructure.persistence.entities.RefreshSessionJpaEntity;
import com.villo.truco.auth.infrastructure.persistence.mappers.UserSessionMapper;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.SpringDataRefreshSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JpaUserSessionRepositoryAdapter")
class JpaUserSessionRepositoryAdapterTest {

  private static RefreshSessionJpaEntity entity(final UUID id, final UUID userId,
      final String tokenHash, final UUID replacementId) {

    final var entity = new RefreshSessionJpaEntity();
    entity.setId(id);
    entity.setUserId(userId);
    entity.setTokenHash(tokenHash);
    entity.setExpiresAt(Instant.parse("2026-03-31T00:00:00Z"));
    entity.setCreatedAt(Instant.parse("2026-03-30T00:00:00Z"));
    entity.setReplacedBySessionId(replacementId);
    return entity;
  }

  @Test
  @DisplayName("resuelve la sesion de usuario desde el refresh token")
  void resolvesUserSessionFromRefreshTokenHash() {

    final var springRepo = mock(SpringDataRefreshSessionRepository.class);
    final var adapter = new JpaUserSessionRepositoryAdapter(springRepo, new UserSessionMapper());
    final var tokenId = UUID.randomUUID();
    final var entity = entity(tokenId, UUID.randomUUID(), "hash", null);

    when(springRepo.findByTokenHash("hash")).thenReturn(Optional.of(entity));
    when(springRepo.findByReplacedBySessionId(tokenId)).thenReturn(Optional.empty());

    adapter.findByRefreshTokenHash("hash");

    verify(springRepo).findByTokenHash("hash");
    verify(springRepo).findByReplacedBySessionId(tokenId);
  }

  @Test
  @DisplayName("resuelve la sesion raiz desde su id")
  void resolvesUserSessionFromRootId() {

    final var springRepo = mock(SpringDataRefreshSessionRepository.class);
    final var adapter = new JpaUserSessionRepositoryAdapter(springRepo, new UserSessionMapper());
    final var sessionId = UUID.randomUUID();
    final var entity = entity(sessionId, UUID.randomUUID(), "hash", null);

    when(springRepo.findById(sessionId)).thenReturn(Optional.of(entity));
    when(springRepo.findByReplacedBySessionId(sessionId)).thenReturn(Optional.empty());

    adapter.findById(new UserSessionId(sessionId));

    verify(springRepo).findById(sessionId);
    verify(springRepo).findByReplacedBySessionId(sessionId);
  }

}
