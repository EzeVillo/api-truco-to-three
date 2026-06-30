package com.villo.truco.social.application.services;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.Friendship;

public final class FriendActivityTestFixtures {

  private FriendActivityTestFixtures() {

  }

  public static Friendship acceptedFriendship(final PlayerId requester, final PlayerId friend) {

    final var friendship = Friendship.request(requester, friend, true);
    friendship.accept(friend);
    return friendship;
  }

  public static Match startedMatch(final PlayerId playerOne, final PlayerId playerTwo) {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    return match;
  }

}
