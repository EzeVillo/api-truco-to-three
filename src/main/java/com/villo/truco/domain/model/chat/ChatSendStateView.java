package com.villo.truco.domain.model.chat;

public record ChatSendStateView(boolean canSendNow, Long nextMessageAllowedAt) {

}
