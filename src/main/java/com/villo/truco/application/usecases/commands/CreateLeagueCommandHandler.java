package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateLeagueCommand;
import com.villo.truco.application.dto.CreateLeagueDTO;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class CreateLeagueCommandHandler implements CreateLeagueUseCase {

  private final LeagueRepository leagueRepository;

  public CreateLeagueCommandHandler(final LeagueRepository leagueRepository) {

    this.leagueRepository = Objects.requireNonNull(leagueRepository);
  }

  @Override
  public CreateLeagueDTO handle(final CreateLeagueCommand command) {

    final var league = League.create(command.playerId(), command.numberOfPlayers(),
        command.gamesToPlay());

    this.leagueRepository.save(league);

    return new CreateLeagueDTO(league.getId().value().toString(), league.getInviteCode().value());
  }

}
