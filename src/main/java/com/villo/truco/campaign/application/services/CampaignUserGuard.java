package com.villo.truco.campaign.application.services;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.campaign.application.exceptions.CampaignRequiresRegisteredUserException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;
import java.util.Set;

public final class CampaignUserGuard {

  private final UserQueryRepository userQueryRepository;

  public CampaignUserGuard(final UserQueryRepository userQueryRepository) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
  }

  public void ensureRegisteredUser(final PlayerId playerId) {

    final var users = this.userQueryRepository.findUsernamesByIds(Set.of(playerId));
    if (!users.containsKey(playerId)) {
      throw new CampaignRequiresRegisteredUserException();
    }
  }

}
