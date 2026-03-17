package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.ports.AggregateLockManager;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class FoldCommandHandler implements FoldUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final AggregateLockManager<MatchId> matchLockManager;

  public FoldCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.matchLockManager = Objects.requireNonNull(matchLockManager);
  }

  @Override
  public MatchId handle(final FoldCommand command) {

    return this.matchLockManager.executeWithLock(command.matchId(), () -> {
      final var match = this.matchResolver.resolve(command.matchId());

      match.fold(command.playerId());

      this.matchRepository.save(match);
      this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
          match.getPlayerTwo(), match.getDomainEvents());
      match.clearDomainEvents();

      return command.matchId();
    });
  }

}

