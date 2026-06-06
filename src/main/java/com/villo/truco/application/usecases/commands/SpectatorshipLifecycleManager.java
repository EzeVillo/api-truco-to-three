package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.SpectatorshipStopReason;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
import java.util.List;
import java.util.Objects;

public final class SpectatorshipLifecycleManager {

  private final SpectatorshipRepository spectatorshipRepository;
  private final SpectatorCountChangedPublisher countChangedPublisher;
  private final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier;
  private final PresenceNotifier presenceNotifier;

  public SpectatorshipLifecycleManager(final SpectatorshipRepository spectatorshipRepository,
      final SpectatorCountChangedPublisher countChangedPublisher,
      final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier,
      final PresenceNotifier presenceNotifier) {

    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.countChangedPublisher = Objects.requireNonNull(countChangedPublisher);
    this.friendAvailabilityChangeNotifier = Objects.requireNonNull(
        friendAvailabilityChangeNotifier);
    this.presenceNotifier = Objects.requireNonNull(presenceNotifier);
  }

  public void stopManually(final PlayerId spectatorId) {

    final var spectatorship = this.spectatorshipRepository.findBySpectatorId(spectatorId)
        .orElseGet(() -> Spectatorship.create(spectatorId));
    final var matchId = spectatorship.stopWatching(SpectatorshipStopReason.MANUAL);

    this.spectatorshipRepository.save(spectatorship);
    this.countChangedPublisher.publishFor(matchId);
    this.friendAvailabilityChangeNotifier.notifyAvailabilityChanged(spectatorId,
        System.currentTimeMillis());
    this.presenceNotifier.notifyPlayers(List.of(spectatorId));
  }

  public void forceStop(final PlayerId spectatorId, final SpectatorshipStopReason reason) {

    this.spectatorshipRepository.findBySpectatorId(spectatorId).ifPresent(spectatorship -> {
      if (!spectatorship.isActive()) {
        return;
      }

      final var matchId = spectatorship.stopWatching(reason);
      this.spectatorshipRepository.save(spectatorship);
      this.countChangedPublisher.publishFor(matchId);
      this.friendAvailabilityChangeNotifier.notifyAvailabilityChanged(spectatorId,
          System.currentTimeMillis());
      this.presenceNotifier.notifyPlayers(List.of(spectatorId));
    });
  }

  public void clearMatchSpectators(final MatchId matchId) {

    final var spectatorships = this.spectatorshipRepository.findActiveByMatchId(matchId);
    if (spectatorships.isEmpty()) {
      return;
    }

    final var spectatorIds = spectatorships.stream().map(Spectatorship::getId).toList();
    for (final var spectatorship : spectatorships) {
      spectatorship.stopWatching(SpectatorshipStopReason.MATCH_ENDED);
      this.spectatorshipRepository.save(spectatorship);
    }

    this.countChangedPublisher.publishFor(matchId);
    for (final var spectatorId : spectatorIds) {
      this.friendAvailabilityChangeNotifier.notifyAvailabilityChanged(spectatorId,
          System.currentTimeMillis());
    }
    this.presenceNotifier.notifyPlayers(spectatorIds);
  }

}
