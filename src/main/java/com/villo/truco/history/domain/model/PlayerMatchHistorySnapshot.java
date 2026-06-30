package com.villo.truco.history.domain.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record PlayerMatchHistorySnapshot(PlayerId playerId, List<MatchHistoryEntry> entries) {

}
