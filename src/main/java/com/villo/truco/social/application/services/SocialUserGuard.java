package com.villo.truco.social.application.services;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.exceptions.SocialFeatureRequiresRegisteredUserException;
import com.villo.truco.social.application.exceptions.SocialUserNotFoundException;
import java.util.Objects;
import java.util.Set;

public final class SocialUserGuard {

  private final UserQueryRepository userQueryRepository;

  public SocialUserGuard(final UserQueryRepository userQueryRepository) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
  }

  public void ensureRegisteredUser(final PlayerId playerId) {

    final var users = this.userQueryRepository.findUsernamesByIds(Set.of(playerId));
    if (!users.containsKey(playerId)) {
      throw new SocialFeatureRequiresRegisteredUserException();
    }
  }

  public PlayerId findRegisteredUserIdByUsername(final String username) {

    return this.userQueryRepository.findUserIdByUsername(username)
        .orElseThrow(() -> new SocialUserNotFoundException(username));
  }

}
