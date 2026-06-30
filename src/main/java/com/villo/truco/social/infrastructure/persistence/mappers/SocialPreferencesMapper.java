package com.villo.truco.social.infrastructure.persistence.mappers;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.infrastructure.persistence.entities.SocialPreferencesJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SocialPreferencesMapper {

  public SocialPreferencesJpaEntity toEntity(final SocialPreferences socialPreferences) {

    final var snapshot = socialPreferences.snapshot();
    final var entity = new SocialPreferencesJpaEntity();
    entity.setPlayerId(snapshot.playerId().value());
    entity.setAcceptsFriendRequests(snapshot.acceptsFriendRequests());
    entity.setVersion((int) socialPreferences.getVersion());
    return entity;
  }

  public SocialPreferences toDomain(final SocialPreferencesJpaEntity entity) {

    final var socialPreferences = SocialPreferences.reconstruct(new PlayerId(entity.getPlayerId()),
        entity.isAcceptsFriendRequests());
    socialPreferences.setVersion(entity.getVersion());
    return socialPreferences;
  }

}
