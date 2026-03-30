package com.villo.truco.domain.model.chat;

import com.villo.truco.domain.model.chat.events.ChatCreatedEvent;
import com.villo.truco.domain.model.chat.events.ChatDomainEvent;
import com.villo.truco.domain.model.chat.events.ChatEventEnvelope;
import com.villo.truco.domain.model.chat.exceptions.ChatRateLimitExceededException;
import com.villo.truco.domain.model.chat.exceptions.PlayerNotInChatException;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Chat extends AggregateBase<ChatId> {

  static final int MAX_MESSAGES = 50;
  static final Duration DEFAULT_RATE_LIMIT_COOLDOWN = Duration.ofSeconds(2);

  private final ChatParentType parentType;
  private final String parentId;
  private final Set<PlayerId> participants;
  private final List<ChatMessage> messages;
  private final Map<PlayerId, Instant> lastMessageTimestamps;
  private final Duration rateLimitCooldown;

  private Chat(final ChatId id, final ChatParentType parentType, final String parentId,
      final Set<PlayerId> participants, final List<ChatMessage> messages,
      final Map<PlayerId, Instant> lastMessageTimestamps, final Duration rateLimitCooldown) {

    super(id);
    this.parentType = Objects.requireNonNull(parentType);
    this.parentId = Objects.requireNonNull(parentId);
    this.participants = new LinkedHashSet<>(participants);
    this.messages = new ArrayList<>(messages);
    this.lastMessageTimestamps = new HashMap<>(lastMessageTimestamps);
    this.rateLimitCooldown = Objects.requireNonNull(rateLimitCooldown);
  }

  static Chat reconstruct(final ChatId id, final ChatParentType parentType, final String parentId,
      final Set<PlayerId> participants, final List<ChatMessage> messages,
      final Map<PlayerId, Instant> lastMessageTimestamps, final Duration rateLimitCooldown) {

    return new Chat(id, parentType, parentId, participants, messages, lastMessageTimestamps,
        rateLimitCooldown);
  }

  public static Chat create(final ChatParentType parentType, final String parentId,
      final Set<PlayerId> participants) {

    return create(parentType, parentId, participants, DEFAULT_RATE_LIMIT_COOLDOWN);
  }

  static Chat create(final ChatParentType parentType, final String parentId,
      final Set<PlayerId> participants, final Duration rateLimitCooldown) {

    Objects.requireNonNull(parentType, "Parent type cannot be null");
    Objects.requireNonNull(parentId, "Parent id cannot be null");
    Objects.requireNonNull(participants, "Participants cannot be null");
    if (participants.size() < 2) {
      throw new IllegalArgumentException("Chat requires at least 2 participants");
    }

    final var chat = new Chat(ChatId.generate(), parentType, parentId, participants,
        new ArrayList<>(), new HashMap<>(), rateLimitCooldown);
    chat.addDomainEvent(
        new ChatCreatedEvent(chat.getId(), List.copyOf(chat.participants), chat.parentType,
            chat.parentId));
    return chat;
  }

  public void sendMessage(final PlayerId sender, final String content) {

    this.validateParticipant(sender);
    this.validateRateLimit(sender);

    final var sentAt = Instant.now();
    final var message = ChatMessage.create(sender, content, sentAt);

    if (this.messages.size() >= MAX_MESSAGES) {
      this.messages.removeFirst();
    }
    this.messages.add(message);
    this.lastMessageTimestamps.put(sender, sentAt);

    this.collectMessageEvents(message);
  }

  public void validateParticipant(final PlayerId playerId) {

    if (!this.participants.contains(playerId)) {
      throw new PlayerNotInChatException(playerId);
    }
  }

  boolean hasPlayer(final PlayerId playerId) {

    return this.participants.contains(playerId);
  }

  public Set<PlayerId> getParticipants() {

    return Collections.unmodifiableSet(this.participants);
  }

  public ChatParentType getParentType() {

    return this.parentType;
  }

  public String getParentId() {

    return this.parentId;
  }

  Duration getRateLimitCooldown() {

    return this.rateLimitCooldown;
  }

  Map<PlayerId, Instant> getLastMessageTimestamps() {

    return Collections.unmodifiableMap(this.lastMessageTimestamps);
  }

  public ChatReadView toReadView() {

    return new ChatReadView(this.id, this.parentType, this.parentId,
        Collections.unmodifiableSet(new LinkedHashSet<>(this.participants)),
        this.messages.stream().map(ChatMessage::toReadView).toList());
  }

  public ChatSnapshot snapshot() {

    return new ChatSnapshot(this.id, this.parentType, this.parentId,
        Collections.unmodifiableSet(new LinkedHashSet<>(this.participants)),
        this.messages.stream().map(ChatMessage::toSnapshot).toList(),
        Collections.unmodifiableMap(new LinkedHashMap<>(this.lastMessageTimestamps)),
        this.rateLimitCooldown);
  }

  private void collectMessageEvents(final ChatMessage message) {

    message.getDomainEvents().stream()
        .map(event -> new ChatEventEnvelope(this.id, List.copyOf(this.participants), event))
        .forEach(this.domainEvents::add);
    message.clearDomainEvents();
  }

  public List<ChatDomainEvent> getChatDomainEvents() {

    return getDomainEvents().stream().map(ChatDomainEvent.class::cast).toList();
  }

  private void validateRateLimit(final PlayerId sender) {

    final var lastSent = this.lastMessageTimestamps.get(sender);
    if (lastSent != null && Instant.now().isBefore(lastSent.plus(this.rateLimitCooldown))) {
      throw new ChatRateLimitExceededException();
    }
  }

}
