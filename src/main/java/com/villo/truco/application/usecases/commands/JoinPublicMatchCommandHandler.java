package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinPublicMatchCommand;
import com.villo.truco.application.dto.JoinMatchDTO;
import com.villo.truco.application.exceptions.PublicMatchLobbyConflictException;
import com.villo.truco.application.ports.in.JoinPublicMatchUseCase;
import com.villo.truco.domain.model.match.exceptions.PublicMatchLobbyUnavailableException;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class JoinPublicMatchCommandHandler implements JoinPublicMatchUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public JoinPublicMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public JoinMatchDTO handle(final JoinPublicMatchCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var match = this.matchResolver.resolve(command.matchId());

    try {
      match.joinPublic(command.playerId());
    } catch (final PublicMatchLobbyUnavailableException ex) {
      throw new PublicMatchLobbyConflictException();
    }

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return new JoinMatchDTO(match.getId().value().toString());
  }

}
