package com.villo.truco.application.dto;

public record LobbyStateDTO(String visibility, String joinCode, Long lobbyTimeoutDeadline,
                            boolean readyPlayerOne, boolean readyPlayerTwo) {

}
