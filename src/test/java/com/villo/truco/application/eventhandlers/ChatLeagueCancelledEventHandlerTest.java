package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatLeagueCancelledEventHandler")
class ChatLeagueCancelledEventHandlerTest {

    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;
    private ChatLeagueCancelledEventHandler handler;

    @BeforeEach
    void setUp() {

        this.chatRepository = mock(ChatRepository.class);
        this.chatQueryRepository = mock(ChatQueryRepository.class);
        this.handler = new ChatLeagueCancelledEventHandler(this.chatRepository,
            this.chatQueryRepository);
    }

    @Test
    @DisplayName("eventType es LeagueCancelledEvent")
    void eventTypeIsLeagueCancelledEvent() {

        assertThat(this.handler.eventType()).isEqualTo(LeagueCancelledEvent.class);
    }

    @Test
    @DisplayName("elimina chat en LeagueCancelledEvent")
    void deletesChat() {

        final var leagueId = LeagueId.generate();
        final var chatId = ChatId.generate();
        final var existingChat = mock(Chat.class);
        when(existingChat.getId()).thenReturn(chatId);
        when(this.chatQueryRepository.findByParentTypeAndParentId(
            eq(ChatParentType.LEAGUE), eq(leagueId.value().toString())))
            .thenReturn(Optional.of(existingChat));

        this.handler.handle(new LeagueCancelledEvent(leagueId),
            new LeagueEventContext(leagueId, List.of()));

        verify(this.chatRepository).delete(chatId);
    }

    @Test
    @DisplayName("no falla si no existe chat")
    void doesNothingWhenChatAbsent() {

        final var leagueId = LeagueId.generate();
        when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any()))
            .thenReturn(Optional.empty());

        this.handler.handle(new LeagueCancelledEvent(leagueId),
            new LeagueEventContext(leagueId, List.of()));

        verify(this.chatRepository, never()).delete(any());
    }

}
