package com.villo.truco.domain.model.cup;

import java.util.HashSet;
import java.util.List;

public final class CupSnapshotExtractor {

  private CupSnapshotExtractor() {

  }

  public static CupSnapshot.CupData extract(final Cup cup) {

    final var bouts = cup.getBoutsInternal().stream().map(CupSnapshotExtractor::extractBout)
        .toList();

    return new CupSnapshot.CupData(cup.getId(), List.copyOf(cup.getParticipants()), bouts,
        new HashSet<>(cup.getForfeitedPlayersInternal()), cup.getNumberOfPlayers(),
        cup.getGamesToPlay(), cup.getInviteCode(), cup.getStatus(), cup.getChampion());
  }

  private static CupSnapshot.BoutData extractBout(final Cup.Bout bout) {

    return new CupSnapshot.BoutData(bout.id(), bout.roundNumber(), bout.bracketPosition(),
        bout.playerOne(), bout.playerTwo(), bout.matchId(), bout.winner(), bout.status());
  }

}
