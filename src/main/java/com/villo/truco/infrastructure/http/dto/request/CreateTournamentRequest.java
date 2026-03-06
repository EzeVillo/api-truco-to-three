package com.villo.truco.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Solicitud para crear un torneo")
public record CreateTournamentRequest(
    @ArraySchema(schema = @Schema(description = "ID de jugador", example = "player-1"), arraySchema = @Schema(description = "Lista de IDs de jugadores participantes")) List<String> playerIds) {

}
