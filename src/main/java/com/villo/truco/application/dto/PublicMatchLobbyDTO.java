package com.villo.truco.application.dto;

public record PublicMatchLobbyDTO(String matchId, String host, int gamesToPlay, int totalSlots,
                                  int occupiedSlots, String status, String joinCode) {

}
