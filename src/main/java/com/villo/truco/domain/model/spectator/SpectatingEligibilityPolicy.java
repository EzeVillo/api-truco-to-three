package com.villo.truco.domain.model.spectator;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.spectator.exceptions.AlreadySpectatingException;
import com.villo.truco.domain.model.spectator.exceptions.CannotSpectateOwnMatchException;
import com.villo.truco.domain.model.spectator.exceptions.SpectateBotMatchNotOwnerException;
import com.villo.truco.domain.model.spectator.exceptions.SpectateNotAllowedException;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.ports.CompetitionMembershipResolver;
import com.villo.truco.domain.ports.FriendshipSpectateEligibilityResolver;
import java.util.Objects;

public final class SpectatingEligibilityPolicy {

  private final CompetitionMembershipResolver competitionMembershipResolver;
  private final FriendshipSpectateEligibilityResolver friendshipSpectateEligibilityResolver;
  private final BotVsBotMatchRegistry botVsBotMatchRegistry;

  public SpectatingEligibilityPolicy(
      final CompetitionMembershipResolver competitionMembershipResolver,
      final FriendshipSpectateEligibilityResolver friendshipSpectateEligibilityResolver,
      final BotVsBotMatchRegistry botVsBotMatchRegistry) {

    this.competitionMembershipResolver = Objects.requireNonNull(competitionMembershipResolver);
    this.friendshipSpectateEligibilityResolver = Objects.requireNonNull(
        friendshipSpectateEligibilityResolver);
    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
  }

  public void ensureCanStartWatching(final Spectatorship spectatorship, final Match match) {

    Objects.requireNonNull(spectatorship);
    Objects.requireNonNull(match);

    final var spectatorId = spectatorship.getId();

    if (match.getStatus() != MatchStatus.IN_PROGRESS) {
      throw new InvalidMatchStateException(match.getStatus(), MatchStatus.IN_PROGRESS);
    }

    if (spectatorId.equals(match.getPlayerOne()) || spectatorId.equals(match.getPlayerTwo())) {
      throw new CannotSpectateOwnMatchException();
    }

    if (this.botVsBotMatchRegistry.isBotVsBotMatch(match.getId())) {
      final var owner = this.botVsBotMatchRegistry.findOwnerByMatchId(match.getId());
      if (owner.isEmpty() || !owner.get().equals(spectatorId)) {
        throw new SpectateBotMatchNotOwnerException();
      }
    } else {
      final var canSpectateByCompetition = this.competitionMembershipResolver.belongsToSameCompetition(
          match.getId(), spectatorId);
      final var canSpectateByFriendship = this.friendshipSpectateEligibilityResolver.canSpectateAsFriend(
          match, spectatorId);

      if (!canSpectateByCompetition && !canSpectateByFriendship) {
        throw new SpectateNotAllowedException();
      }
    }

    if (spectatorship.isActive() && !spectatorship.isWatching(match.getId())) {
      throw new AlreadySpectatingException();
    }
  }

}
