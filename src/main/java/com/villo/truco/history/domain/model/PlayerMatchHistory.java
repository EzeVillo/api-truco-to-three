package com.villo.truco.history.domain.model;

import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class PlayerMatchHistory extends AggregateBase<PlayerId> {

  public static final int MAX_ENTRIES = 5;

  private final List<MatchHistoryEntry> entries = new ArrayList<>();

  private PlayerMatchHistory(final PlayerId playerId) {

    super(Objects.requireNonNull(playerId));
  }

  public static PlayerMatchHistory create(final PlayerId playerId) {

    return new PlayerMatchHistory(playerId);
  }

  public static PlayerMatchHistory reconstruct(final PlayerId playerId,
      final List<MatchHistoryEntry> entries) {

    final var history = new PlayerMatchHistory(playerId);
    history.entries.addAll(entries);
    history.sortAndTrim();
    return history;
  }

  public boolean record(final MatchHistoryEntry entry) {

    Objects.requireNonNull(entry, "entry cannot be null");
    final var alreadyPresent = this.entries.stream()
        .anyMatch(existing -> existing.matchId().equals(entry.matchId()));
    if (alreadyPresent) {
      return false;
    }
    this.entries.add(entry);
    this.sortAndTrim();
    return true;
  }

  public List<MatchHistoryEntry> getEntries() {

    return List.copyOf(this.entries);
  }

  public PlayerMatchHistorySnapshot snapshot() {

    return new PlayerMatchHistorySnapshot(this.getId(), List.copyOf(this.entries));
  }

  private void sortAndTrim() {

    this.entries.sort(Comparator.comparing(MatchHistoryEntry::endedAt).reversed());
    if (this.entries.size() > MAX_ENTRIES) {
      this.entries.subList(MAX_ENTRIES, this.entries.size()).clear();
    }
  }

}
