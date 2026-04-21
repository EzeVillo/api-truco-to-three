package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.SendMessageToParentCommand;
import com.villo.truco.application.exceptions.ChatNotFoundException;
import com.villo.truco.application.ports.FriendshipParticipantsPort;
import com.villo.truco.application.ports.in.SendMessageToParentUseCase;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class SendMessageToParentCommandHandler implements SendMessageToParentUseCase {

  private final ChatQueryRepository chatQueryRepository;
  private final ChatRepository chatRepository;
  private final ChatEventNotifier chatEventNotifier;
  private final FriendshipParticipantsPort friendshipParticipantsPort;

  public SendMessageToParentCommandHandler(final ChatQueryRepository chatQueryRepository,
      final ChatRepository chatRepository, final ChatEventNotifier chatEventNotifier,
      final FriendshipParticipantsPort friendshipParticipantsPort) {

    this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatEventNotifier = Objects.requireNonNull(chatEventNotifier);
    this.friendshipParticipantsPort = Objects.requireNonNull(friendshipParticipantsPort);
  }

  @Override
  public ChatId handle(final SendMessageToParentCommand command) {

    final var chat = this.chatQueryRepository.findByParentTypeAndParentId(command.parentType(),
        command.parentId()).orElseGet(() -> this.createFriendshipChatIfAllowed(command));

    chat.sendMessage(command.playerId(), command.content());
    this.chatRepository.save(chat);
    this.chatEventNotifier.publishDomainEvents(chat.getChatDomainEvents());
    chat.clearDomainEvents();

    return chat.getId();
  }

  private Chat createFriendshipChatIfAllowed(final SendMessageToParentCommand command) {

    if (command.parentType() != ChatParentType.FRIENDSHIP) {
      throw new ChatNotFoundException(command.parentType(), command.parentId());
    }

    final var participants = this.friendshipParticipantsPort.findParticipantsIfAccepted(
            command.parentId(), command.playerId())
        .orElseThrow(() -> new ChatNotFoundException(command.parentType(), command.parentId()));

    return Chat.create(command.parentType(), command.parentId(), new LinkedHashSet<>(participants));
  }

}
