package com.villo.truco.domain.model.cup;

import java.util.ArrayList;

public final class CupRehydrator {

  private CupRehydrator() {

  }

  public static Cup rehydrate(final CupSnapshot snapshot) {

    final var bouts = new ArrayList<Bout>();
    for (final var bout : snapshot.bouts()) {
      bouts.add(
          Bout.reconstruct(bout.id(), bout.roundNumber(), bout.bracketPosition(), bout.playerOne(),
              bout.playerTwo(), bout.matchId(), bout.winner(), bout.status()));
    }

    return Cup.reconstruct(snapshot.id(), new ArrayList<>(snapshot.participants()), bouts,
        new java.util.HashSet<>(snapshot.forfeitedPlayers()), snapshot.numberOfPlayers(),
        snapshot.gamesToPlay(), snapshot.visibility(), snapshot.inviteCode(), snapshot.status(),
        snapshot.champion());
  }

}
