package com.villo.truco.application.usecases.recording;

import com.villo.truco.application.commands.MatchActionCommand;
import com.villo.truco.application.events.RecordedDecisionCaptured;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorSeat;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorType;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedDecision;
import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.ports.MatchQueryRepository;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameplayRecordingDecorator {

  private static final Logger LOGGER = LoggerFactory.getLogger(GameplayRecordingDecorator.class);

  private static final int SCHEMA_VERSION = 1;

  private final MatchQueryRepository matchQueryRepository;
  private final BotRegistry botRegistry;
  private final RecordedActionFactory recordedActionFactory;
  private final ApplicationEventPublisher applicationEventPublisher;

  public GameplayRecordingDecorator(final MatchQueryRepository matchQueryRepository,
      final BotRegistry botRegistry, final RecordedActionFactory recordedActionFactory,
      final ApplicationEventPublisher applicationEventPublisher) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.recordedActionFactory = Objects.requireNonNull(recordedActionFactory);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  public <C extends MatchActionCommand, R> UseCase<C, R> decorate(final UseCase<C, R> delegate) {

    Objects.requireNonNull(delegate, "delegate is required");

    return command -> {
      final R result = delegate.handle(command);
      this.safeCapture(command);
      return result;
    };
  }

  private void safeCapture(final MatchActionCommand command) {

    final var matchId = command.matchId();

    try {
      this.matchQueryRepository.findById(matchId).map(MatchSnapshotExtractor::extract).ifPresent(
          snapshot -> this.applicationEventPublisher.publish(
              new RecordedDecisionCaptured(this.toDecision(command, snapshot))));
    } catch (final RuntimeException exception) {
      LOGGER.warn("No se pudo capturar la decisión de la partida {}: {}",
          matchId != null ? matchId.value() : null, exception.getMessage(), exception);
    }
  }

  private RecordedDecision toDecision(final MatchActionCommand command,
      final MatchSnapshot snapshot) {

    final var actorType =
        this.botRegistry.isBot(command.playerId()) ? ActorType.BOT : ActorType.HUMAN;
    final var actorSeat = command.playerId().equals(snapshot.playerOne()) ? ActorSeat.PLAYER_ONE
        : ActorSeat.PLAYER_TWO;

    return new RecordedDecision(snapshot.id(), snapshot.stateVersion(), snapshot.gameNumber(),
        snapshot.roundNumber(), actorSeat, actorType, this.recordedActionFactory.from(command),
        snapshot, Instant.now(), SCHEMA_VERSION);
  }

}
