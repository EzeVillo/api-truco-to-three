package com.villo.truco.social.application.eventhandlers;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestAcceptedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestCancelledEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestDeclinedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestReceivedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendshipRemovedEvent;
import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDeclinedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationExpiredEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SocialEventMapper {

  private final PublicActorResolver publicActorResolver;

  public SocialEventMapper(final PublicActorResolver publicActorResolver) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  public Map<String, Object> map(final SocialDomainEvent event) {

    return switch (event) {
      case FriendRequestReceivedEvent friendshipEvent ->
          this.mapFriendRequestReceived(friendshipEvent);
      case FriendRequestAcceptedEvent friendshipEvent ->
          this.mapFriendRequestAccepted(friendshipEvent);
      case FriendRequestDeclinedEvent friendshipEvent ->
          this.mapFriendRequestDeclined(friendshipEvent);
      case FriendRequestCancelledEvent friendshipEvent ->
          this.mapFriendRequestCancelled(friendshipEvent);
      case FriendshipRemovedEvent friendshipEvent -> this.mapFriendshipRemoved(friendshipEvent);
      case ResourceInvitationReceivedEvent invitationEvent ->
          this.mapInvitationReceived(invitationEvent);
      case ResourceInvitationAcceptedEvent invitationEvent ->
          this.mapInvitationAccepted(invitationEvent);
      case ResourceInvitationCancelledEvent invitationEvent ->
          this.mapInvitationCancelled(invitationEvent);
      case ResourceInvitationDeclinedEvent invitationEvent ->
          this.mapInvitationDeclined(invitationEvent);
      case ResourceInvitationExpiredEvent invitationEvent ->
          this.mapInvitationExpired(invitationEvent);
      default -> Map.of();
    };
  }

  private Map<String, Object> mapFriendRequestReceived(final FriendRequestReceivedEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("requesterUsername", this.publicActorResolver.resolve(event.getRequesterId()));
    payload.put("addresseeUsername", this.publicActorResolver.resolve(event.getAddresseeId()));
    return payload;
  }

  private Map<String, Object> mapFriendRequestAccepted(final FriendRequestAcceptedEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("requesterUsername", this.publicActorResolver.resolve(event.getRequesterId()));
    payload.put("addresseeUsername", this.publicActorResolver.resolve(event.getAddresseeId()));
    return payload;
  }

  private Map<String, Object> mapFriendRequestDeclined(final FriendRequestDeclinedEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("requesterUsername", this.publicActorResolver.resolve(event.getRequesterId()));
    payload.put("addresseeUsername", this.publicActorResolver.resolve(event.getAddresseeId()));
    return payload;
  }

  private Map<String, Object> mapFriendRequestCancelled(final FriendRequestCancelledEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("requesterUsername", this.publicActorResolver.resolve(event.getRequesterId()));
    payload.put("addresseeUsername", this.publicActorResolver.resolve(event.getAddresseeId()));
    return payload;
  }

  private Map<String, Object> mapFriendshipRemoved(final FriendshipRemovedEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("requesterUsername", this.publicActorResolver.resolve(event.getRequesterId()));
    payload.put("addresseeUsername", this.publicActorResolver.resolve(event.getAddresseeId()));
    payload.put("removedByUsername", this.publicActorResolver.resolve(event.getRemovedById()));
    return payload;
  }

  private Map<String, Object> baseInvitationPayload(final ResourceInvitationDomainEvent event) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("invitationId", event.getInvitationId().value().toString());
    payload.put("targetType", event.getTargetType().name());
    payload.put("targetId", event.getTargetId());
    return payload;
  }

  private Map<String, Object> mapInvitationReceived(final ResourceInvitationReceivedEvent event) {

    final var payload = this.baseInvitationPayload(event);
    payload.put("senderUsername", this.publicActorResolver.resolve(event.getSenderId()));
    payload.put("expiresAt", event.getExpiresAt().toEpochMilli());
    return payload;
  }

  private Map<String, Object> mapInvitationAccepted(final ResourceInvitationAcceptedEvent event) {

    final var payload = this.baseInvitationPayload(event);
    payload.put("recipientUsername", this.publicActorResolver.resolve(event.getRecipientId()));
    return payload;
  }

  private Map<String, Object> mapInvitationCancelled(final ResourceInvitationCancelledEvent event) {

    final var payload = this.baseInvitationPayload(event);
    payload.put("senderUsername", this.publicActorResolver.resolve(event.getSenderId()));
    return payload;
  }

  private Map<String, Object> mapInvitationDeclined(final ResourceInvitationDeclinedEvent event) {

    final var payload = this.baseInvitationPayload(event);
    payload.put("recipientUsername", this.publicActorResolver.resolve(event.getRecipientId()));
    return payload;
  }

  private Map<String, Object> mapInvitationExpired(final ResourceInvitationExpiredEvent event) {

    final var payload = this.baseInvitationPayload(event);
    payload.put("senderUsername", this.publicActorResolver.resolve(event.getSenderId()));
    payload.put("recipientUsername", this.publicActorResolver.resolve(event.getRecipientId()));
    return payload;
  }

}
