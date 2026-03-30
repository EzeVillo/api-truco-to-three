package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.SendMessageCommand;
import com.villo.truco.application.ports.in.SendMessageUseCase;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatRepository;
import java.util.Objects;

public final class SendMessageCommandHandler implements SendMessageUseCase {

  private final ChatResolver chatResolver;
  private final ChatRepository chatRepository;
  private final ChatEventNotifier chatEventNotifier;

  public SendMessageCommandHandler(final ChatResolver chatResolver,
      final ChatRepository chatRepository, final ChatEventNotifier chatEventNotifier) {

    this.chatResolver = Objects.requireNonNull(chatResolver);
    this.chatRepository = Objects.requireNonNull(chatRepository);
    this.chatEventNotifier = Objects.requireNonNull(chatEventNotifier);
  }

  @Override
  public ChatId handle(final SendMessageCommand command) {

    final var chat = this.chatResolver.resolve(command.chatId());
    chat.sendMessage(command.playerId(), command.content());
    this.chatRepository.save(chat);
    this.chatEventNotifier.publishDomainEvents(chat.getChatDomainEvents());
    chat.clearDomainEvents();

    return command.chatId();
  }

}
