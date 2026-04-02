package com.villo.truco.auth.infrastructure.persistence.mappers;

import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.UserSnapshot;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.infrastructure.persistence.entities.UserJpaEntity;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserJpaEntity toEntity(final User user) {

    return this.toEntity(user.snapshot());
  }

  public UserJpaEntity toEntity(final UserSnapshot snapshot) {

    final var entity = new UserJpaEntity();
    entity.setId(snapshot.id().value());
    entity.setUsername(snapshot.username().value());
    entity.setHashedPassword(snapshot.hashedPassword().value());
    return entity;
  }

  public UserSnapshot toSnapshot(final UserJpaEntity entity) {

    return new UserSnapshot(new PlayerId(entity.getId()), new Username(entity.getUsername()),
        new HashedPassword(entity.getHashedPassword()));
  }

}
