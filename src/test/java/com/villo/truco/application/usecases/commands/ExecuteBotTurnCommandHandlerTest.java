package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.ExecuteBotTurnCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExecuteBotTurnCommandHandlerTest {

  @Test
  @DisplayName("usa la vista de decision del match y despacha exactamente una accion")
  void dispatchesExactlyOneBotAction() {

    final var botPlayer = PlayerId.generate();
    final var rivalPlayer = PlayerId.generate();
    final var match = Match.createReady(botPlayer, rivalPlayer,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    match.startMatch(botPlayer);
    match.startMatch(rivalPlayer);

    final var dispatchedCommands = new ArrayList<>();
    final PlayCardUseCase playCardUseCase = command -> {
      dispatchedCommands.add(command);
      return command.matchId();
    };
    final CallTrucoUseCase callTrucoUseCase = command -> {
      dispatchedCommands.add(command);
      return command.matchId();
    };
    final RespondTrucoUseCase respondTrucoUseCase = command -> {
      dispatchedCommands.add(command);
      return command.matchId();
    };
    final CallEnvidoUseCase callEnvidoUseCase = command -> {
      dispatchedCommands.add(command);
      return command.matchId();
    };
    final RespondEnvidoUseCase respondEnvidoUseCase = command -> {
      dispatchedCommands.add(command);
      return null;
    };

    final var handler = new ExecuteBotTurnCommandHandler(
        registryWith(botPlayer),
        repositoryWith(match),
        playCardUseCase,
        callTrucoUseCase,
        respondTrucoUseCase,
        callEnvidoUseCase,
        respondEnvidoUseCase);

    handler.handle(new ExecuteBotTurnCommand(match.getId(), botPlayer));

    assertThat(dispatchedCommands).singleElement().satisfies(
        command -> assertDispatchedFor(command, match.getId(), botPlayer));
  }

  private static BotRegistry registryWith(final PlayerId botPlayerId) {

    final var profile = new BotProfile(botPlayerId, "bot",
        new BotPersonality(50, 50, 50, 50, 50));

    return new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId playerId) {

        return botPlayerId.equals(playerId);
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId playerId) {

        return botPlayerId.equals(playerId) ? Optional.of(profile) : Optional.empty();
      }

      @Override
      public List<BotProfile> getAll() {

        return List.of(profile);
      }

      @Override
      public void register(final BotProfile ignored) {

      }
    };
  }

  private static MatchQueryRepository repositoryWith(final Match match) {

    return new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.of(match);
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public boolean hasUnfinishedMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }
    };
  }

  private static void assertDispatchedFor(final Object command, final MatchId matchId,
      final PlayerId botPlayerId) {

    if (command instanceof PlayCardCommand playCardCommand) {
      assertThat(playCardCommand.matchId()).isEqualTo(matchId);
      assertThat(playCardCommand.playerId()).isEqualTo(botPlayerId);
      return;
    }
    if (command instanceof CallTrucoCommand(MatchId id, PlayerId playerId)) {
      assertThat(id).isEqualTo(matchId);
      assertThat(playerId).isEqualTo(botPlayerId);
      return;
    }
    if (command instanceof RespondTrucoCommand respondTrucoCommand) {
      assertThat(respondTrucoCommand.matchId()).isEqualTo(matchId);
      assertThat(respondTrucoCommand.playerId()).isEqualTo(botPlayerId);
      return;
    }
    if (command instanceof CallEnvidoCommand callEnvidoCommand) {
      assertThat(callEnvidoCommand.matchId()).isEqualTo(matchId);
      assertThat(callEnvidoCommand.playerId()).isEqualTo(botPlayerId);
      return;
    }
    if (command instanceof RespondEnvidoCommand respondEnvidoCommand) {
      assertThat(respondEnvidoCommand.matchId()).isEqualTo(matchId);
      assertThat(respondEnvidoCommand.playerId()).isEqualTo(botPlayerId);
      return;
    }

    throw new AssertionError("Unexpected command type: " + command.getClass().getName());
  }
}
