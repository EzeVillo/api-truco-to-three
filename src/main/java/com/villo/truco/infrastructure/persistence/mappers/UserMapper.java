package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.user.User;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserJpaEntity toEntity(final User user) {

    final var entity = new UserJpaEntity();
    entity.setId(user.id().value());
    entity.setUsername(user.username());
    entity.setHashedPassword(user.hashedPassword());
    return entity;
  }

  public User toDomain(final UserJpaEntity entity) {

    return new User(new PlayerId(entity.getId()), entity.getUsername(), entity.getHashedPassword());
  }

}
