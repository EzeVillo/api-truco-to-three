package com.villo.truco.social.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendAvailabilityDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStatus;
import com.villo.truco.social.application.events.FriendAvailabilityNotification;
import com.villo.truco.social.application.events.SocialEventNotification;
import com.villo.truco.social.application.services.FriendAvailabilityResolver;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SocialNotificationEventTranslator")
class SocialNotificationEventTranslatorTest {

  private static FriendAvailabilityDTO available(final String username) {

    return new FriendAvailabilityDTO(username, false, FriendAvailabilityStatus.AVAILABLE, null,
        null);
  }

  @Test
  @DisplayName("ante invitacion aceptada emite delta de disponibilidad a ambas partes")
  void emitsAvailabilityDeltaForBothPartiesOnAccepted() {

    final var mapper = mock(SocialEventMapper.class);
    when(mapper.map(any())).thenReturn(Map.of());
    final var availabilityResolver = mock(FriendAvailabilityResolver.class);
    final var publisher = new RecordingPublisher();
    final var translator = new SocialNotificationEventTranslator(mapper, availabilityResolver,
        publisher);

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    when(availabilityResolver.resolveFriendDeltaFor(sender, recipient)).thenReturn(
        Optional.of(available("recipiente")));
    when(availabilityResolver.resolveFriendDeltaFor(recipient, sender)).thenReturn(
        Optional.of(available("emisor")));

    translator.handle(
        new ResourceInvitationAcceptedEvent(ResourceInvitationId.generate(), sender, recipient,
            ResourceInvitationTargetType.MATCH, "match-1", Instant.now()));

    final var availabilityEvents = publisher.events().stream()
        .filter(FriendAvailabilityNotification.class::isInstance)
        .map(FriendAvailabilityNotification.class::cast).toList();
    assertThat(availabilityEvents).hasSize(2);
    assertThat(availabilityEvents).allSatisfy(
        event -> assertThat(event.eventType()).isEqualTo("FRIEND_AVAILABILITY_CHANGED"));
    assertThat(availabilityEvents).flatExtracting(FriendAvailabilityNotification::recipients)
        .containsExactlyInAnyOrder(sender, recipient);
  }

  @Test
  @DisplayName("no emite delta de disponibilidad cuando la amistad ya no vige")
  void doesNotEmitDeltaWhenFriendshipMissing() {

    final var mapper = mock(SocialEventMapper.class);
    when(mapper.map(any())).thenReturn(Map.of());
    final var availabilityResolver = mock(FriendAvailabilityResolver.class);
    when(availabilityResolver.resolveFriendDeltaFor(any(), any())).thenReturn(Optional.empty());
    final var publisher = new RecordingPublisher();
    final var translator = new SocialNotificationEventTranslator(mapper, availabilityResolver,
        publisher);

    translator.handle(
        new ResourceInvitationAcceptedEvent(ResourceInvitationId.generate(), PlayerId.generate(),
            PlayerId.generate(), ResourceInvitationTargetType.MATCH, "match-1", Instant.now()));

    assertThat(publisher.events()).noneMatch(FriendAvailabilityNotification.class::isInstance);
    assertThat(publisher.events()).anyMatch(SocialEventNotification.class::isInstance);
  }

  @Test
  @DisplayName("una invitacion recibida no genera deltas de disponibilidad")
  void receivedInvitationDoesNotEmitAvailabilityDelta() {

    final var mapper = mock(SocialEventMapper.class);
    when(mapper.map(any())).thenReturn(Map.of());
    final var availabilityResolver = mock(FriendAvailabilityResolver.class);
    final var publisher = new RecordingPublisher();
    final var translator = new SocialNotificationEventTranslator(mapper, availabilityResolver,
        publisher);

    translator.handle(
        new ResourceInvitationReceivedEvent(ResourceInvitationId.generate(), PlayerId.generate(),
            PlayerId.generate(), ResourceInvitationTargetType.MATCH, "match-1", Instant.now()));

    assertThat(publisher.events()).noneMatch(FriendAvailabilityNotification.class::isInstance);
  }

  private static final class RecordingPublisher implements ApplicationEventPublisher {

    private final List<ApplicationEvent> events = new ArrayList<>();

    @Override
    public void publish(final ApplicationEvent event) {

      this.events.add(event);
    }

    private List<ApplicationEvent> events() {

      return List.copyOf(this.events);
    }

  }

}
