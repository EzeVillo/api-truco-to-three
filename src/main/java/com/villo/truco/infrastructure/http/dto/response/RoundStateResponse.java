package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.RoundStateDTO;
import java.util.List;

public record RoundStateResponse(String status, String currentTurn, int scorePlayerOne,
                                 int scorePlayerTwo, List<CardResponse> myCards, String roundStatus,
                                 String currentTrucoCall, String winner,
                                 List<AvailableActionResponse> availableActions,
                                 List<PlayedHandResponse> playedHands,
                                 CurrentHandResponse currentHand) {

  public static RoundStateResponse from(final RoundStateDTO dto) {

    return new RoundStateResponse(dto.status(), dto.currentTurn(), dto.scorePlayerOne(),
        dto.scorePlayerTwo(), dto.myCards().stream().map(CardResponse::from).toList(),
        dto.roundStatus(), dto.currentTrucoCall(), dto.winner(),
        dto.availableActions().stream().map(AvailableActionResponse::from).toList(),
        dto.playedHands().stream().map(PlayedHandResponse::from).toList(),
        dto.currentHand() != null ? CurrentHandResponse.from(dto.currentHand()) : null);
  }

}
