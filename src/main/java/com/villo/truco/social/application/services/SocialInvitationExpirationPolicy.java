package com.villo.truco.social.application.services;

import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Duration;
import java.util.Objects;

public final class SocialInvitationExpirationPolicy {

  private final Duration matchExpiration;
  private final Duration leagueExpiration;
  private final Duration cupExpiration;

  public SocialInvitationExpirationPolicy(final Duration matchExpiration,
      final Duration leagueExpiration, final Duration cupExpiration) {

    this.matchExpiration = Objects.requireNonNull(matchExpiration);
    this.leagueExpiration = Objects.requireNonNull(leagueExpiration);
    this.cupExpiration = Objects.requireNonNull(cupExpiration);
  }

  public Duration expirationFor(final ResourceInvitationTargetType targetType) {

    return switch (targetType) {
      case MATCH -> this.matchExpiration;
      case LEAGUE -> this.leagueExpiration;
      case CUP -> this.cupExpiration;
    };
  }

}
