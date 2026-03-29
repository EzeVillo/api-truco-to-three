package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveLeagueCommand;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class LeaveLeagueCommandHandler implements LeaveLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final LeagueEventNotifier leagueEventNotifier;

  public LeaveLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final LeagueEventNotifier leagueEventNotifier) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
  }

  @Override
  public Void handle(final LeaveLeagueCommand command) {

    final var league = this.leagueResolver.resolve(command.leagueId());

    league.leave(command.playerId());

    this.leagueRepository.save(league);

    this.leagueEventNotifier.publishDomainEvents(league.getLeagueDomainEvents());
    league.clearDomainEvents();

    return null;
  }

}
