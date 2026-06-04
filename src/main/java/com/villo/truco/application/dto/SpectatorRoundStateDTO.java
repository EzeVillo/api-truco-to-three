package com.villo.truco.application.dto;

import java.util.List;

public record SpectatorRoundStateDTO(String status, String currentTurn, String roundStatus,
                                     String currentTrucoCall, String currentEnvidoCall,
                                     String winner, List<PlayedHandDTO> playedHands,
                                     CurrentHandDTO currentHand, Long actionDeadline,
                                     Long turnDurationMillis, String actionDeadlineSeat) {

}
