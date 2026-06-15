package com.villo.truco.application.usecases.recording;

import com.villo.truco.application.commands.MatchActionCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.domain.model.gameplay.ActorSeat;
import com.villo.truco.domain.model.gameplay.ActorType;
import com.villo.truco.domain.model.gameplay.RecordedDecision;
import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.ports.GameplayRecorderPort;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decora los 6 use cases de acción para registrar, de forma transparente y POR FUERA del pipeline
 * transaccional, la decisión jugable resultante. Tras delegar (la jugada ya commiteó), re-lee la
 * partida, extrae el {@link MatchSnapshot} resultante y persiste un {@link RecordedDecision} vía el
 * puerto de salida. Un fallo de registro se traga y loguea: nunca interrumpe ni revierte la jugada
 * (FR-009/FR-010). Como humano y bot ejecutan a través de los mismos beans, ambos quedan registrados.
 */
public final class GameplayRecordingDecorator {

  private static final Logger LOGGER = LoggerFactory.getLogger(GameplayRecordingDecorator.class);

  private static final int SCHEMA_VERSION = 1;

  private final MatchQueryRepository matchQueryRepository;
  private final BotRegistry botRegistry;
  private final RecordedActionFactory recordedActionFactory;
  private final GameplayRecorderPort gameplayRecorderPort;

  public GameplayRecordingDecorator(final MatchQueryRepository matchQueryRepository,
      final BotRegistry botRegistry, final RecordedActionFactory recordedActionFactory,
      final GameplayRecorderPort gameplayRecorderPort) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.recordedActionFactory = Objects.requireNonNull(recordedActionFactory);
    this.gameplayRecorderPort = Objects.requireNonNull(gameplayRecorderPort);
  }

  public <C extends MatchActionCommand, R> UseCase<C, R> decorate(final UseCase<C, R> delegate) {

    Objects.requireNonNull(delegate, "delegate is required");

    return command -> {
      final R result = delegate.handle(command);
      this.safeRecord(command);
      return result;
    };
  }

  private void safeRecord(final MatchActionCommand command) {

    final MatchId matchId = command.matchId();

    try {
      this.matchQueryRepository.findById(matchId).map(MatchSnapshotExtractor::extract)
          .ifPresent(snapshot -> this.gameplayRecorderPort.record(toDecision(command, snapshot)));
    } catch (final RuntimeException exception) {
      LOGGER.warn("No se pudo registrar la decisión de la partida {}: {}",
          matchId != null ? matchId.value() : null, exception.getMessage(), exception);
    }
  }

  private RecordedDecision toDecision(final MatchActionCommand command,
      final MatchSnapshot snapshot) {

    final ActorType actorType =
        this.botRegistry.isBot(command.playerId()) ? ActorType.BOT : ActorType.HUMAN;
    final ActorSeat actorSeat = command.playerId().equals(snapshot.playerOne()) ? ActorSeat.PLAYER_ONE
        : ActorSeat.PLAYER_TWO;

    return new RecordedDecision(snapshot.id(), snapshot.stateVersion(), snapshot.gameNumber(),
        snapshot.roundNumber(), actorSeat, actorType, this.recordedActionFactory.from(command),
        snapshot, Instant.now(), SCHEMA_VERSION);
  }

}
