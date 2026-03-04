package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.dto.JoinMatchDTO;
import com.villo.truco.application.ports.SessionGrantProvider;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class JoinMatchCommandHandler implements JoinMatchUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final SessionGrantProvider sessionGrantProvider;

  public JoinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final SessionGrantProvider sessionGrantProvider) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.sessionGrantProvider = Objects.requireNonNull(sessionGrantProvider);
  }

  @Override
  public JoinMatchDTO handle(final JoinMatchCommand command) {

    final var match = this.matchResolver.resolve(command.matchId());

    match.join(command.inviteCode());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();

    final var sessionGrant = this.sessionGrantProvider.generateGrant(match.getId(),
        match.getPlayerTwo());

    return new JoinMatchDTO(sessionGrant);
  }

}
