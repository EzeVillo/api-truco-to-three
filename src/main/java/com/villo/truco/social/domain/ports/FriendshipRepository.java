package com.villo.truco.social.domain.ports;

import com.villo.truco.social.domain.model.friendship.Friendship;

public interface FriendshipRepository {

  void save(Friendship friendship);

}
