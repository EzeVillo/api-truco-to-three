package com.villo.truco.infrastructure.persistence.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMapper")
class ChatMapperTest {

  @Test
  @DisplayName("serializa y rehidrata conservando el snapshot del aggregate")
  void preservesAggregateSnapshotThroughEntityMapping() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var chat = Chat.create(ChatParentType.MATCH, "parent-123",
        new LinkedHashSet<>(List.of(playerOne, playerTwo)));
    chat.clearDomainEvents();
    chat.sendMessage(playerOne, "hola");
    chat.sendMessage(playerTwo, "que tal");
    chat.setVersion(5);

    final var mapper = new ChatMapper();

    final var entity = mapper.toEntity(chat);
    final var restored = mapper.toDomain(entity);
    final var originalSnapshot = chat.snapshot();
    final var restoredSnapshot = restored.snapshot();

    assertThat(entity.getVersion()).isEqualTo(5);
    assertThat(entity.getMessages()).hasSize(2);
    assertThat(entity.getMessages().getFirst().content()).isEqualTo("hola");
    assertThat(entity.getMessages().get(1).content()).isEqualTo("que tal");
    assertThat(restoredSnapshot.id()).isEqualTo(originalSnapshot.id());
    assertThat(restoredSnapshot.parentType()).isEqualTo(originalSnapshot.parentType());
    assertThat(restoredSnapshot.parentId()).isEqualTo(originalSnapshot.parentId());
    assertThat(restoredSnapshot.participants()).isEqualTo(originalSnapshot.participants());
    assertThat(restoredSnapshot.messages()).hasSize(2);
    assertThat(restoredSnapshot.messages().getFirst().id()).isEqualTo(
        originalSnapshot.messages().getFirst().id());
    assertThat(restoredSnapshot.messages().getFirst().senderId()).isEqualTo(
        originalSnapshot.messages().getFirst().senderId());
    assertThat(restoredSnapshot.messages().getFirst().content()).isEqualTo(
        originalSnapshot.messages().getFirst().content());
    assertThat(restoredSnapshot.messages().getFirst().sentAt().toEpochMilli()).isEqualTo(
        originalSnapshot.messages().getFirst().sentAt().toEpochMilli());
    assertThat(restoredSnapshot.messages().get(1).id()).isEqualTo(
        originalSnapshot.messages().get(1).id());
    assertThat(restoredSnapshot.messages().get(1).senderId()).isEqualTo(
        originalSnapshot.messages().get(1).senderId());
    assertThat(restoredSnapshot.messages().get(1).content()).isEqualTo(
        originalSnapshot.messages().get(1).content());
    assertThat(restoredSnapshot.messages().get(1).sentAt().toEpochMilli()).isEqualTo(
        originalSnapshot.messages().get(1).sentAt().toEpochMilli());
    assertThat(restoredSnapshot.lastMessageTimestamps()).hasSameSizeAs(
        originalSnapshot.lastMessageTimestamps());
    originalSnapshot.lastMessageTimestamps().forEach((playerId, sentAt) -> assertThat(
        restoredSnapshot.lastMessageTimestamps().get(playerId).toEpochMilli()).isEqualTo(
        sentAt.toEpochMilli()));
    assertThat(restoredSnapshot.rateLimitCooldown()).isEqualTo(
        originalSnapshot.rateLimitCooldown());
    assertThat(restored.getVersion()).isEqualTo(5);
  }

}
