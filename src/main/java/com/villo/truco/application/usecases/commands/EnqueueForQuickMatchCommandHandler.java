package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.EnqueueForQuickMatchCommand;
import com.villo.truco.application.dto.QuickMatchSearchDTO;
import com.villo.truco.application.dto.QuickMatchStatus;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.application.ports.in.EnqueueForQuickMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class EnqueueForQuickMatchCommandHandler implements EnqueueForQuickMatchUseCase {

  private final QuickMatchQueuePort queuePort;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier;
  private final PresenceNotifier presenceNotifier;

  public EnqueueForQuickMatchCommandHandler(final QuickMatchQueuePort queuePort,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier,
      final PresenceNotifier presenceNotifier) {

    this.queuePort = Objects.requireNonNull(queuePort);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.friendAvailabilityChangeNotifier = Objects.requireNonNull(
        friendAvailabilityChangeNotifier);
    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  @Override
  public QuickMatchSearchDTO handle(final EnqueueForQuickMatchCommand command) {

    final var playerId = command.playerId();

    final var existingTicket = this.queuePort.findByPlayer(playerId);
    if (existingTicket.isPresent()) {
      return new QuickMatchSearchDTO(QuickMatchStatus.SEARCHING, null,
          existingTicket.get().enqueuedAt());
    }

    this.playerAvailabilityChecker.ensureAvailable(playerId);

    final var opponent = this.queuePort.tryMatchOpponent(playerId, command.gamesToPlay());
    if (opponent.isPresent()) {
      final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay(), true);
      final var match = Match.quickMatch(opponent.get().playerId(), playerId, rules);
      this.matchRepository.save(match);
      this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
      match.clearDomainEvents();
      return new QuickMatchSearchDTO(QuickMatchStatus.MATCHED, match.getId().value(),
          Instant.now());
    }

    final var ticket = new QuickMatchTicket(playerId, command.gamesToPlay(), Instant.now(),
        command.webSocketSessionId());
    this.queuePort.enqueue(ticket);
    this.friendAvailabilityChangeNotifier.notifyAvailabilityChanged(playerId,
        ticket.enqueuedAt().toEpochMilli());
    this.presenceNotifier.notifyPlayers(List.of(playerId));
    return new QuickMatchSearchDTO(QuickMatchStatus.SEARCHING, null, ticket.enqueuedAt());
  }

}
