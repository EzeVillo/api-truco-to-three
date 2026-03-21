package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class StartMatchCommandHandler implements StartMatchUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public StartMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public MatchId handle(final StartMatchCommand command) {

    final var match = this.matchResolver.resolve(command.matchId());

    if (match.getStatus() != MatchStatus.IN_PROGRESS) {
      this.playerAvailabilityChecker.ensureCanStartMatch(match.getPlayerOne());
      if (match.getPlayerTwo() != null) {
        this.playerAvailabilityChecker.ensureCanStartMatch(match.getPlayerTwo());
      }
    }

    match.startMatch(command.playerId());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();

    return command.matchId();
  }

}
