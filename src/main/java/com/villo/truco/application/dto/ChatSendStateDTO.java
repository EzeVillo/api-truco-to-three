package com.villo.truco.application.dto;

public record ChatSendStateDTO(boolean canSendNow, Long nextMessageAllowedAt) {

}
