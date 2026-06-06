package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface FriendshipSpectateEligibilityResolver {

  boolean canSpectateAsFriend(Match match, PlayerId spectatorId);

}
