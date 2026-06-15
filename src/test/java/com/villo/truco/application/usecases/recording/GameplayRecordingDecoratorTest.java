package com.villo.truco.application.usecases.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.events.RecordedDecisionCaptured;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorSeat;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorType;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedActionType;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("GameplayRecordingDecorator")
class GameplayRecordingDecoratorTest {

  private final MatchQueryRepository matchQueryRepository = mock(MatchQueryRepository.class);
  private final BotRegistry botRegistry = mock(BotRegistry.class);
  private final ApplicationEventPublisher applicationEventPublisher = mock(
      ApplicationEventPublisher.class);

  private final GameplayRecordingDecorator decorator = new GameplayRecordingDecorator(
      this.matchQueryRepository, this.botRegistry, new RecordedActionFactory(),
      this.applicationEventPublisher, true);

  private Match newMatch() {

    return Match.create(PlayerId.generate(), MatchRules.fromGamesToPlay(GamesToPlay.of(3), true),
        Visibility.PRIVATE);
  }

  @Test
  @DisplayName("publica la decisión capturada tras una jugada exitosa del humano")
  void publishesHumanDecisionAfterSuccess() {

    final var match = this.newMatch();
    final var playerId = match.getPlayerOne();
    final var command = new FoldCommand(match.getId(), playerId);
    when(this.matchQueryRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(this.botRegistry.isBot(playerId)).thenReturn(false);

    final UseCase<FoldCommand, Void> delegate = c -> null;

    this.decorator.decorate(delegate).handle(command);

    final var captor = ArgumentCaptor.forClass(RecordedDecisionCaptured.class);
    verify(this.applicationEventPublisher).publish(captor.capture());
    final var decision = captor.getValue().decision();
    assertThat(decision.matchId()).isEqualTo(match.getId());
    assertThat(decision.stateVersion()).isEqualTo(match.getStateVersion());
    assertThat(decision.actorType()).isEqualTo(ActorType.HUMAN);
    assertThat(decision.actorSeat()).isEqualTo(ActorSeat.PLAYER_ONE);
    assertThat(decision.action().type()).isEqualTo(RecordedActionType.FOLD);
    assertThat(decision.snapshotBefore()).isNotNull();
    assertThat(decision.snapshotAfter()).isNotNull();
    assertThat(decision.context()).isNotNull();
    assertThat(decision.schemaVersion()).isEqualTo(2);
  }

  @Test
  @DisplayName("marca la decisión como BOT cuando el actor es un bot")
  void publishesBotActor() {

    final var match = this.newMatch();
    final var playerId = match.getPlayerOne();
    final var command = new FoldCommand(match.getId(), playerId);
    when(this.matchQueryRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(this.botRegistry.isBot(playerId)).thenReturn(true);

    this.decorator.decorate((UseCase<FoldCommand, Void>) c -> null).handle(command);

    final var captor = ArgumentCaptor.forClass(RecordedDecisionCaptured.class);
    verify(this.applicationEventPublisher).publish(captor.capture());
    assertThat(captor.getValue().decision().actorType()).isEqualTo(ActorType.BOT);
  }

  @Test
  @DisplayName("no captura si la jugada delegada falla y propaga el error")
  void doesNotCaptureWhenDelegateThrows() {

    final var match = this.newMatch();
    final var command = new FoldCommand(match.getId(), match.getPlayerOne());
    when(this.matchQueryRepository.findById(match.getId())).thenReturn(Optional.of(match));

    final UseCase<FoldCommand, Void> delegate = c -> {
      throw new IllegalStateException("jugada inválida");
    };

    assertThatThrownBy(() -> this.decorator.decorate(delegate).handle(command)).isInstanceOf(
        IllegalStateException.class);

    verifyNoInteractions(this.applicationEventPublisher);
  }

  @Test
  @DisplayName("traga y no propaga si la captura falla")
  void swallowsCaptureFailure() {

    final var match = this.newMatch();
    final var playerId = match.getPlayerOne();
    final var command = new FoldCommand(match.getId(), playerId);
    when(this.matchQueryRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(this.botRegistry.isBot(playerId)).thenReturn(false);
    doThrow(new RuntimeException("publicación caída")).when(this.applicationEventPublisher)
        .publish(any());

    final UseCase<FoldCommand, Void> delegate = c -> null;

    assertThat(this.decorator.decorate(delegate).handle(command)).isNull();
  }

  @Test
  @DisplayName("no lee ni captura cuando el flag de grabación está deshabilitado")
  void skipsReadAndCaptureWhenDisabled() {

    final var disabledDecorator = new GameplayRecordingDecorator(this.matchQueryRepository,
        this.botRegistry, new RecordedActionFactory(), this.applicationEventPublisher, false);

    final var match = this.newMatch();
    final var command = new FoldCommand(match.getId(), match.getPlayerOne());
    final var delegateResult = new Object();

    final UseCase<FoldCommand, Object> delegate = c -> delegateResult;

    assertThat(disabledDecorator.decorate(delegate).handle(command)).isSameAs(delegateResult);

    verifyNoInteractions(this.matchQueryRepository);
    verifyNoInteractions(this.applicationEventPublisher);
  }

}
