package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.ChatRehydrator;
import com.villo.truco.domain.model.chat.ChatSnapshot;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.persistence.inmemory.AbstractTransactionalInMemoryRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryChatRepositoryAdapter extends
    AbstractTransactionalInMemoryRepository<InMemoryChatRepositoryAdapter.ChatTransactionContext> implements
    ChatRepository, ChatQueryRepository {

  private final AtomicReference<CommittedState> committedState = new AtomicReference<>(
      CommittedState.empty());

  private static StaleAggregateException stale(final ChatId chatId, final long expectedVersion,
      final long actualVersion) {

    return new StaleAggregateException(
        "Chat " + chatId + " was modified concurrently: expected version " + expectedVersion
            + " but was " + actualVersion, null);
  }

  @Override
  public void save(final Chat chat) {

    Objects.requireNonNull(chat);

    final var snapshot = this.copySnapshot(chat.snapshot());
    final var parentKey = this.parentKey(snapshot.parentType(), snapshot.parentId());
    final var chatKey = this.chatKey(snapshot.id());

    this.executeWrite(List.of(chatKey, parentKey), context -> {
      final var current = this.findStoredById(context, chat.getId()).orElse(null);
      if (current == null) {
        this.saveNewChat(context, chat, snapshot, parentKey);
        return null;
      }

      if (chat.getVersion() != current.version()) {
        throw stale(chat.getId(), current.version(), chat.getVersion());
      }

      final var newVersion = current.version() + 1;
      context.upsert(snapshot.id(), new StoredChat(snapshot, newVersion));
      context.upsertParent(parentKey, chat.getId());
      chat.setVersion(newVersion);
      return null;
    });
  }

  @Override
  public void delete(final ChatId chatId) {

    Objects.requireNonNull(chatId);

    final var parentKey = this.currentStoredById(chatId)
        .map(stored -> this.parentKey(stored.snapshot().parentType(), stored.snapshot().parentId()))
        .orElse(null);

    final var resources = parentKey == null ? List.of(this.chatKey(chatId))
        : List.of(this.chatKey(chatId), parentKey);

    this.executeWrite(resources, context -> {
      this.findStoredById(context, chatId).ifPresent(stored -> {
        final var resolvedParentKey = this.parentKey(stored.snapshot().parentType(),
            stored.snapshot().parentId());
        context.delete(chatId);
        context.deleteParent(resolvedParentKey);
      });
      return null;
    });
  }

  @Override
  public Optional<Chat> findById(final ChatId chatId) {

    Objects.requireNonNull(chatId);
    return this.currentStoredById(chatId)
        .map(stored -> this.toDomain(stored.snapshot(), stored.version()));
  }

  @Override
  public Optional<Chat> findByParentTypeAndParentId(final ChatParentType parentType,
      final String parentId) {

    Objects.requireNonNull(parentType);
    Objects.requireNonNull(parentId);

    final var parentKey = this.parentKey(parentType, parentId);
    return this.currentStoredByParent(parentKey)
        .map(stored -> this.toDomain(stored.snapshot(), stored.version()));
  }

  @Override
  protected ChatTransactionContext newTransactionContext() {

    return new ChatTransactionContext();
  }

  @Override
  protected void commit(final ChatTransactionContext context) {

    if (!context.hasChanges()) {
      return;
    }

    while (true) {
      final var current = this.committedState.get();
      final var next = context.apply(current);
      if (this.committedState.compareAndSet(current, next)) {
        return;
      }
    }
  }

  private void saveNewChat(final ChatTransactionContext context, final Chat chat,
      final ChatSnapshot snapshot, final String parentKey) {

    if (chat.getVersion() != 0) {
      throw stale(chat.getId(), 0, chat.getVersion());
    }

    final var existingChat = this.findStoredByParent(context, parentKey).orElse(null);
    if (existingChat != null && !existingChat.snapshot().id().equals(chat.getId())) {
      throw new StaleAggregateException("Chat for " + parentKey + " was modified concurrently",
          null);
    }

    final var newVersion = 1L;
    context.upsert(chat.getId(), new StoredChat(snapshot, newVersion));
    context.upsertParent(parentKey, chat.getId());
    chat.setVersion(newVersion);
  }

  private Optional<StoredChat> currentStoredById(final ChatId chatId) {

    final var context = this.currentTransactionContext();
    if (context != null) {
      return this.findStoredById(context, chatId);
    }
    return Optional.ofNullable(this.committedState.get().chatsById().get(chatId));
  }

  private Optional<StoredChat> currentStoredByParent(final String parentKey) {

    final var context = this.currentTransactionContext();
    if (context != null) {
      return this.findStoredByParent(context, parentKey);
    }

    final var chatId = this.committedState.get().chatIdsByParent().get(parentKey);
    return chatId == null ? Optional.empty()
        : Optional.ofNullable(this.committedState.get().chatsById().get(chatId));
  }

  private Optional<StoredChat> findStoredById(final ChatTransactionContext context,
      final ChatId chatId) {

    if (context.hasDeletedChat(chatId)) {
      return Optional.empty();
    }
    final var staged = context.upsertedChat(chatId);
    if (staged != null) {
      return Optional.of(staged);
    }
    return Optional.ofNullable(this.committedState.get().chatsById().get(chatId));
  }

  private Optional<StoredChat> findStoredByParent(final ChatTransactionContext context,
      final String parentKey) {

    if (context.hasDeletedParent(parentKey)) {
      return Optional.empty();
    }

    final var stagedChatId = context.upsertedParent(parentKey);
    if (stagedChatId != null) {
      return this.findStoredById(context, stagedChatId);
    }

    final var chatId = this.committedState.get().chatIdsByParent().get(parentKey);
    return chatId == null ? Optional.empty() : this.findStoredById(context, chatId);
  }

  private Chat toDomain(final ChatSnapshot snapshot, final long version) {

    final var chat = ChatRehydrator.rehydrate(this.copySnapshot(snapshot));
    chat.setVersion(version);
    return chat;
  }

  private ChatSnapshot copySnapshot(final ChatSnapshot snapshot) {

    return new ChatSnapshot(snapshot.id(), snapshot.parentType(), snapshot.parentId(),
        new LinkedHashSet<>(snapshot.participants()), List.copyOf(snapshot.messages()),
        new LinkedHashMap<>(snapshot.lastMessageTimestamps()), snapshot.rateLimitCooldown());
  }

  private String parentKey(final ChatParentType parentType, final String parentId) {

    return parentType.name() + ":" + parentId;
  }

  private String chatKey(final ChatId chatId) {

    return "chat:" + chatId.value();
  }

  static final class ChatTransactionContext extends TransactionContext {

    private final Map<ChatId, StoredChat> upsertsById = new LinkedHashMap<>();
    private final Map<String, ChatId> upsertsByParent = new LinkedHashMap<>();
    private final LinkedHashSet<ChatId> deletedChatIds = new LinkedHashSet<>();
    private final LinkedHashSet<String> deletedParentKeys = new LinkedHashSet<>();

    private boolean hasChanges() {

      return !this.upsertsById.isEmpty() || !this.upsertsByParent.isEmpty()
          || !this.deletedChatIds.isEmpty() || !this.deletedParentKeys.isEmpty();
    }

    private void upsert(final ChatId chatId, final StoredChat storedChat) {

      this.deletedChatIds.remove(chatId);
      this.upsertsById.put(chatId, storedChat);
    }

    private void delete(final ChatId chatId) {

      this.upsertsById.remove(chatId);
      this.deletedChatIds.add(chatId);
    }

    private void upsertParent(final String parentKey, final ChatId chatId) {

      this.deletedParentKeys.remove(parentKey);
      this.upsertsByParent.put(parentKey, chatId);
    }

    private void deleteParent(final String parentKey) {

      this.upsertsByParent.remove(parentKey);
      this.deletedParentKeys.add(parentKey);
    }

    private boolean hasDeletedChat(final ChatId chatId) {

      return this.deletedChatIds.contains(chatId);
    }

    private boolean hasDeletedParent(final String parentKey) {

      return this.deletedParentKeys.contains(parentKey);
    }

    private StoredChat upsertedChat(final ChatId chatId) {

      return this.upsertsById.get(chatId);
    }

    private ChatId upsertedParent(final String parentKey) {

      return this.upsertsByParent.get(parentKey);
    }

    private CommittedState apply(final CommittedState current) {

      final var chatsById = new LinkedHashMap<>(current.chatsById());
      final var chatIdsByParent = new LinkedHashMap<>(current.chatIdsByParent());

      this.deletedChatIds.forEach(chatsById::remove);
      this.deletedParentKeys.forEach(chatIdsByParent::remove);
      chatsById.putAll(this.upsertsById);
      chatIdsByParent.putAll(this.upsertsByParent);

      return new CommittedState(Map.copyOf(chatsById), Map.copyOf(chatIdsByParent));
    }

  }

  private record StoredChat(ChatSnapshot snapshot, long version) {

  }

  private record CommittedState(Map<ChatId, StoredChat> chatsById,
                                Map<String, ChatId> chatIdsByParent) {

    private static CommittedState empty() {

      return new CommittedState(Map.of(), Map.of());
    }

  }

}
