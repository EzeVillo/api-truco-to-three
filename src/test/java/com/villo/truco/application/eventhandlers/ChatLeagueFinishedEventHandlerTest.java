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
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatLeagueFinishedEventHandler")
class ChatLeagueFinishedEventHandlerTest {

    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;
    private ChatLeagueFinishedEventHandler handler;

    @BeforeEach
    void setUp() {

        this.chatRepository = mock(ChatRepository.class);
        this.chatQueryRepository = mock(ChatQueryRepository.class);
        this.handler = new ChatLeagueFinishedEventHandler(this.chatRepository,
            this.chatQueryRepository);
    }

    @Test
    @DisplayName("eventType es LeagueFinishedEvent")
    void eventTypeIsLeagueFinishedEvent() {

        assertThat(this.handler.eventType()).isEqualTo(LeagueFinishedEvent.class);
    }

    @Test
    @DisplayName("elimina chat en LeagueFinishedEvent")
    void deletesChat() {

        final var leagueId = LeagueId.generate();
        final var chatId = ChatId.generate();
        final var existingChat = mock(Chat.class);
        when(existingChat.getId()).thenReturn(chatId);
        when(this.chatQueryRepository.findByParentTypeAndParentId(
            eq(ChatParentType.LEAGUE), eq(leagueId.value().toString())))
            .thenReturn(Optional.of(existingChat));

        this.handler.handle(new LeagueFinishedEvent(leagueId, List.of(PlayerId.generate())),
            new LeagueEventContext(leagueId, List.of()));

        verify(this.chatRepository).delete(chatId);
    }

    @Test
    @DisplayName("no falla si no existe chat")
    void doesNothingWhenChatAbsent() {

        final var leagueId = LeagueId.generate();
        when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any()))
            .thenReturn(Optional.empty());

        this.handler.handle(new LeagueFinishedEvent(leagueId, List.of(PlayerId.generate())),
            new LeagueEventContext(leagueId, List.of()));

        verify(this.chatRepository, never()).delete(any());
    }

}
