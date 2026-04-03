package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.events.SpectatorCountChanged;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class SpectatorCountChangedPublisher {

  private final MatchQueryRepository matchQueryRepository;
  private final SpectatorshipRepository spectatorshipRepository;
  private final ApplicationEventPublisher eventPublisher;

  public SpectatorCountChangedPublisher(final MatchQueryRepository matchQueryRepository,
      final SpectatorshipRepository spectatorshipRepository,
      final ApplicationEventPublisher eventPublisher) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.eventPublisher = Objects.requireNonNull(eventPublisher);
  }

  public void publishFor(final MatchId matchId) {

    final var spectators = this.spectatorshipRepository.findActiveSpectatorIdsByMatchId(matchId);
    final var count = spectators.size();
    final var players = this.matchQueryRepository.findById(matchId).map(
        match -> Stream.of(match.getPlayerOne(), match.getPlayerTwo()).filter(Objects::nonNull)
            .toList()).orElse(List.of());

    this.eventPublisher.publish(new SpectatorCountChanged(matchId, players, spectators, count));
  }

  public int countByMatch(final MatchId matchId) {

    return this.spectatorshipRepository.countActiveByMatchId(matchId);
  }

}
