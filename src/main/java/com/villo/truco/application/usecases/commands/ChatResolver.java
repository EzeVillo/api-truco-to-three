package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.exceptions.ChatNotFoundException;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.ports.ChatQueryRepository;
import java.util.Objects;

public final class ChatResolver {

    private final ChatQueryRepository chatQueryRepository;

    public ChatResolver(final ChatQueryRepository chatQueryRepository) {

        this.chatQueryRepository = Objects.requireNonNull(chatQueryRepository);
    }

    public Chat resolve(final ChatId chatId) {

        return this.chatQueryRepository.findById(chatId)
            .orElseThrow(() -> new ChatNotFoundException(chatId));
    }

}
