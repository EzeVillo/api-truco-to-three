package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.ports.AggregateLockManager;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class RespondTrucoCommandHandler implements RespondTrucoUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final AggregateLockManager<MatchId> matchLockManager;

  public RespondTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.matchLockManager = Objects.requireNonNull(matchLockManager);
  }

  @Override
  public MatchId handle(final RespondTrucoCommand command) {

    return this.matchLockManager.executeWithLock(command.matchId(), () -> {
      final var match = this.matchResolver.resolve(command.matchId());

      switch (command.response()) {
        case QUIERO -> match.acceptTruco(command.playerId());
        case NO_QUIERO -> match.rejectTruco(command.playerId());
        case QUIERO_Y_ME_VOY_AL_MAZO -> match.acceptTrucoAndFold(command.playerId());
      }

      this.matchRepository.save(match);
      this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
          match.getPlayerTwo(), match.getDomainEvents());
      match.clearDomainEvents();

      return command.matchId();
    });
  }

}
