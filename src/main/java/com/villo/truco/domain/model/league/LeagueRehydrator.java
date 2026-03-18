package com.villo.truco.domain.model.league;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class LeagueRehydrator {

  private LeagueRehydrator() {

  }

  public static League rehydrate(final LeagueSnapshot.LeagueData snapshot) {

    final var fixtures = new ArrayList<League.Fixture>();
    for (final var fixture : snapshot.fixtures()) {
      fixtures.add(
          League.Fixture.reconstruct(fixture.id(), fixture.matchdayNumber(), fixture.playerOne(),
              fixture.playerTwo(), fixture.matchId(), fixture.winner(), fixture.status()));
    }

    return League.reconstruct(snapshot.id(), new ArrayList<>(snapshot.participants()), fixtures,
        new LinkedHashMap<>(snapshot.winsByPlayer()), snapshot.status(), snapshot.numberOfPlayers(),
        snapshot.gamesToPlay(), snapshot.inviteCode());
  }

}
