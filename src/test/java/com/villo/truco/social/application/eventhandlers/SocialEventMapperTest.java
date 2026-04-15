package com.villo.truco.social.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestAcceptedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestCancelledEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestDeclinedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestReceivedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendshipRemovedEvent;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDeclinedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationExpiredEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SocialEventMapper")
class SocialEventMapperTest {

  @Test
  @DisplayName("omite ids publicos en eventos de amistad")
  void mapsFriendshipEventsWithOnlyUsernames() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendshipId = FriendshipId.generate();
    final var mapper = new SocialEventMapper(
        new FixedPublicActorResolver(Map.of(requester, "juancho", addressee, "martina")));

    assertThat(mapper.map(
        new FriendRequestReceivedEvent(friendshipId, requester, addressee))).containsEntry(
            "requesterUsername", "juancho").containsEntry("addresseeUsername", "martina")
        .doesNotContainKeys("requesterId", "friendshipId");
    assertThat(mapper.map(
        new FriendRequestAcceptedEvent(friendshipId, requester, addressee))).containsEntry(
            "requesterUsername", "juancho").containsEntry("addresseeUsername", "martina")
        .doesNotContainKeys("friendId", "friendshipId");
    assertThat(mapper.map(
        new FriendRequestDeclinedEvent(friendshipId, requester, addressee))).containsEntry(
            "requesterUsername", "juancho").containsEntry("addresseeUsername", "martina")
        .doesNotContainKeys("addresseeId", "friendshipId");
    assertThat(mapper.map(
        new FriendRequestCancelledEvent(friendshipId, requester, addressee))).containsEntry(
            "requesterUsername", "juancho").containsEntry("addresseeUsername", "martina")
        .doesNotContainKeys("requesterId", "friendshipId");
    assertThat(mapper.map(
        new FriendshipRemovedEvent(friendshipId, requester, addressee, requester))).containsEntry(
            "requesterUsername", "juancho").containsEntry("addresseeUsername", "martina")
        .containsEntry("removedByUsername", "juancho")
        .doesNotContainKeys("removedById", "friendshipId");
  }

  @Test
  @DisplayName("expone solo los campos utiles por evento de invitacion")
  void mapsInvitationEventsWithOnlyRelevantFields() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitationId = ResourceInvitationId.generate();
    final var expiresAt = Instant.parse("2026-04-04T12:10:00Z");
    final var targetId = "11111111-1111-1111-1111-111111111111";
    final var mapper = new SocialEventMapper(
        new FixedPublicActorResolver(Map.of(sender, "juancho", recipient, "martina")));

    assertThat(mapper.map(new ResourceInvitationReceivedEvent(invitationId, sender, recipient,
        ResourceInvitationTargetType.MATCH, targetId, expiresAt))).containsEntry("invitationId",
            invitationId.value().toString()).containsEntry("senderUsername", "juancho")
        .containsEntry("expiresAt", expiresAt.toEpochMilli())
        .doesNotContainKeys("recipientUsername", "status", "senderId", "recipientId");
    assertThat(mapper.map(new ResourceInvitationAcceptedEvent(invitationId, sender, recipient,
        ResourceInvitationTargetType.MATCH, targetId, expiresAt))).containsEntry(
            "recipientUsername", "martina")
        .containsEntry("invitationId", invitationId.value().toString())
        .containsEntry("targetType", "MATCH").containsEntry("targetId", targetId)
        .doesNotContainKeys("senderUsername", "expiresAt", "status", "senderId", "recipientId");
    assertThat(mapper.map(new ResourceInvitationCancelledEvent(invitationId, sender, recipient,
        ResourceInvitationTargetType.MATCH, targetId, expiresAt))).containsEntry("senderUsername",
            "juancho").containsEntry("invitationId", invitationId.value().toString())
        .containsEntry("targetType", "MATCH").containsEntry("targetId", targetId)
        .doesNotContainKeys("recipientUsername", "expiresAt", "status", "senderId", "recipientId");
    assertThat(mapper.map(new ResourceInvitationDeclinedEvent(invitationId, sender, recipient,
        ResourceInvitationTargetType.MATCH, targetId, expiresAt))).containsEntry(
            "recipientUsername", "martina")
        .containsEntry("invitationId", invitationId.value().toString())
        .containsEntry("targetType", "MATCH").containsEntry("targetId", targetId)
        .doesNotContainKeys("senderUsername", "expiresAt", "status", "senderId", "recipientId");
    assertThat(mapper.map(new ResourceInvitationExpiredEvent(invitationId, sender, recipient,
        ResourceInvitationTargetType.MATCH, targetId, expiresAt))).containsEntry("senderUsername",
            "juancho").containsEntry("recipientUsername", "martina")
        .containsEntry("invitationId", invitationId.value().toString())
        .containsEntry("targetType", "MATCH").containsEntry("targetId", targetId)
        .doesNotContainKeys("senderId", "recipientId");
  }

  private record FixedPublicActorResolver(Map<PlayerId, String> usernamesById) implements
      PublicActorResolver {

    private FixedPublicActorResolver(final Map<PlayerId, String> usernamesById) {

      this.usernamesById = new LinkedHashMap<>(usernamesById);
    }

    @Override
    public String resolve(final PlayerId playerId) {

      return this.usernamesById.get(playerId);
    }

    @Override
    public Map<PlayerId, String> resolveAll(final Collection<PlayerId> playerIds) {

      final var resolved = new LinkedHashMap<PlayerId, String>();
      for (final var playerId : playerIds) {
        final var username = this.usernamesById.get(playerId);
        if (username != null) {
          resolved.put(playerId, username);
        }
      }
      return resolved;
    }

  }

}
