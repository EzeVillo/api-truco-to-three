package com.villo.truco.application.dto;

public record PublicCupLobbyDTO(String cupId, String host, int gamesToPlay, int totalSlots,
                                int occupiedSlots, String status) {

}
