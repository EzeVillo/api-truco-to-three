package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateTournamentCommand;
import com.villo.truco.application.dto.CreateTournamentDTO;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class CreateTournamentCommandHandler implements CreateTournamentUseCase {

  private final TournamentRepository tournamentRepository;

  public CreateTournamentCommandHandler(final TournamentRepository tournamentRepository) {

    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
  }

  @Override
  public CreateTournamentDTO handle(final CreateTournamentCommand command) {

    final var tournament = Tournament.create(command.playerId(), command.numberOfPlayers(),
        command.gamesToPlay());

    this.tournamentRepository.save(tournament);

    return new CreateTournamentDTO(tournament.getId().value().toString(),
        tournament.getInviteCode().value());
  }

}
