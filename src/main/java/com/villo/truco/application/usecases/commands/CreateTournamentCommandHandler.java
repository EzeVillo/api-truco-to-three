package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateTournamentCommand;
import com.villo.truco.application.dto.CreateTournamentDTO;
import com.villo.truco.application.dto.TournamentFixtureDTO;
import com.villo.truco.application.dto.TournamentMatchdayDTO;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class CreateTournamentCommandHandler implements CreateTournamentUseCase {

  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final MatchRules matchRules;

  public CreateTournamentCommandHandler(final TournamentRepository tournamentRepository,
      final MatchRepository matchRepository, final MatchRules matchRules) {

    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchRules = Objects.requireNonNull(matchRules);
  }

  @Override
  public CreateTournamentDTO handle(final CreateTournamentCommand command) {

    final var tournament = Tournament.create(command.playerIds());

    for (final var fixture : tournament.getFixtures()) {
      if (fixture.status() == FixtureStatus.LIBRE) {
        continue;
      }

      final var match = Match.create(fixture.playerOne(), fixture.playerTwo(), this.matchRules);
      match.join(match.getInviteCode());
      this.matchRepository.save(match);
      tournament.linkFixtureMatch(fixture.fixtureId(), match.getId());
    }

    this.tournamentRepository.save(tournament);

    final var matchdayDTOs = tournament.getMatchdays().stream().map(matchday -> {
      final var matchdayFixtures = matchday.fixtures().stream().map(
          fixture -> new TournamentFixtureDTO(fixture.fixtureId().value().toString(),
              fixture.matchdayNumber(), fixture.playerOne().value().toString(),
              fixture.playerTwo() != null ? fixture.playerTwo().value().toString() : null,
              fixture.matchId() != null ? fixture.matchId().value().toString() : null,
              fixture.winner() != null ? fixture.winner().value().toString() : null,
              fixture.status().name())).toList();

      return new TournamentMatchdayDTO(matchday.matchdayNumber(), matchdayFixtures);
    }).toList();

    return new CreateTournamentDTO(tournament.getId().value().toString(), matchdayDTOs);
  }

}
