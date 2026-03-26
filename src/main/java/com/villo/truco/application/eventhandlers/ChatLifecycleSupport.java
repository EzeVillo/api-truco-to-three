package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChatLifecycleSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatLifecycleSupport.class);

    private ChatLifecycleSupport() {
    }

    static void deleteChat(final ChatQueryRepository chatQueryRepository,
        final ChatRepository chatRepository, final ChatParentType parentType,
        final String parentId) {

        chatQueryRepository.findByParentTypeAndParentId(parentType, parentId)
            .ifPresent(chat -> {
                chatRepository.delete(chat.getId());
                LOGGER.info("Deleted chat for {}={}", parentType.name().toLowerCase(), parentId);
            });
    }

}
