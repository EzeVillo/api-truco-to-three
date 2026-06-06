package com.villo.truco.social.application.ports.out;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface FriendOnlinePresencePort {

  boolean isOnline(PlayerId playerId);

}
