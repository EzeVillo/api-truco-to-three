package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.application.ports.in.GetSentResourceInvitationsUseCase;
import com.villo.truco.social.application.queries.GetSentResourceInvitationsQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.util.List;
import java.util.Objects;

public final class GetSentResourceInvitationsQueryHandler implements
    GetSentResourceInvitationsUseCase {

  private final SocialUserGuard socialUserGuard;
  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;
  private final SocialViewAssembler socialViewAssembler;

  public GetSentResourceInvitationsQueryHandler(final SocialUserGuard socialUserGuard,
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final SocialViewAssembler socialViewAssembler) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.resourceInvitationQueryRepository = Objects.requireNonNull(
        resourceInvitationQueryRepository);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
  }

  @Override
  public List<ResourceInvitationDTO> handle(final GetSentResourceInvitationsQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.senderId());
    return this.resourceInvitationQueryRepository.findPendingSentBy(query.senderId()).stream()
        .map(this.socialViewAssembler::toInvitationDto).toList();
  }

}
