package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.TournamentFixtureDTO;
import com.villo.truco.application.dto.TournamentMatchdayDTO;
import com.villo.truco.application.dto.TournamentStandingDTO;
import com.villo.truco.application.dto.TournamentStateDTO;
import com.villo.truco.application.ports.in.GetTournamentStateUseCase;
import com.villo.truco.application.queries.GetTournamentStateQuery;
import com.villo.truco.application.usecases.commands.TournamentResolver;
import com.villo.truco.domain.model.tournament.exceptions.PlayerNotInTournamentException;
import java.util.Objects;

public final class GetTournamentStateQueryHandler implements GetTournamentStateUseCase {

  private final TournamentResolver tournamentResolver;

  public GetTournamentStateQueryHandler(final TournamentResolver tournamentResolver) {

    this.tournamentResolver = Objects.requireNonNull(tournamentResolver);
  }

  @Override
  public TournamentStateDTO handle(final GetTournamentStateQuery query) {

    final var tournament = this.tournamentResolver.resolve(query.tournamentId());

    if (!tournament.hasPlayer(query.requestingPlayer())) {
      throw new PlayerNotInTournamentException();
    }

    final var standings = tournament.getWinsByPlayer().entrySet().stream().map(
            entry -> new TournamentStandingDTO(entry.getKey().value().toString(), entry.getValue()))
        .sorted((left, right) -> Integer.compare(right.wins(), left.wins())).toList();

    final var winners = tournament.getLeaders().stream()
        .map(playerId -> playerId.value().toString()).toList();

    final var matchdays = tournament.getMatchdays().stream().map(matchday -> {
      final var matchdayFixtures = matchday.fixtures().stream().map(
          fixture -> new TournamentFixtureDTO(fixture.fixtureId().value().toString(),
              fixture.matchdayNumber(), fixture.playerOne().value().toString(),
              fixture.playerTwo() != null ? fixture.playerTwo().value().toString() : null,
              fixture.matchId() != null ? fixture.matchId().value().toString() : null,
              fixture.winner() != null ? fixture.winner().value().toString() : null,
              fixture.status().name())).toList();

      return new TournamentMatchdayDTO(matchday.matchdayNumber(), matchdayFixtures);
    }).toList();

    return new TournamentStateDTO(tournament.getId().value().toString(),
        tournament.getStatus().name(), standings, winners, matchdays);
  }

}
