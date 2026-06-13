package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.LobbyStateDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de la sala de espera (lobby). Presente solo mientras el match espera "
    + "jugadores o el arranque; null una vez en juego.")
public record LobbyStateResponse(
    @Schema(description = "Visibilidad de la partida", example = "PRIVATE", allowableValues = {
        "PUBLIC", "PRIVATE"}) String visibility,
    @Schema(description = "Codigo para invitar/unirse a la partida", example = "ABCD2345") String joinCode,
    @Schema(description = "Epoch millis en que la sala expira por inactividad", example = "1718246400000", nullable = true) Long lobbyTimeoutDeadline,
    @Schema(description = "Si el jugador en el asiento PLAYER_ONE marco listo", example = "true") boolean readyPlayerOne,
    @Schema(description = "Si el jugador en el asiento PLAYER_TWO marco listo", example = "false") boolean readyPlayerTwo) {

  public static LobbyStateResponse from(final LobbyStateDTO dto) {

    return dto == null ? null
        : new LobbyStateResponse(dto.visibility(), dto.joinCode(), dto.lobbyTimeoutDeadline(),
            dto.readyPlayerOne(), dto.readyPlayerTwo());
  }

}
