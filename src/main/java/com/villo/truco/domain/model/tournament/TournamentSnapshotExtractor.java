package com.villo.truco.domain.model.tournament;

import java.util.LinkedHashMap;
import java.util.List;

public final class TournamentSnapshotExtractor {

  private TournamentSnapshotExtractor() {

  }

  public static TournamentSnapshot.TournamentData extract(final Tournament tournament) {

    final var fixtures = tournament.getFixturesInternal().stream()
        .map(TournamentSnapshotExtractor::extractFixture).toList();

    return new TournamentSnapshot.TournamentData(tournament.getId(),
        List.copyOf(tournament.getParticipants()), fixtures,
        new LinkedHashMap<>(tournament.getWinsByPlayer()), tournament.getCapacity(),
        tournament.getGamesToPlay(), tournament.getInviteCode(), tournament.getStatus());
  }

  private static TournamentSnapshot.FixtureData extractFixture(final Tournament.Fixture fixture) {

    return new TournamentSnapshot.FixtureData(fixture.id(), fixture.matchdayNumber(),
        fixture.playerOne(), fixture.playerTwo(), fixture.matchId(), fixture.winner(),
        fixture.status());
  }

}
