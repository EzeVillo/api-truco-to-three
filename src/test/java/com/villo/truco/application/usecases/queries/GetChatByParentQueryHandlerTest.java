package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.queries.GetChatByParentQuery;
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

@DisplayName("GetChatByParentQueryHandler")
class GetChatByParentQueryHandlerTest {

  private static final String PARENT_ID = "match-abc";

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private Chat chat;
  private GetChatByParentQueryHandler handler;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.chat = Chat.create(ChatParentType.MATCH, PARENT_ID, Set.of(playerOne, playerTwo));
    this.chat.clearDomainEvents();
    this.chat.sendMessage(playerTwo, "buenas");

    final ChatQueryRepository queryRepository = new ChatQueryRepository() {
      @Override
      public Optional<Chat> findById(final ChatId chatId) {

        return Optional.empty();
      }

      @Override
      public Optional<Chat> findByParentTypeAndParentId(final ChatParentType parentType,
          final String parentId) {

        return parentType == ChatParentType.MATCH && PARENT_ID.equals(parentId) ? Optional.of(chat)
            : Optional.empty();
      }
    };

    this.handler = new GetChatByParentQueryHandler(queryRepository);
  }

  @Test
  @DisplayName("devuelve el chat consultado por recurso padre")
  void returnsChatByParent() {

    final var dto = this.handler.handle(
        new GetChatByParentQuery(ChatParentType.MATCH, PARENT_ID, this.playerOne));

    assertThat(dto.parentType()).isEqualTo(ChatParentType.MATCH.name());
    assertThat(dto.parentId()).isEqualTo(PARENT_ID);
    assertThat(dto.messages()).hasSize(1);
    assertThat(dto.messages().getFirst().senderId()).isEqualTo(this.playerTwo.value().toString());
    assertThat(dto.messages().getFirst().content()).isEqualTo("buenas");
  }

  @Test
  @DisplayName("rechaza consulta por parent de jugadores que no participan")
  void rejectsPlayersOutsideTheChat() {

    final var outsider = PlayerId.generate();

    assertThatThrownBy(() -> this.handler.handle(
        new GetChatByParentQuery(ChatParentType.MATCH, PARENT_ID, outsider))).isInstanceOf(
        PlayerNotInChatException.class);
  }

}
