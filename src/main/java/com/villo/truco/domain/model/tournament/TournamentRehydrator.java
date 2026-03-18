package com.villo.truco.domain.model.tournament;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class TournamentRehydrator {

  private TournamentRehydrator() {

  }

  public static Tournament rehydrate(final TournamentSnapshot.TournamentData snapshot) {

    final var fixtures = new ArrayList<Tournament.Fixture>();
    for (final var fixture : snapshot.fixtures()) {
      fixtures.add(Tournament.Fixture.reconstruct(fixture.id(), fixture.matchdayNumber(),
          fixture.playerOne(), fixture.playerTwo(), fixture.matchId(), fixture.winner(),
          fixture.status()));
    }

    return Tournament.reconstruct(snapshot.id(), new ArrayList<>(snapshot.participants()), fixtures,
        new LinkedHashMap<>(snapshot.winsByPlayer()), snapshot.status(), snapshot.numberOfPlayers(),
        snapshot.gamesToPlay(), snapshot.inviteCode());
  }

}
