package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.SpectatorshipStopReason;
import com.villo.truco.domain.ports.CompetitionMembershipResolver;
import com.villo.truco.domain.ports.FriendshipSpectateEligibilityResolver;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.events.FriendshipRemovedEvent;
import java.util.Objects;

public final class SpectatorCleanupOnFriendshipRemovedEventHandler implements
    DomainEventHandler<FriendshipRemovedEvent> {

  private final SpectatorshipRepository spectatorshipRepository;
  private final MatchQueryRepository matchQueryRepository;
  private final CompetitionMembershipResolver competitionMembershipResolver;
  private final FriendshipSpectateEligibilityResolver friendshipSpectateEligibilityResolver;
  private final SpectatorshipLifecycleManager lifecycleManager;

  public SpectatorCleanupOnFriendshipRemovedEventHandler(
      final SpectatorshipRepository spectatorshipRepository,
      final MatchQueryRepository matchQueryRepository,
      final CompetitionMembershipResolver competitionMembershipResolver,
      final FriendshipSpectateEligibilityResolver friendshipSpectateEligibilityResolver,
      final SpectatorshipLifecycleManager lifecycleManager) {

    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.competitionMembershipResolver = Objects.requireNonNull(competitionMembershipResolver);
    this.friendshipSpectateEligibilityResolver = Objects.requireNonNull(
        friendshipSpectateEligibilityResolver);
    this.lifecycleManager = Objects.requireNonNull(lifecycleManager);
  }

  @Override
  public Class<FriendshipRemovedEvent> eventType() {

    return FriendshipRemovedEvent.class;
  }

  @Override
  public void handle(final FriendshipRemovedEvent event) {

    this.stopIfNoLongerEligible(event.getRequesterId());
    this.stopIfNoLongerEligible(event.getAddresseeId());
  }

  private void stopIfNoLongerEligible(final PlayerId spectatorId) {

    this.spectatorshipRepository.findBySpectatorId(spectatorId)
        .flatMap(Spectatorship::getActiveMatchId).flatMap(this.matchQueryRepository::findById)
        .filter(match -> !this.canContinueWatching(match, spectatorId)).ifPresent(
            match -> this.lifecycleManager.forceStop(spectatorId,
                SpectatorshipStopReason.FRIENDSHIP_REMOVED));
  }

  private boolean canContinueWatching(final Match match, final PlayerId spectatorId) {

    return this.competitionMembershipResolver.belongsToSameCompetition(match.getId(), spectatorId)
        || this.friendshipSpectateEligibilityResolver.canSpectateAsFriend(match, spectatorId);
  }

}
