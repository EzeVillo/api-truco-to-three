package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.dto.JoinMatchDTO;
import com.villo.truco.application.ports.AggregateLockManager;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class JoinMatchCommandHandler implements JoinMatchUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final AggregateLockManager<MatchId> matchLockManager;

  public JoinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.matchLockManager = Objects.requireNonNull(matchLockManager);
  }

  @Override
  public JoinMatchDTO handle(final JoinMatchCommand command) {

    final var match = this.matchResolver.resolve(command.inviteCode());

    return this.matchLockManager.executeWithLock(match.getId(), () -> {
      match.join(command.playerId(), command.inviteCode());

      this.matchRepository.save(match);
      this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
          match.getPlayerTwo(), match.getDomainEvents());
      match.clearDomainEvents();

      return new JoinMatchDTO(match.getId().value().toString());
    });
  }

}
