package com.villo.truco.application.dto;

import java.util.List;

public record RoundStateDTO(String status, String currentTurn, int scorePlayerOne,
                            int scorePlayerTwo, List<CardDTO> myCards, String roundStatus,
                            String currentTrucoCall, String winner,
                            List<AvailableActionDTO> availableActions,
                            List<PlayedHandDTO> playedHands, CurrentHandDTO currentHand) {

}
