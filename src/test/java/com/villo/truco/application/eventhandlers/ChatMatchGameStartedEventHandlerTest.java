package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMatchGameStartedEventHandler")
class ChatMatchGameStartedEventHandlerTest {

  private BotRegistry botRegistry;
  private ChatRepository chatRepository;
  private ChatQueryRepository chatQueryRepository;
  private ChatEventNotifier chatEventNotifier;
  private ChatMatchGameStartedEventHandler handler;

  @BeforeEach
  void setUp() {

    this.botRegistry = mock(BotRegistry.class);
    this.chatRepository = mock(ChatRepository.class);
    this.chatQueryRepository = mock(ChatQueryRepository.class);
    this.chatEventNotifier = mock(ChatEventNotifier.class);
    this.handler = new ChatMatchGameStartedEventHandler(this.botRegistry, this.chatRepository,
        this.chatQueryRepository, this.chatEventNotifier);
  }

  @Test
  @DisplayName("eventType es GameStartedEvent")
  void eventTypeIsGameStartedEvent() {

    assertThat(this.handler.eventType()).isEqualTo(GameStartedEvent.class);
  }

  @Test
  @DisplayName("crea chat en GameStartedEvent con gameNumber=1")
  void createsOnFirstGame() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    when(this.botRegistry.isBot(playerOne)).thenReturn(false);
    when(this.botRegistry.isBot(playerTwo)).thenReturn(false);
    when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any())).thenReturn(
        Optional.empty());

    this.handler.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));

    verify(this.chatRepository).save(any(Chat.class));
    verify(this.chatEventNotifier).publishDomainEvents(any());
  }

  @Test
  @DisplayName("no crea chat para gameNumber>1")
  void ignoresSubsequentGames() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    this.handler.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 2));

    verify(this.chatQueryRepository, never()).findByParentTypeAndParentId(any(), any());
    verify(this.chatRepository, never()).save(any());
    verify(this.chatEventNotifier, never()).publishDomainEvents(any());
  }

  @Test
  @DisplayName("no crea chat si uno de los participantes es bot")
  void ignoresMatchesAgainstBots() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    when(this.botRegistry.isBot(playerOne)).thenReturn(false);
    when(this.botRegistry.isBot(playerTwo)).thenReturn(true);

    this.handler.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));

    verify(this.chatQueryRepository, never()).findByParentTypeAndParentId(any(), any());
    verify(this.chatRepository, never()).save(any());
    verify(this.chatEventNotifier, never()).publishDomainEvents(any());
  }

  @Test
  @DisplayName("no crea chat duplicado")
  void doesNotCreateDuplicate() {

    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var existingChat = mock(Chat.class);
    when(this.botRegistry.isBot(playerOne)).thenReturn(false);
    when(this.botRegistry.isBot(playerTwo)).thenReturn(false);
    when(this.chatQueryRepository.findByParentTypeAndParentId(eq(ChatParentType.MATCH),
        eq(matchId.value().toString()))).thenReturn(Optional.of(existingChat));

    this.handler.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));

    verify(this.chatRepository, never()).save(any());
    verify(this.chatEventNotifier, never()).publishDomainEvents(any());
  }

}
