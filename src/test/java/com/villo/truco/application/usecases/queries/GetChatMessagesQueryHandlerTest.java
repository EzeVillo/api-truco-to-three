package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.queries.GetChatMessagesQuery;
import com.villo.truco.application.usecases.commands.ChatResolver;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.exceptions.PlayerNotInChatException;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetChatMessagesQueryHandler")
class GetChatMessagesQueryHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private Chat chat;
  private GetChatMessagesQueryHandler handler;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.chat = Chat.create(ChatParentType.MATCH, "parent-123", Set.of(playerOne, playerTwo));
    this.chat.clearDomainEvents();
    this.chat.sendMessage(playerOne, "hola");

    final ChatQueryRepository queryRepository = new ChatQueryRepository() {
      @Override
      public Optional<Chat> findById(final ChatId chatId) {

        return chat.getId().equals(chatId) ? Optional.of(chat) : Optional.empty();
      }

      @Override
      public Optional<Chat> findByParentTypeAndParentId(final ChatParentType parentType,
          final String parentId) {

        return Optional.empty();
      }
    };

    this.handler = new GetChatMessagesQueryHandler(new ChatResolver(queryRepository));
  }

  @Test
  @DisplayName("devuelve mensajes mapeados desde ChatReadView")
  void returnsMessagesMappedFromReadView() {

    final var dto = this.handler.handle(
        new GetChatMessagesQuery(this.chat.getId(), this.playerOne));

    assertThat(dto.chatId()).isEqualTo(this.chat.getId().value().toString());
    assertThat(dto.parentType()).isEqualTo(ChatParentType.MATCH.name());
    assertThat(dto.parentId()).isEqualTo("parent-123");
    assertThat(dto.messages()).hasSize(1);
    assertThat(dto.messages().getFirst().senderId()).isEqualTo(this.playerOne.value().toString());
    assertThat(dto.messages().getFirst().content()).isEqualTo("hola");
  }

  @Test
  @DisplayName("rechaza consultas de jugadores ajenos al chat")
  void rejectsPlayersOutsideTheChat() {

    final var outsider = PlayerId.generate();

    assertThatThrownBy(() -> this.handler.handle(
        new GetChatMessagesQuery(this.chat.getId(), outsider))).isInstanceOf(
        PlayerNotInChatException.class);
  }

}
