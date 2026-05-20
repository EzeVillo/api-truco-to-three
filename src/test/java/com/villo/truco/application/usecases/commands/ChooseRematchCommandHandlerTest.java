package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.ChooseRematchCommand;
import com.villo.truco.application.exceptions.RematchSessionNotFoundException;
import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChooseRematchCommandHandler")
class ChooseRematchCommandHandlerTest {

  @Mock
  private RematchSessionRepository repository;
  @Mock
  private RematchSessionEventNotifier eventNotifier;

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private MatchId originMatchId;
  private Instant now;

  @BeforeEach
  void setUp() {

    playerOne = PlayerId.generate();
    playerTwo = PlayerId.generate();
    originMatchId = MatchId.generate();
    now = Instant.now();
  }

  private RematchSession openSession() {

    return RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false, now,
        Duration.ofMinutes(2));
  }

  private RematchSession openSessionWithBotAsPlayerOne() {

    return RematchSession.open(originMatchId, playerOne, playerTwo, 2, true, false, now,
        Duration.ofMinutes(2));
  }

  private ChooseRematchCommandHandler handler() {

    return new ChooseRematchCommandHandler(repository, eventNotifier,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("saves session and publishes events when player chooses rematch")
  void savesAndPublishesOnChoose() {

    final var session = openSession();
    session.clearDomainEvents();
    when(repository.findByOriginMatchId(originMatchId)).thenReturn(Optional.of(session));

    handler().handle(new ChooseRematchCommand(originMatchId, playerOne));

    final var captor = ArgumentCaptor.forClass(RematchSession.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getPlayerOneChoice()).isEqualTo(RematchPlayerChoice.WANTS_REMATCH);
    verify(eventNotifier).publishDomainEvents(argThat(events -> !events.isEmpty()));
  }

  @Test
  @DisplayName("throws RematchSessionNotFoundException when session does not exist")
  void throwsWhenNotFound() {

    when(repository.findByOriginMatchId(originMatchId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> handler().handle(new ChooseRematchCommand(originMatchId, playerOne))).isInstanceOf(
        RematchSessionNotFoundException.class);
  }

  @Test
  @DisplayName("confirms session when both players choose")
  void confirmsWhenBothChoose() {

    final var session = openSession();
    session.clearDomainEvents();
    when(repository.findByOriginMatchId(originMatchId)).thenReturn(Optional.of(session));

    final var handler = handler();
    handler.handle(new ChooseRematchCommand(originMatchId, playerOne));
    handler.handle(new ChooseRematchCommand(originMatchId, playerTwo));

    assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CONFIRMED);
  }

  @Test
  @DisplayName("confirms session immediately when bot is player one and human (player two) chooses")
  void confirmsImmediatelyWhenBotIsPlayerOneAndHumanChooses() {

    final var session = openSessionWithBotAsPlayerOne();
    session.clearDomainEvents();
    when(repository.findByOriginMatchId(originMatchId)).thenReturn(Optional.of(session));

    handler().handle(new ChooseRematchCommand(originMatchId, playerTwo));

    final var captor = ArgumentCaptor.forClass(RematchSession.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(RematchSessionStatus.CONFIRMED);
    verify(eventNotifier).publishDomainEvents(argThat(events -> !events.isEmpty()));
  }

}
