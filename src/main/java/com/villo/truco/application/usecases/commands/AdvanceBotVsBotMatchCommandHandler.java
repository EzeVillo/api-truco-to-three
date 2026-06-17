package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.AdvanceBotVsBotMatchCommand;
import com.villo.truco.application.commands.ExecuteBotTurnCommand;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.in.AdvanceBotVsBotMatchUseCase;
import com.villo.truco.application.ports.in.ExecuteBotTurnUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.AdvanceBotMatchNotOwnerException;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.ports.MatchLockingRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class AdvanceBotVsBotMatchCommandHandler implements AdvanceBotVsBotMatchUseCase {

  private final BotVsBotMatchRegistry botVsBotMatchRegistry;
  private final MatchLockingRepository matchLockingRepository;
  private final ExecuteBotTurnUseCase executeBotTurnUseCase;

  public AdvanceBotVsBotMatchCommandHandler(final BotVsBotMatchRegistry botVsBotMatchRegistry,
      final MatchLockingRepository matchLockingRepository,
      final ExecuteBotTurnUseCase executeBotTurnUseCase) {

    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
    this.matchLockingRepository = Objects.requireNonNull(matchLockingRepository);
    this.executeBotTurnUseCase = Objects.requireNonNull(executeBotTurnUseCase);
  }

  @Override
  public Void handle(final AdvanceBotVsBotMatchCommand command) {

    final var owner = this.botVsBotMatchRegistry.findOwnerByMatchId(command.matchId());
    if (owner.isEmpty() || !owner.get().equals(command.ownerId())) {
      throw new AdvanceBotMatchNotOwnerException();
    }

    final var match = this.matchLockingRepository.findByIdForUpdate(command.matchId())
        .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    if (match.isFinished()) {
      return null;
    }

    final var botToAct = this.resolveBotToAct(match);
    if (botToAct == null) {
      return null;
    }

    this.executeBotTurnUseCase.handle(new ExecuteBotTurnCommand(command.matchId(), botToAct));

    return null;
  }

  private PlayerId resolveBotToAct(final Match match) {

    if (match.getDecisionViewFor(match.getPlayerOne()).hasAvailableActions()) {
      return match.getPlayerOne();
    }
    if (match.getDecisionViewFor(match.getPlayerTwo()).hasAvailableActions()) {
      return match.getPlayerTwo();
    }
    return null;
  }

}
