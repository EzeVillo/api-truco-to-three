package com.villo.truco.auth.infrastructure.persistence.mappers;

import com.villo.truco.auth.domain.model.auth.RefreshTokenSnapshot;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.UserSessionRehydrator;
import com.villo.truco.auth.domain.model.auth.UserSessionSnapshot;
import com.villo.truco.auth.domain.model.auth.valueobjects.RefreshTokenId;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.auth.infrastructure.persistence.entities.RefreshSessionJpaEntity;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserSessionMapper {

  public List<RefreshSessionJpaEntity> toEntities(final UserSession userSession) {

    return userSession.refreshTokenSnapshots().stream().map(tokenSnapshot -> {
      final var entity = new RefreshSessionJpaEntity();
      entity.setId(tokenSnapshot.id().value());
      entity.setUserId(tokenSnapshot.userId().value());
      entity.setTokenHash(tokenSnapshot.tokenHash());
      entity.setExpiresAt(tokenSnapshot.expiresAt());
      entity.setCreatedAt(tokenSnapshot.createdAt());
      entity.setRevokedAt(tokenSnapshot.revokedAt());
      entity.setRotatedAt(tokenSnapshot.rotatedAt());
      entity.setReplacedBySessionId(
          tokenSnapshot.replacedByTokenId() != null ? tokenSnapshot.replacedByTokenId().value()
              : null);
      return entity;
    }).toList();
  }

  public UserSession toDomain(final List<RefreshSessionJpaEntity> entities) {

    final var tokenSnapshots = entities.stream().map(
        entity -> new RefreshTokenSnapshot(new RefreshTokenId(entity.getId()),
            new PlayerId(entity.getUserId()), entity.getTokenHash(), entity.getExpiresAt(),
            entity.getCreatedAt(), entity.getRevokedAt(), entity.getRotatedAt(),
            entity.getReplacedBySessionId() != null ? new RefreshTokenId(
                entity.getReplacedBySessionId()) : null)).toList();
    final var rootEntity = entities.getFirst();
    return UserSessionRehydrator.rehydrate(
        new UserSessionSnapshot(new UserSessionId(rootEntity.getId()),
            new PlayerId(rootEntity.getUserId()), tokenSnapshots));
  }

}
