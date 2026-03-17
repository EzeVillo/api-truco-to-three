package com.villo.truco.infrastructure.persistence.entities;

import java.util.List;
import java.util.UUID;

public record RoundData(UUID id, int roundNumber, UUID mano, UUID playerOne, UUID playerTwo,
                        HandData handPlayerOne, HandData handPlayerTwo,
                        List<PlayedHandData> playedHands, List<CardPlayData> currentHandCards,
                        TrucoData trucoStateMachine, EnvidoData envidoStateMachine, String status,
                        UUID currentTurn, UUID turnBeforeTrucoCall, UUID turnBeforeEnvidoCall) {

  public record HandData(UUID id, List<CardData> cards) {

  }

  public record CardData(String suit, int number) {

  }

  public record PlayedHandData(CardData cardMano, CardData cardPie, UUID winner) {

  }

  public record CardPlayData(UUID playerId, CardData card) {

  }

  public record TrucoData(String currentCall, UUID caller, int pointsAtStake) {

  }

  public record EnvidoData(List<String> chain, boolean resolved) {

  }

}
