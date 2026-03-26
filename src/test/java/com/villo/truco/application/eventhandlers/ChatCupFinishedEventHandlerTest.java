package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.out.CupEventContext;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatCupFinishedEventHandler")
class ChatCupFinishedEventHandlerTest {

    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;
    private ChatCupFinishedEventHandler handler;

    @BeforeEach
    void setUp() {

        this.chatRepository = mock(ChatRepository.class);
        this.chatQueryRepository = mock(ChatQueryRepository.class);
        this.handler = new ChatCupFinishedEventHandler(this.chatRepository,
            this.chatQueryRepository);
    }

    @Test
    @DisplayName("eventType es CupFinishedEvent")
    void eventTypeIsCupFinishedEvent() {

        assertThat(this.handler.eventType()).isEqualTo(CupFinishedEvent.class);
    }

    @Test
    @DisplayName("elimina chat en CupFinishedEvent")
    void deletesChat() {

        final var cupId = CupId.generate();
        final var chatId = ChatId.generate();
        final var existingChat = mock(Chat.class);
        when(existingChat.getId()).thenReturn(chatId);
        when(this.chatQueryRepository.findByParentTypeAndParentId(
            eq(ChatParentType.CUP), eq(cupId.value().toString())))
            .thenReturn(Optional.of(existingChat));

        this.handler.handle(new CupFinishedEvent(cupId, PlayerId.generate()),
            new CupEventContext(cupId, List.of()));

        verify(this.chatRepository).delete(chatId);
    }

    @Test
    @DisplayName("no falla si no existe chat")
    void doesNothingWhenChatAbsent() {

        final var cupId = CupId.generate();
        when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any()))
            .thenReturn(Optional.empty());

        this.handler.handle(new CupFinishedEvent(cupId, PlayerId.generate()),
            new CupEventContext(cupId, List.of()));

        verify(this.chatRepository, never()).delete(any());
    }

}
