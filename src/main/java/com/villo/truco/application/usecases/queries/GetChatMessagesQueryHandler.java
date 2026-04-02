package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetChatMessagesUseCase;
import com.villo.truco.application.queries.GetChatMessagesQuery;
import com.villo.truco.application.usecases.commands.ChatResolver;
import java.util.Objects;

public final class GetChatMessagesQueryHandler implements GetChatMessagesUseCase {

  private final ChatResolver chatResolver;
  private final PublicActorResolver publicActorResolver;

  public GetChatMessagesQueryHandler(final ChatResolver chatResolver,
      final PublicActorResolver publicActorResolver) {

    this.chatResolver = Objects.requireNonNull(chatResolver);
    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  @Override
  public ChatMessagesDTO handle(final GetChatMessagesQuery query) {

    final var chat = this.chatResolver.resolve(query.chatId());

    chat.validateParticipant(query.requestingPlayer());

    return ChatMessagesDTO.of(chat.toReadView(), this.publicActorResolver);
  }

}
