package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.LeagueFixtureDTO;
import com.villo.truco.application.dto.LeagueMatchdayDTO;
import com.villo.truco.application.dto.LeagueStandingDTO;
import com.villo.truco.application.dto.LeagueStateDTO;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.queries.GetLeagueStateQuery;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import java.util.Objects;

public final class GetLeagueStateQueryHandler implements GetLeagueStateUseCase {

  private final LeagueResolver leagueResolver;

  public GetLeagueStateQueryHandler(final LeagueResolver leagueResolver) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
  }

  @Override
  public LeagueStateDTO handle(final GetLeagueStateQuery query) {

    final var league = this.leagueResolver.resolve(query.leagueId());

    league.validatePlayerInLeague(query.requestingPlayer());

    final var standings = league.getWinsByPlayer().entrySet().stream()
        .map(entry -> new LeagueStandingDTO(entry.getKey().value().toString(), entry.getValue()))
        .sorted((left, right) -> Integer.compare(right.wins(), left.wins())).toList();

    final var winners = league.getLeaders().stream().map(playerId -> playerId.value().toString())
        .toList();

    final var matchdays = league.getMatchdays().stream().map(matchday -> {
      final var matchdayFixtures = matchday.fixtures().stream().map(
          fixture -> new LeagueFixtureDTO(fixture.fixtureId().value().toString(),
              fixture.matchdayNumber(), fixture.playerOne().value().toString(),
              fixture.playerTwo() != null ? fixture.playerTwo().value().toString() : null,
              fixture.matchId() != null ? fixture.matchId().value().toString() : null,
              fixture.winner() != null ? fixture.winner().value().toString() : null,
              fixture.status().name())).toList();

      return new LeagueMatchdayDTO(matchday.matchdayNumber(), matchdayFixtures);
    }).toList();

    return new LeagueStateDTO(league.getId().value().toString(), league.getStatus().name(),
        standings, winners, matchdays);
  }

}
