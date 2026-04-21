package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.support.TestTransactionRunner;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryChatRepositoryAdapter")
class InMemoryChatRepositoryAdapterTest {

  @Test
  @DisplayName("save asigna version y permite buscar por id y parent")
  void saveAssignsVersionAndAllowsQueryingByIdAndParent() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = createChat();

    repository.save(chat);

    assertThat(chat.getVersion()).isEqualTo(1);
    assertThat(repository.findById(chat.getId())).isPresent();
    assertThat(repository.findByParentTypeAndParentId(ChatParentType.MATCH,
        chat.getParentId())).isPresent();
  }

  @Test
  @DisplayName("find devuelve copias defensivas")
  void findReturnsDefensiveCopies() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = createChat();
    repository.save(chat);

    final var loaded = repository.findById(chat.getId()).orElseThrow();
    loaded.sendMessage(loaded.getParticipants().iterator().next(), "hola");

    final var reloaded = repository.findById(chat.getId()).orElseThrow();
    assertThat(reloaded.toReadView().messages()).isEmpty();
  }

  @Test
  @DisplayName("delete elimina chat e indice por parent")
  void deleteRemovesChatAndParentIndex() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = createChat();
    repository.save(chat);

    repository.delete(chat.getId());

    assertThat(repository.findById(chat.getId())).isEmpty();
    assertThat(
        repository.findByParentTypeAndParentId(ChatParentType.MATCH, chat.getParentId())).isEmpty();
  }

  @Test
  @DisplayName("save rechaza versiones viejas")
  void saveRejectsStaleVersions() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = createChat();
    repository.save(chat);

    final var staleCopy = repository.findById(chat.getId()).orElseThrow();
    final var currentCopy = repository.findById(chat.getId()).orElseThrow();

    currentCopy.sendMessage(currentCopy.getParticipants().iterator().next(), "hola");
    repository.save(currentCopy);

    staleCopy.sendMessage(staleCopy.getParticipants().iterator().next(), "chau");

    assertThatThrownBy(() -> repository.save(staleCopy)).isInstanceOf(
        StaleAggregateException.class);
  }

  @Test
  @DisplayName("commit aplica cambios stageados al cerrar la transaccion")
  void commitAppliesStagedChanges() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = createChat();

    TestTransactionRunner.inTransaction(() -> {
      repository.save(chat);
      assertThat(repository.findById(chat.getId())).isPresent();
    });

    assertThat(repository.findById(chat.getId())).isPresent();
    assertThat(chat.getVersion()).isEqualTo(1);
  }

  @Test
  @DisplayName("rollback descarta cambios stageados")
  void rollbackDiscardsStagedChanges() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var chat = createChat();

    assertThatThrownBy(() -> TestTransactionRunner.inTransaction(() -> {
      repository.save(chat);
      throw new IllegalStateException("boom");
    })).isInstanceOf(IllegalStateException.class).hasMessage("boom");

    assertThat(repository.findById(chat.getId())).isEmpty();
    assertThat(
        repository.findByParentTypeAndParentId(ChatParentType.MATCH, chat.getParentId())).isEmpty();
  }

  @Test
  @DisplayName("save rechaza crear dos chats para el mismo parent")
  void saveRejectsDuplicatedParent() {

    final var repository = new InMemoryChatRepositoryAdapter();
    repository.save(createChat());

    final var duplicated = createChat();

    assertThatThrownBy(() -> repository.save(duplicated)).isInstanceOf(
        StaleAggregateException.class);
  }

  private Chat createChat() {

    return Chat.create(ChatParentType.MATCH, "parent-123",
        Set.of(PlayerId.generate(), PlayerId.generate()));
  }

}
