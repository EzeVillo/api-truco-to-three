package com.villo.truco.application.dto;

import java.util.List;

public record SpectatorRoundStateDTO(String status, String currentTurn, String roundStatus,
                                     String currentTrucoCall, String winner,
                                     List<PlayedHandDTO> playedHands, CurrentHandDTO currentHand) {

}
