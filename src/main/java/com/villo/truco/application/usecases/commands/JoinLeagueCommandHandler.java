package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinLeagueCommand;
import com.villo.truco.application.dto.JoinLeagueDTO;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class JoinLeagueCommandHandler implements JoinLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final LeagueEventNotifier leagueEventNotifier;

  public JoinLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final LeagueEventNotifier leagueEventNotifier) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
  }

  @Override
  public JoinLeagueDTO handle(final JoinLeagueCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var league = this.leagueResolver.resolve(command.inviteCode());

    league.join(command.playerId(), command.inviteCode());

    this.leagueRepository.save(league);

    this.leagueEventNotifier.publishDomainEvents(league.getLeagueDomainEvents());

    league.clearDomainEvents();

    return new JoinLeagueDTO(league.getId().value().toString());
  }

}
