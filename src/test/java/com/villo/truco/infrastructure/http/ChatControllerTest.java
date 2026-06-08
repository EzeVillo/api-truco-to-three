package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.ChatMessageDTO;
import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.dto.ChatSendStateDTO;
import com.villo.truco.application.dto.SendMessageResultDTO;
import com.villo.truco.application.ports.in.GetChatByParentUseCase;
import com.villo.truco.application.ports.in.GetChatMessagesUseCase;
import com.villo.truco.application.ports.in.SendMessageToParentUseCase;
import com.villo.truco.application.ports.in.SendMessageUseCase;
import com.villo.truco.domain.model.chat.exceptions.ChatRateLimitExceededException;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.http.dto.request.SendMessageRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("ChatController")
class ChatControllerTest {

  private static final String CHAT_ID = ChatId.generate().value().toString();
  private static final String PLAYER_ID = PlayerId.generate().value().toString();

  private final SendMessageUseCase sendMessage = mock(SendMessageUseCase.class);
  private final SendMessageToParentUseCase sendMessageToParent = mock(
      SendMessageToParentUseCase.class);
  private final GetChatMessagesUseCase getChatMessages = mock(GetChatMessagesUseCase.class);
  private final GetChatByParentUseCase getChatByParent = mock(GetChatByParentUseCase.class);
  private final ChatController controller = new ChatController(this.sendMessage,
      this.sendMessageToParent, this.getChatMessages, this.getChatByParent);
  private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

  private static Jwt jwt() {

    return Jwt.withTokenValue("token").header("alg", "none").subject(PLAYER_ID).build();
  }

  @Test
  @DisplayName("GET /api/chats/{chatId}/messages serializa sendState")
  void getChatMessagesSerializesSendState() {

    when(this.getChatMessages.handle(any())).thenReturn(
        new ChatMessagesDTO("chat-1", "MATCH", "parent-1",
            new ChatSendStateDTO(false, 1772768160123L),
            List.of(new ChatMessageDTO("message-1", "Jugador 1", "hola", 1772768158123L))));

    final var response = this.controller.getChatMessages(CHAT_ID, jwt());

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().sendState().canSendNow()).isFalse();
    assertThat(response.getBody().sendState().nextMessageAllowedAt()).isEqualTo(1772768160123L);
  }

  @Test
  @DisplayName("GET /api/chats/by-parent/{parentType}/{parentId} serializa sendState")
  void getChatByParentSerializesSendState() {

    when(this.getChatByParent.handle(any())).thenReturn(
        new ChatMessagesDTO("chat-1", "MATCH", "parent-1", new ChatSendStateDTO(true, null),
            List.of()));

    final var response = this.controller.getChatByParent("MATCH", "parent-1", jwt());

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().sendState().canSendNow()).isTrue();
    assertThat(response.getBody().sendState().nextMessageAllowedAt()).isNull();
  }

  @Test
  @DisplayName("POST /api/chats/{chatId}/messages responde 200 con sendState")
  void sendMessageReturnsBodyWithSendState() {

    when(this.sendMessage.handle(any())).thenReturn(
        new SendMessageResultDTO("chat-1", new ChatSendStateDTO(false, 1772768160123L)));

    final var response = this.controller.sendMessage(CHAT_ID, new SendMessageRequest("hola"),
        jwt());

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().chatId()).isEqualTo("chat-1");
    assertThat(response.getBody().sendState().canSendNow()).isFalse();
    assertThat(response.getBody().sendState().nextMessageAllowedAt()).isEqualTo(1772768160123L);
  }

  @Test
  @DisplayName("POST /api/chats/by-parent/{parentType}/{parentId}/messages responde 201 con sendState")
  void sendMessageToParentReturnsCreatedWithSendState() {

    when(this.sendMessageToParent.handle(any())).thenReturn(
        new SendMessageResultDTO("chat-1", new ChatSendStateDTO(false, 1772768160123L)));

    final var response = this.controller.sendMessageToParent("FRIENDSHIP", "parent-1",
        new SendMessageRequest("hola"), jwt());

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/chats/chat-1");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().chatId()).isEqualTo("chat-1");
    assertThat(response.getBody().sendState().canSendNow()).isFalse();
  }

  @Test
  @DisplayName("el error de rate limit no incluye metadata de cooldown")
  void rateLimitErrorDoesNotIncludeCooldownMetadata() {

    final var response = this.exceptionHandler.handleDomainException(
        new ChatRateLimitExceededException());

    assertThat(response.getStatusCode().value()).isEqualTo(422);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("ChatRateLimitExceededException");
    assertThat(response.getBody().message()).doesNotContain("nextMessageAllowedAt", "retryAfterMs",
        "sendState");
  }

}
