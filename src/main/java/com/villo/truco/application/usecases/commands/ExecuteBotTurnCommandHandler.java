package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.ExecuteBotTurnCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.ExecuteBotTurnUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.domain.model.bot.BotDecisionEngine;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.ports.MatchQueryRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecuteBotTurnCommandHandler implements ExecuteBotTurnUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteBotTurnCommandHandler.class);

  private final BotRegistry botRegistry;
  private final MatchQueryRepository matchQueryRepository;
  private final PlayCardUseCase playCardUseCase;
  private final CallTrucoUseCase callTrucoUseCase;
  private final RespondTrucoUseCase respondTrucoUseCase;
  private final CallEnvidoUseCase callEnvidoUseCase;
  private final RespondEnvidoUseCase respondEnvidoUseCase;
  private final FoldUseCase foldUseCase;

  public ExecuteBotTurnCommandHandler(final BotRegistry botRegistry,
      final MatchQueryRepository matchQueryRepository, final PlayCardUseCase playCardUseCase,
      final CallTrucoUseCase callTrucoUseCase, final RespondTrucoUseCase respondTrucoUseCase,
      final CallEnvidoUseCase callEnvidoUseCase, final RespondEnvidoUseCase respondEnvidoUseCase,
      final FoldUseCase foldUseCase) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.playCardUseCase = Objects.requireNonNull(playCardUseCase);
    this.callTrucoUseCase = Objects.requireNonNull(callTrucoUseCase);
    this.respondTrucoUseCase = Objects.requireNonNull(respondTrucoUseCase);
    this.callEnvidoUseCase = Objects.requireNonNull(callEnvidoUseCase);
    this.respondEnvidoUseCase = Objects.requireNonNull(respondEnvidoUseCase);
    this.foldUseCase = Objects.requireNonNull(foldUseCase);
  }

  @Override
  public Void handle(final ExecuteBotTurnCommand command) {

    final var profileOpt = this.botRegistry.getProfile(command.botPlayerId());
    if (profileOpt.isEmpty()) {
      LOGGER.warn("ExecuteBotTurn for unknown bot {}", command.botPlayerId());
      return null;
    }
    final var profile = profileOpt.get();

    final var matchOpt = this.matchQueryRepository.findById(command.matchId());
    if (matchOpt.isEmpty()) {
      LOGGER.warn("Match {} not found for bot turn", command.matchId());
      return null;
    }
    final var match = matchOpt.get();

    if (match.isFinished()) {
      LOGGER.debug("Match {} already finished, bot {} skips", command.matchId(),
          command.botPlayerId());
      return null;
    }

    final var decisionView = match.getDecisionViewFor(command.botPlayerId());
    if (!decisionView.hasAvailableActions()) {
      LOGGER.debug("Bot {} has no available actions in match {}", command.botPlayerId(),
          command.matchId());
      return null;
    }

    final var view = MatchToBotACL.translate(decisionView);
    final var engine = new BotDecisionEngine(profile.personality());
    final var action = engine.decide(view);

    LOGGER.debug("Bot {} decided {} in match {}", command.botPlayerId(),
        action.getClass().getSimpleName(), command.matchId());

    this.dispatch(action, command);

    return null;
  }

  private void dispatch(final BotAction action, final ExecuteBotTurnCommand command) {

    switch (action) {
      case BotAction.PlayCard pc -> this.playCardUseCase.handle(
          new PlayCardCommand(command.matchId(), command.botPlayerId(),
              MatchToBotACL.toCard(pc.card())));
      case BotAction.CallTruco ignored -> this.callTrucoUseCase.handle(
          new CallTrucoCommand(command.matchId(), command.botPlayerId()));
      case BotAction.RespondTruco rt -> this.respondTrucoUseCase.handle(
          new RespondTrucoCommand(command.matchId(), command.botPlayerId(),
              MatchToBotACL.toTrucoResponse(rt.response())));
      case BotAction.CallEnvido ce -> this.callEnvidoUseCase.handle(
          new CallEnvidoCommand(command.matchId(), command.botPlayerId(),
              MatchToBotACL.toEnvidoCall(ce.call())));
      case BotAction.RespondEnvido re -> this.respondEnvidoUseCase.handle(
          new RespondEnvidoCommand(command.matchId(), command.botPlayerId(),
              MatchToBotACL.toEnvidoResponse(re.response())));
      case BotAction.Fold ignored ->
          this.foldUseCase.handle(new FoldCommand(command.matchId(), command.botPlayerId()));
    }
  }

}
