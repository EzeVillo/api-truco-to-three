package com.villo.truco.domain.model.league;

import java.util.LinkedHashMap;
import java.util.List;

public final class LeagueSnapshotExtractor {

  private LeagueSnapshotExtractor() {

  }

  public static LeagueSnapshot extract(final League league) {

    final var fixtures = league.getFixturesInternal().stream()
        .map(LeagueSnapshotExtractor::extractFixture).toList();

    return new LeagueSnapshot(league.getId(), List.copyOf(league.getParticipants()), fixtures,
        new LinkedHashMap<>(league.getWinsByPlayer()), league.getNumberOfPlayers(),
        league.getGamesToPlay(), league.getInviteCode(), league.getStatus());
  }

  private static FixtureSnapshot extractFixture(final Fixture fixture) {

    return new FixtureSnapshot(fixture.id(), fixture.matchdayNumber(), fixture.playerOne(),
        fixture.playerTwo(), fixture.matchId(), fixture.winner(), fixture.status());
  }

}
