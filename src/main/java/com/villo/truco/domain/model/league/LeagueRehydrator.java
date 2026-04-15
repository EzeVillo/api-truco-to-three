package com.villo.truco.domain.model.league;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class LeagueRehydrator {

  private LeagueRehydrator() {

  }

  public static League rehydrate(final LeagueSnapshot snapshot) {

    final var fixtures = new ArrayList<Fixture>();
    for (final var fixture : snapshot.fixtures()) {
      fixtures.add(Fixture.reconstruct(fixture.id(), fixture.matchdayNumber(), fixture.playerOne(),
          fixture.playerTwo(), fixture.matchId(), fixture.winner(), fixture.status()));
    }

    return League.reconstruct(snapshot.id(), new ArrayList<>(snapshot.participants()), fixtures,
        new LinkedHashMap<>(snapshot.winsByPlayer()), snapshot.status(), snapshot.numberOfPlayers(),
        snapshot.gamesToPlay(), snapshot.visibility(), snapshot.joinCode());
  }

}
