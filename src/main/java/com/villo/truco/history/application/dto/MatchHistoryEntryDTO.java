package com.villo.truco.history.application.dto;

import java.util.UUID;

public record MatchHistoryEntryDTO(UUID matchId, String opponentName, boolean opponentIsBot,
                                   String outcome, String endReason, int ownGamesWon,
                                   int opponentGamesWon, long endedAt) {

}
