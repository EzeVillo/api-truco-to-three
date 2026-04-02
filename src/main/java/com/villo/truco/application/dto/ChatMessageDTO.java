package com.villo.truco.application.dto;

public record ChatMessageDTO(String messageId, String sender, String content, long sentAt) {

}
