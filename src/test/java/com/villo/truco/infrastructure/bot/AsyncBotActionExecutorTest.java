package com.villo.truco.infrastructure.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.ExecuteBotTurnCommand;
import com.villo.truco.application.events.BotTurnRequired;
import com.villo.truco.application.ports.in.ExecuteBotTurnUseCase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncBotActionExecutor")
class AsyncBotActionExecutorTest {

  private static ExecuteBotTurnUseCase useCaseRecording(
      final List<ExecuteBotTurnCommand> executed) {

    return command -> {
      executed.add(command);
      return null;
    };
  }

  @Test
  @DisplayName("sin transaccion ejecuta el turno del bot inmediatamente")
  void executesImmediatelyWithoutTransactionSynchronization() {

    final var executed = new ArrayList<ExecuteBotTurnCommand>();
    final var executor = new RecordingExecutor();
    final var handler = new AsyncBotActionExecutor(useCaseRecording(executed), executor);
    final var event = new BotTurnRequired(MatchId.generate(), PlayerId.generate());

    handler.handle(event);

    assertThat(executor.submitted).hasSize(1);
    executor.runAll();

    assertThat(executed).singleElement().satisfies(command -> {
      assertThat(command.matchId()).isEqualTo(event.matchId());
      assertThat(command.botPlayerId()).isEqualTo(event.botPlayerId());
    });
  }

  private static final class RecordingExecutor implements Executor {

    private final List<Runnable> submitted = new ArrayList<>();

    @Override
    public void execute(final Runnable command) {

      this.submitted.add(command);
    }

    private void runAll() {

      new ArrayList<>(this.submitted).forEach(Runnable::run);
    }

  }

}
