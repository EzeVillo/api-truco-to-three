package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateLeagueCommand;
import com.villo.truco.application.dto.CreateLeagueDTO;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class CreateLeagueCommandHandler implements CreateLeagueUseCase {

  private final LeagueRepository leagueRepository;
  private final LeagueEventNotifier leagueEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public CreateLeagueCommandHandler(final LeagueRepository leagueRepository,
      final LeagueEventNotifier leagueEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public CreateLeagueDTO handle(final CreateLeagueCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var league = League.create(command.playerId(), command.numberOfPlayers(),
        command.gamesToPlay(), command.visibility());

    this.leagueRepository.save(league);
    this.leagueEventNotifier.publishDomainEvents(league.getLeagueDomainEvents());
    league.clearDomainEvents();

    return new CreateLeagueDTO(league.getId().value().toString(),
        league.getInviteCode() != null ? league.getInviteCode().value() : null,
        league.getVisibility().name());
  }

}
