package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.LeagueFixtureDTO;
import com.villo.truco.application.dto.LeagueMatchdayDTO;
import com.villo.truco.application.dto.LeagueStandingDTO;
import com.villo.truco.application.dto.LeagueStateDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.queries.GetLeagueStateQuery;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import java.util.Objects;

public final class GetLeagueStateQueryHandler implements GetLeagueStateUseCase {

  private final LeagueResolver leagueResolver;
  private final PublicActorResolver publicActorResolver;

  public GetLeagueStateQueryHandler(final LeagueResolver leagueResolver,
      final PublicActorResolver publicActorResolver) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  @Override
  public LeagueStateDTO handle(final GetLeagueStateQuery query) {

    final var league = this.leagueResolver.resolve(query.leagueId());

    league.validatePlayerInLeague(query.requestingPlayer());

    final var standings = league.getWinsByPlayer().entrySet().stream().map(
            entry -> new LeagueStandingDTO(this.publicActorResolver.resolve(entry.getKey()),
                entry.getValue())).sorted((left, right) -> Integer.compare(right.wins(), left.wins()))
        .toList();

    final var winners = league.getLeaders().stream().map(this.publicActorResolver::resolve)
        .toList();

    final var matchdays = league.getMatchdays().stream().map(matchday -> {
      final var matchdayFixtures = matchday.fixtures().stream().map(
          fixture -> new LeagueFixtureDTO(fixture.fixtureId().value().toString(),
              fixture.matchdayNumber(), this.publicActorResolver.resolve(fixture.playerOne()),
              fixture.playerTwo() != null ? this.publicActorResolver.resolve(fixture.playerTwo())
                  : null, fixture.matchId() != null ? fixture.matchId().value().toString() : null,
              fixture.winner() != null ? this.publicActorResolver.resolve(fixture.winner()) : null,
              fixture.status().name())).toList();

      return new LeagueMatchdayDTO(matchday.matchdayNumber(), matchdayFixtures);
    }).toList();

    return new LeagueStateDTO(league.getId().value().toString(), league.getStatus().name(),
        standings, winners, matchdays);
  }

}
