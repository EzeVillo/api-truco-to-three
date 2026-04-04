package com.villo.truco.application.dto;

public record PublicLeagueLobbyDTO(String leagueId, String host, int gamesToPlay, int totalSlots,
                                   int occupiedSlots, String status) {

}
