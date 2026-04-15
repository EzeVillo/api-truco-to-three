package com.villo.truco.social.infrastructure.persistence.mappers;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.FriendshipRehydrator;
import com.villo.truco.social.domain.model.friendship.FriendshipSnapshot;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;
import com.villo.truco.social.infrastructure.persistence.entities.FriendshipJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class FriendshipMapper {

  public FriendshipJpaEntity toEntity(final Friendship friendship) {

    final var entity = new FriendshipJpaEntity();
    entity.setId(friendship.getId().value());
    entity.setRequesterId(friendship.getRequesterId().value());
    entity.setAddresseeId(friendship.getAddresseeId().value());
    entity.setStatus(friendship.getStatus().name());
    entity.setVersion((int) friendship.getVersion());
    return entity;
  }

  public Friendship toDomain(final FriendshipJpaEntity entity) {

    final var friendship = FriendshipRehydrator.rehydrate(
        new FriendshipSnapshot(new FriendshipId(entity.getId()),
            new PlayerId(entity.getRequesterId()), new PlayerId(entity.getAddresseeId()),
            FriendshipStatus.valueOf(entity.getStatus())));
    friendship.setVersion(entity.getVersion());
    return friendship;
  }

}
