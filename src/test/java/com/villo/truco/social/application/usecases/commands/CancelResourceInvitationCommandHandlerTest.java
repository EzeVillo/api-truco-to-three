package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.commands.CancelResourceInvitationCommand;
import com.villo.truco.social.application.services.ResourceInvitationResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.exceptions.OnlySenderCanCancelResourceInvitationException;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CancelResourceInvitationCommandHandler")
class CancelResourceInvitationCommandHandlerTest {

  @Test
  @DisplayName("cancela la invitacion y la marca como cancelada")
  void cancelsInvitationAndMarksAsCancelled() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitation = ResourceInvitation.create(sender, recipient,
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));
    invitation.clearDomainEvents();
    final var saved = new AtomicReference<ResourceInvitation>();
    final var publishedEvents = new AtomicReference<List<? extends SocialDomainEvent>>();
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = new CancelResourceInvitationCommandHandler(
        new SocialUserGuard(userQueryRepository),
        new ResourceInvitationResolver(new FixedInvitationQueryRepository(invitation)), saved::set,
        publishedEvents::set, new SocialViewAssembler(userQueryRepository));

    final var dto = handler.handle(
        new CancelResourceInvitationCommand(invitation.getId().value().toString(),
            sender.value().toString()));

    assertThat(saved.get()).isSameAs(invitation);
    assertThat(invitation.getStatus()).isEqualTo(ResourceInvitationStatus.CANCELLED);
    assertThat(publishedEvents.get()).singleElement()
        .isInstanceOfSatisfying(ResourceInvitationCancelledEvent.class,
            event -> assertThat(event.getRecipients()).containsExactly(recipient));
    assertThat(dto.status()).isEqualTo("CANCELLED");
  }

  @Test
  @DisplayName("lanza excepcion si intenta cancelar alguien que no es el remitente")
  void throwsWhenActorIsNotSender() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitation = ResourceInvitation.create(sender, recipient,
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));
    invitation.clearDomainEvents();
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var saved = new AtomicBoolean(false);
    final var published = new AtomicBoolean(false);
    final var handler = new CancelResourceInvitationCommandHandler(
        new SocialUserGuard(userQueryRepository),
        new ResourceInvitationResolver(new FixedInvitationQueryRepository(invitation)),
        inv -> saved.set(true), events -> published.set(true),
        new SocialViewAssembler(userQueryRepository));

    assertThatThrownBy(() -> handler.handle(
        new CancelResourceInvitationCommand(invitation.getId().value().toString(),
            recipient.value().toString()))).isInstanceOf(
        OnlySenderCanCancelResourceInvitationException.class);
    assertThat(invitation.getStatus()).isEqualTo(ResourceInvitationStatus.PENDING);
    assertThat(saved).matches(wasSaved -> !wasSaved.get(), "not save invitation");
    assertThat(published).matches(wasPublished -> !wasPublished.get(), "not publish events");
  }

  private record FixedUserQueryRepository(Map<PlayerId, String> usernamesById) implements
      UserQueryRepository {

    private FixedUserQueryRepository(final Map<PlayerId, String> usernamesById) {

      this.usernamesById = new LinkedHashMap<>(usernamesById);
    }

    @Override
    public Map<PlayerId, String> findUsernamesByIds(final Set<PlayerId> playerIds) {

      return this.usernamesById.entrySet().stream()
          .filter(entry -> playerIds.contains(entry.getKey())).collect(
              Collectors.toMap(Entry::getKey, Entry::getValue, (left, right) -> left,
                  LinkedHashMap::new));
    }

    @Override
    public Optional<PlayerId> findUserIdByUsername(final String username) {

      return this.usernamesById.entrySet().stream()
          .filter(entry -> entry.getValue().equals(username)).map(Entry::getKey).findFirst();
    }

  }

  private record FixedInvitationQueryRepository(ResourceInvitation invitation) implements
      ResourceInvitationQueryRepository {

    @Override
    public Optional<ResourceInvitation> findById(final ResourceInvitationId invitationId) {

      return this.invitation.getId().equals(invitationId) ? Optional.of(this.invitation)
          : Optional.empty();
    }

    @Override
    public List<ResourceInvitation> findPendingReceivedBy(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<ResourceInvitation> findPendingInvitations() {

      return List.of();
    }

    @Override
    public boolean existsPendingBySenderAndRecipientAndTarget(final PlayerId senderId,
        final PlayerId recipientId, final ResourceInvitationTargetType targetType,
        final String targetId) {

      return false;
    }

    @Override
    public List<ResourceInvitation> findPendingByTarget(
        final ResourceInvitationTargetType targetType, final String targetId) {

      return List.of();
    }

    @Override
    public List<ResourceInvitation> findPendingSentBy(final PlayerId playerId) {

      return List.of();
    }

  }

}
