package com.villo.truco.domain.model.spectator;

import com.villo.truco.domain.model.spectator.exceptions.AlreadySpectatingException;
import com.villo.truco.domain.model.spectator.exceptions.NotSpectatingException;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;
import java.util.Optional;

public final class Spectatorship extends AggregateBase<PlayerId> {

  private MatchId activeMatchId;

  private Spectatorship(final PlayerId spectatorId, final MatchId activeMatchId) {

    super(spectatorId);
    this.activeMatchId = activeMatchId;
  }

  public static Spectatorship create(final PlayerId spectatorId) {

    return new Spectatorship(Objects.requireNonNull(spectatorId), null);
  }

  public static Spectatorship reconstruct(final PlayerId spectatorId, final MatchId activeMatchId) {

    return new Spectatorship(Objects.requireNonNull(spectatorId), activeMatchId);
  }

  public void startWatching(final MatchId matchId) {

    if (this.activeMatchId != null) {
      throw new AlreadySpectatingException();
    }
    this.activeMatchId = Objects.requireNonNull(matchId);
  }

  public MatchId stopWatching(final SpectatorshipStopReason reason) {

    Objects.requireNonNull(reason);

    if (this.activeMatchId == null) {
      throw new NotSpectatingException(this.id);
    }

    final var previousMatchId = this.activeMatchId;
    this.activeMatchId = null;
    return previousMatchId;
  }

  public Optional<MatchId> getActiveMatchId() {

    return Optional.ofNullable(this.activeMatchId);
  }

  public boolean isActive() {

    return this.activeMatchId != null;
  }

  public boolean isWatching(final MatchId matchId) {

    return this.activeMatchId != null && this.activeMatchId.equals(matchId);
  }

}
