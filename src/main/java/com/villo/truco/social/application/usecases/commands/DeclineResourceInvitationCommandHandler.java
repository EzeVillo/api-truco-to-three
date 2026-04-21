package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.social.application.commands.DeclineResourceInvitationCommand;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.application.ports.in.DeclineResourceInvitationUseCase;
import com.villo.truco.social.application.services.ResourceInvitationResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.util.Objects;

public final class DeclineResourceInvitationCommandHandler implements
    DeclineResourceInvitationUseCase {

  private final SocialUserGuard socialUserGuard;
  private final ResourceInvitationResolver resourceInvitationResolver;
  private final ResourceInvitationRepository resourceInvitationRepository;
  private final SocialEventNotifier socialEventNotifier;
  private final SocialViewAssembler socialViewAssembler;

  public DeclineResourceInvitationCommandHandler(final SocialUserGuard socialUserGuard,
      final ResourceInvitationResolver resourceInvitationResolver,
      final ResourceInvitationRepository resourceInvitationRepository,
      final SocialEventNotifier socialEventNotifier,
      final SocialViewAssembler socialViewAssembler) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.resourceInvitationResolver = Objects.requireNonNull(resourceInvitationResolver);
    this.resourceInvitationRepository = Objects.requireNonNull(resourceInvitationRepository);
    this.socialEventNotifier = Objects.requireNonNull(socialEventNotifier);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
  }

  @Override
  public ResourceInvitationDTO handle(final DeclineResourceInvitationCommand command) {

    this.socialUserGuard.ensureRegisteredUser(command.actorId());

    final var invitation = this.resourceInvitationResolver.resolve(command.invitationId());
    invitation.decline(command.actorId());
    this.saveAndPublish(invitation);

    return this.socialViewAssembler.toInvitationDto(invitation);
  }

  private void saveAndPublish(final ResourceInvitation invitation) {

    this.resourceInvitationRepository.save(invitation);
    this.socialEventNotifier.publishDomainEvents(invitation.getResourceInvitationDomainEvents());
    invitation.clearDomainEvents();
  }

}
