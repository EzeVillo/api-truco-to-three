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
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMatchFinishedEventHandler")
class ChatMatchFinishedEventHandlerTest {

    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;
    private ChatMatchFinishedEventHandler handler;

    @BeforeEach
    void setUp() {

        this.chatRepository = mock(ChatRepository.class);
        this.chatQueryRepository = mock(ChatQueryRepository.class);
        this.handler = new ChatMatchFinishedEventHandler(this.chatRepository,
            this.chatQueryRepository);
    }

    @Test
    @DisplayName("eventType es MatchFinishedEvent")
    void eventTypeIsMatchFinishedEvent() {

        assertThat(this.handler.eventType()).isEqualTo(MatchFinishedEvent.class);
    }

    @Test
    @DisplayName("elimina chat en MatchFinishedEvent")
    void deletesChat() {

        final var matchId = MatchId.generate();
        final var chatId = ChatId.generate();
        final var existingChat = mock(Chat.class);
        when(existingChat.getId()).thenReturn(chatId);
        when(this.chatQueryRepository.findByParentTypeAndParentId(
            eq(ChatParentType.MATCH), eq(matchId.value().toString())))
            .thenReturn(Optional.of(existingChat));

        this.handler.handle(new MatchFinishedEvent(PlayerSeat.PLAYER_ONE, 2, 1),
            new MatchEventContext(matchId, PlayerId.generate(), PlayerId.generate()));

        verify(this.chatRepository).delete(chatId);
    }

    @Test
    @DisplayName("no falla si no existe chat")
    void doesNothingWhenChatAbsent() {

        final var matchId = MatchId.generate();
        when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any()))
            .thenReturn(Optional.empty());

        this.handler.handle(new MatchFinishedEvent(PlayerSeat.PLAYER_ONE, 2, 0),
            new MatchEventContext(matchId, PlayerId.generate(), PlayerId.generate()));

        verify(this.chatRepository, never()).delete(any());
    }

}
