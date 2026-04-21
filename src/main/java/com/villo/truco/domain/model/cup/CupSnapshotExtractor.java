package com.villo.truco.domain.model.cup;

import java.util.HashSet;
import java.util.List;

public final class CupSnapshotExtractor {

  private CupSnapshotExtractor() {

  }

  public static CupSnapshot extract(final Cup cup) {

    final var bouts = cup.getBoutsInternal().stream().map(CupSnapshotExtractor::extractBout)
        .toList();

    return new CupSnapshot(cup.getId(), List.copyOf(cup.getParticipants()), bouts,
        new HashSet<>(cup.getForfeitedPlayersInternal()), cup.getNumberOfPlayers(),
        cup.getGamesToPlay(), cup.getVisibility(), cup.getJoinCode(), cup.getStatus(),
        cup.getChampion());
  }

  private static BoutSnapshot extractBout(final Bout bout) {

    return new BoutSnapshot(bout.id(), bout.roundNumber(), bout.bracketPosition(), bout.playerOne(),
        bout.playerTwo(), bout.matchId(), bout.winner(), bout.status());
  }

}
