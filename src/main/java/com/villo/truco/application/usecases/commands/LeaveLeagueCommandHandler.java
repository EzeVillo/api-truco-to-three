package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveLeagueCommand;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class LeaveLeagueCommandHandler implements LeaveLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;

  public LeaveLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
  }

  @Override
  public Void handle(final LeaveLeagueCommand command) {

    final var league = this.leagueResolver.resolve(command.leagueId());

    league.leave(command.playerId());

    this.leagueRepository.save(league);

    return null;
  }

}
