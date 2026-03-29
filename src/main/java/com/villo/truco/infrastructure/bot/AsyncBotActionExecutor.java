package com.villo.truco.infrastructure.bot;

import com.villo.truco.application.commands.ExecuteBotTurnCommand;
import com.villo.truco.application.events.BotTurnRequired;
import com.villo.truco.application.ports.in.ExecuteBotTurnUseCase;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class AsyncBotActionExecutor implements ApplicationEventHandler<BotTurnRequired> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncBotActionExecutor.class);

  private final ExecuteBotTurnUseCase executeBotTurnUseCase;
  private final Executor executor;

  public AsyncBotActionExecutor(final ExecuteBotTurnUseCase executeBotTurnUseCase,
      final Executor executor) {

    this.executeBotTurnUseCase = Objects.requireNonNull(executeBotTurnUseCase);
    this.executor = Objects.requireNonNull(executor);
  }

  @Override
  public Class<BotTurnRequired> eventType() {

    return BotTurnRequired.class;
  }

  @Override
  public void handle(final BotTurnRequired event) {

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {

          AsyncBotActionExecutor.this.submit(event);
        }
      });
      return;
    }

    this.submit(event);
  }

  private void executeAsync(final BotTurnRequired event) {

    try {
      this.executeBotTurnUseCase.handle(
          new ExecuteBotTurnCommand(event.matchId(), event.botPlayerId()));
    } catch (final Exception ex) {
      LOGGER.error("Bot {} action in match {} was rejected ({})", event.botPlayerId(),
          event.matchId(), ex.getMessage());
    }
  }

  private void submit(final BotTurnRequired event) {

    this.executor.execute(() -> this.executeAsync(event));
  }

}
