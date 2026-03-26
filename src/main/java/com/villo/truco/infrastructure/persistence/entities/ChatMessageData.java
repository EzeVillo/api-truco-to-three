package com.villo.truco.infrastructure.persistence.entities;

import java.util.UUID;

public record ChatMessageData(UUID id, UUID senderId, String content, long sentAt) {

}
