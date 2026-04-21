package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.FriendshipParticipantsPort;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetChatByParentUseCase;
import com.villo.truco.application.ports.in.GetChatMessagesUseCase;
import com.villo.truco.application.ports.in.SendMessageToParentUseCase;
import com.villo.truco.application.ports.in.SendMessageUseCase;
import com.villo.truco.application.usecases.commands.ChatResolver;
import com.villo.truco.application.usecases.commands.SendMessageCommandHandler;
import com.villo.truco.application.usecases.commands.SendMessageToParentCommandHandler;
import com.villo.truco.application.usecases.queries.GetChatByParentQueryHandler;
import com.villo.truco.application.usecases.queries.GetChatMessagesQueryHandler;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatUseCaseConfiguration {

  private final ChatQueryRepository chatQueryRepository;
  private final ChatRepository chatRepository;
  private final ChatEventNotifier chatEventNotifier;
  private final PublicActorResolver publicActorResolver;
  private final FriendshipParticipantsPort friendshipParticipantsPort;
  private final UseCasePipeline retryTransactionalPipeline;

  public ChatUseCaseConfiguration(final ChatQueryRepository chatQueryRepository,
      final ChatRepository chatRepository, final ChatEventNotifier chatEventNotifier,
      final PublicActorResolver publicActorResolver,
      final FriendshipParticipantsPort friendshipParticipantsPort,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline) {

    this.chatQueryRepository = chatQueryRepository;
    this.chatRepository = chatRepository;
    this.chatEventNotifier = chatEventNotifier;
    this.publicActorResolver = publicActorResolver;
    this.friendshipParticipantsPort = friendshipParticipantsPort;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
  }

  @Bean
  ChatResolver chatResolver() {

    return new ChatResolver(this.chatQueryRepository);
  }

  @Bean
  SendMessageUseCase sendMessageCommandHandler() {

    final var handler = new SendMessageCommandHandler(this.chatResolver(), this.chatRepository,
        this.chatEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetChatMessagesUseCase getChatMessagesQueryHandler() {

    return new GetChatMessagesQueryHandler(this.chatResolver(), this.publicActorResolver);
  }

  @Bean
  SendMessageToParentUseCase sendMessageToParentCommandHandler() {

    final var handler = new SendMessageToParentCommandHandler(this.chatQueryRepository,
        this.chatRepository, this.chatEventNotifier, this.friendshipParticipantsPort);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetChatByParentUseCase getChatByParentQueryHandler() {

    return new GetChatByParentQueryHandler(this.chatQueryRepository, this.publicActorResolver);
  }

}
