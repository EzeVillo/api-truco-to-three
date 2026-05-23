package com.villo.truco.domain.model.quickmatch;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Objects;

public record QuickMatchTicket(PlayerId playerId, GamesToPlay gamesToPlay, Instant enqueuedAt,
                               String webSocketSessionId) {

  public QuickMatchTicket(final PlayerId playerId, final GamesToPlay gamesToPlay,
      final Instant enqueuedAt, final String webSocketSessionId) {

    this.playerId = Objects.requireNonNull(playerId, "PlayerId cannot be null");
    this.gamesToPlay = Objects.requireNonNull(gamesToPlay, "GamesToPlay cannot be null");
    this.enqueuedAt = Objects.requireNonNull(enqueuedAt, "EnqueuedAt cannot be null");
    this.webSocketSessionId = webSocketSessionId;
  }

}
