package com.villo.truco.application.assemblers;

import com.villo.truco.application.dto.ChatMessageDTO;
import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.chat.ChatReadView;
import java.util.Objects;

public final class ChatMessagesDTOAssembler {

  private final PublicActorResolver publicActorResolver;

  public ChatMessagesDTOAssembler(final PublicActorResolver publicActorResolver) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  public ChatMessagesDTO toDto(final ChatReadView chat) {

    final var actorNames = this.publicActorResolver.resolveAll(chat.participants());

    final var messages = chat.messages().stream().map(
            message -> new ChatMessageDTO(message.id().value().toString(),
                actorNames.get(message.senderId()), message.content(), message.sentAt().toEpochMilli()))
        .toList();

    return new ChatMessagesDTO(chat.id().value().toString(), chat.parentType().name(),
        chat.parentId(), messages);
  }

}
