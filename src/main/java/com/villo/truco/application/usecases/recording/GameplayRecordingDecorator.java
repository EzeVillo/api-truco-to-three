package com.villo.truco.application.usecases.recording;

import com.villo.truco.application.commands.MatchActionCommand;
import com.villo.truco.application.events.RecordedDecisionCaptured;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorSeat;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorType;
import com.villo.truco.domain.model.gameplay.valueobjects.DecisionContext;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedDecision;
import com.villo.truco.domain.model.match.CardEvaluationService;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.model.match.valueobjects.ActionType;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameplayRecordingDecorator {

  private static final Logger LOGGER = LoggerFactory.getLogger(GameplayRecordingDecorator.class);

  private static final int SCHEMA_VERSION = 2;

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
      final var before = this.safeRead(command.matchId());
      final R result = delegate.handle(command);
      this.safeCapture(command, before);
      return result;
    };
  }

  private Match safeRead(final MatchId matchId) {

    try {
      return this.matchQueryRepository.findById(matchId).orElse(null);
    } catch (final RuntimeException exception) {
      LOGGER.warn("No se pudo leer el estado previo de la partida {}: {}",
          matchId != null ? matchId.value() : null, exception.getMessage(), exception);
      return null;
    }
  }

  private void safeCapture(final MatchActionCommand command, final Match before) {

    if (before == null) {
      return;
    }

    final var matchId = command.matchId();

    try {
      this.matchQueryRepository.findById(matchId).ifPresent(after -> this.applicationEventPublisher
          .publish(new RecordedDecisionCaptured(this.toDecision(command, before, after))));
    } catch (final RuntimeException exception) {
      LOGGER.warn("No se pudo capturar la decisión de la partida {}: {}",
          matchId != null ? matchId.value() : null, exception.getMessage(), exception);
    }
  }

  private RecordedDecision toDecision(final MatchActionCommand command, final Match before,
      final Match after) {

    final var playerId = command.playerId();
    final var beforeSnapshot = MatchSnapshotExtractor.extract(before);
    final var afterSnapshot = MatchSnapshotExtractor.extract(after);

    final var actorType = this.botRegistry.isBot(playerId) ? ActorType.BOT : ActorType.HUMAN;
    final var isPlayerOne = playerId.equals(beforeSnapshot.playerOne());
    final var actorSeat = isPlayerOne ? ActorSeat.PLAYER_ONE : ActorSeat.PLAYER_TWO;
    final var rivalId = isPlayerOne ? before.getPlayerTwo() : before.getPlayerOne();

    final var context = buildContext(before, beforeSnapshot, afterSnapshot, playerId, rivalId,
        isPlayerOne, actorSeat);

    return new RecordedDecision(afterSnapshot.id(), afterSnapshot.stateVersion(),
        beforeSnapshot.gameNumber(), beforeSnapshot.roundNumber(), actorSeat, actorType,
        this.recordedActionFactory.from(command), beforeSnapshot, afterSnapshot, context,
        Instant.now(), SCHEMA_VERSION);
  }

  private static DecisionContext buildContext(final Match before, final MatchSnapshot beforeSnapshot,
      final MatchSnapshot afterSnapshot, final PlayerId playerId, final PlayerId rivalId,
      final boolean isPlayerOne, final ActorSeat actorSeat) {

    final var scoreActorBefore =
        isPlayerOne ? beforeSnapshot.scorePlayerOne() : beforeSnapshot.scorePlayerTwo();
    final var scoreOppBefore =
        isPlayerOne ? beforeSnapshot.scorePlayerTwo() : beforeSnapshot.scorePlayerOne();
    final var scoreActorAfter =
        isPlayerOne ? afterSnapshot.scorePlayerOne() : afterSnapshot.scorePlayerTwo();
    final var scoreOppAfter =
        isPlayerOne ? afterSnapshot.scorePlayerTwo() : afterSnapshot.scorePlayerOne();
    final var gamesWonActor =
        isPlayerOne ? beforeSnapshot.gamesWonPlayerOne() : beforeSnapshot.gamesWonPlayerTwo();
    final var gamesWonOpp =
        isPlayerOne ? beforeSnapshot.gamesWonPlayerTwo() : beforeSnapshot.gamesWonPlayerOne();

    final var tantosActor = envidoOf(before, playerId);
    final var tantosOpp = envidoOf(before, rivalId);

    final var round = beforeSnapshot.currentRound();
    final var manoSeat = round != null ? seatOf(round.mano(), beforeSnapshot) : null;
    final var turnSeat = round != null ? seatOf(round.currentTurn(), beforeSnapshot) : null;
    final var isMano = manoSeat != null && manoSeat == actorSeat;

    final var availableActions = before.getAvailableActions(playerId);
    final var forced = availableActions.size() == 1;
    final var quieroYMeVoyDisponible = availableActions.stream()
        .anyMatch(action -> action.type() == ActionType.RESPOND_TRUCO
            && TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO.name().equals(action.parameter()));
    final var puedeIrseAlMazo =
        availableActions.stream().anyMatch(action -> action.type() == ActionType.FOLD);
    final var envidoDisponible =
        availableActions.stream().anyMatch(action -> action.type() == ActionType.CALL_ENVIDO);
    final var availableActionLabels = availableActions.stream()
        .map(GameplayRecordingDecorator::label).toList();

    final var observable = before.getDecisionViewFor(playerId);

    return new DecisionContext(scoreActorBefore, scoreActorAfter, scoreOppBefore, scoreOppAfter,
        gamesWonActor, gamesWonOpp, tantosActor, tantosOpp, isMano, manoSeat, turnSeat, forced,
        availableActionLabels, quieroYMeVoyDisponible, puedeIrseAlMazo, envidoDisponible,
        observable);
  }

  private static String label(final AvailableAction action) {

    return action.parameter() == null ? action.type().name()
        : action.type().name() + ":" + action.parameter();
  }

  private static int envidoOf(final Match match, final PlayerId playerId) {

    final var cards = match.getOriginalCardsOf(playerId);
    return cards.isEmpty() ? 0 : CardEvaluationService.envidoScore(cards);
  }

  private static ActorSeat seatOf(final PlayerId playerId, final MatchSnapshot snapshot) {

    if (playerId == null) {
      return null;
    }
    return playerId.equals(snapshot.playerOne()) ? ActorSeat.PLAYER_ONE : ActorSeat.PLAYER_TWO;
  }

}
