package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.exceptions.ChatNotFoundException;
import com.villo.truco.application.ports.in.GetChatByParentUseCase;
import com.villo.truco.application.queries.GetChatByParentQuery;
import com.villo.truco.domain.ports.ChatQueryRepository;
import java.util.Objects;

public final class GetChatByParentQueryHandler implements GetChatByParentUseCase {

  private final ChatQueryRepository chatQueryRepository;

  public GetChatByParentQueryHandler(final ChatQueryRepository chatQueryRepository) {

    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
  }

  @Override
  public ChatMessagesDTO handle(final GetChatByParentQuery query) {

    final var chat = this.chatQueryRepository.findByParentTypeAndParentId(query.parentType(),
            query.parentId())
        .orElseThrow(() -> new ChatNotFoundException(query.parentType(), query.parentId()));

    chat.validateParticipant(query.requestingPlayer());

    return ChatMessagesDTO.of(chat.toReadView());
  }

}
