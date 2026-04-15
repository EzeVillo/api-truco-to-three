package com.villo.truco.social.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import java.util.List;
import java.util.Optional;

public interface FriendshipQueryRepository {

  Optional<Friendship> findById(FriendshipId friendshipId);

  boolean existsAcceptedByPlayers(PlayerId firstPlayerId, PlayerId secondPlayerId);

  Optional<Friendship> findPendingByPlayers(PlayerId firstPlayerId, PlayerId secondPlayerId);

  Optional<Friendship> findPendingByRequesterAndAddressee(PlayerId requesterId,
      PlayerId addresseeId);

  Optional<Friendship> findAcceptedByPlayers(PlayerId firstPlayerId, PlayerId secondPlayerId);

  List<Friendship> findAcceptedByPlayer(PlayerId playerId);

  List<Friendship> findPendingReceivedBy(PlayerId playerId);

  List<Friendship> findPendingSentBy(PlayerId playerId);

}
