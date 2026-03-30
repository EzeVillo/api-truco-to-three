package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChatLifecycleSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChatLifecycleSupport.class);

  private ChatLifecycleSupport() {

  }

  static void createChat(final ChatQueryRepository chatQueryRepository,
      final ChatRepository chatRepository, final ChatEventNotifier chatEventNotifier,
      final ChatParentType parentType, final String parentId,
      final Collection<PlayerId> participants) {

    final var existing = chatQueryRepository.findByParentTypeAndParentId(parentType, parentId);
    if (existing.isPresent()) {
      LOGGER.debug("Chat already exists for {}={}", parentType.name().toLowerCase(), parentId);
      return;
    }

    final var chat = Chat.create(parentType, parentId, new LinkedHashSet<>(participants));
    chatRepository.save(chat);
    chatEventNotifier.publishDomainEvents(chat.getChatDomainEvents());
    chat.clearDomainEvents();
    LOGGER.info("Created chat for {}={}", parentType.name().toLowerCase(), parentId);
  }

  static void deleteChat(final ChatQueryRepository chatQueryRepository,
      final ChatRepository chatRepository, final ChatParentType parentType, final String parentId) {

    chatQueryRepository.findByParentTypeAndParentId(parentType, parentId).ifPresent(chat -> {
      chatRepository.delete(chat.getId());
      LOGGER.info("Deleted chat for {}={}", parentType.name().toLowerCase(), parentId);
    });
  }

}
