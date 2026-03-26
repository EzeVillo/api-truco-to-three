package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMatchGameStartedEventHandler")
class ChatMatchGameStartedEventHandlerTest {

    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;
    private ChatMatchGameStartedEventHandler handler;

    @BeforeEach
    void setUp() {

        this.chatRepository = mock(ChatRepository.class);
        this.chatQueryRepository = mock(ChatQueryRepository.class);
        this.handler = new ChatMatchGameStartedEventHandler(this.chatRepository,
            this.chatQueryRepository);
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
        final var context = new MatchEventContext(matchId, PlayerId.generate(),
            PlayerId.generate());
        when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any()))
            .thenReturn(Optional.empty());

        this.handler.handle(new GameStartedEvent(1), context);

        verify(this.chatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("no crea chat para gameNumber>1")
    void ignoresSubsequentGames() {

        final var matchId = MatchId.generate();
        final var context = new MatchEventContext(matchId, PlayerId.generate(),
            PlayerId.generate());

        this.handler.handle(new GameStartedEvent(2), context);

        verify(this.chatQueryRepository, never()).findByParentTypeAndParentId(any(), any());
        verify(this.chatRepository, never()).save(any());
    }

    @Test
    @DisplayName("no crea chat duplicado")
    void doesNotCreateDuplicate() {

        final var matchId = MatchId.generate();
        final var existingChat = mock(Chat.class);
        when(this.chatQueryRepository.findByParentTypeAndParentId(
            eq(ChatParentType.MATCH), eq(matchId.value().toString())))
            .thenReturn(Optional.of(existingChat));

        this.handler.handle(new GameStartedEvent(1),
            new MatchEventContext(matchId, PlayerId.generate(), PlayerId.generate()));

        verify(this.chatRepository, never()).save(any());
    }

}
