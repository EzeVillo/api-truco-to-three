package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.ActiveCupRefDTO;
import com.villo.truco.application.dto.ActiveLeagueRefDTO;
import com.villo.truco.application.dto.ActiveMatchRefDTO;
import com.villo.truco.application.dto.ActiveRematchRefDTO;
import com.villo.truco.application.dto.UserPresenceDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de presencia/ocupacion del usuario autenticado")
public record UserPresenceResponse(
    @Schema(description = "Verdadero si el usuario esta ocupado en al menos un dominio", example = "true") boolean busy,
    @Schema(description = "Partida no finalizada del usuario, o null", nullable = true) ActiveMatchRef match,
    @Schema(description = "Liga del usuario, o null", nullable = true) ActiveLeagueRef league,
    @Schema(description = "Copa del usuario, o null", nullable = true) ActiveCupRef cup,
    @Schema(description = "Sesion de revancha abierta del usuario, o null", nullable = true) ActiveRematchRef rematch) {

  public static UserPresenceResponse from(final UserPresenceDTO presence) {

    return new UserPresenceResponse(presence.busy(), ActiveMatchRef.from(presence.match()),
        ActiveLeagueRef.from(presence.league()), ActiveCupRef.from(presence.cup()),
        ActiveRematchRef.from(presence.rematch()));
  }

  @Schema(description = "Referencia a la partida activa")
  public record ActiveMatchRef(
      @Schema(description = "ID de la partida", example = "550e8400-e29b-41d4-a716-446655440000") String id,
      @Schema(description = "Estado de la partida", example = "IN_PROGRESS") String status) {

    static ActiveMatchRef from(final ActiveMatchRefDTO dto) {

      return dto == null ? null : new ActiveMatchRef(dto.id(), dto.status());
    }

  }

  @Schema(description = "Referencia a la liga activa")
  public record ActiveLeagueRef(
      @Schema(description = "ID de la liga", example = "550e8400-e29b-41d4-a716-446655440001") String id,
      @Schema(description = "Estado de la liga", example = "IN_PROGRESS") String status,
      @Schema(description = "ID de la partida actual del torneo (solo si en progreso)", example = "550e8400-e29b-41d4-a716-446655440000", nullable = true) String currentMatchId) {

    static ActiveLeagueRef from(final ActiveLeagueRefDTO dto) {

      return dto == null ? null : new ActiveLeagueRef(dto.id(), dto.status(), dto.currentMatchId());
    }

  }

  @Schema(description = "Referencia a la copa activa")
  public record ActiveCupRef(
      @Schema(description = "ID de la copa", example = "550e8400-e29b-41d4-a716-446655440002") String id,
      @Schema(description = "Estado de la copa", example = "IN_PROGRESS") String status,
      @Schema(description = "ID de la partida actual del torneo (solo si en progreso)", example = "550e8400-e29b-41d4-a716-446655440000", nullable = true) String currentMatchId) {

    static ActiveCupRef from(final ActiveCupRefDTO dto) {

      return dto == null ? null : new ActiveCupRef(dto.id(), dto.status(), dto.currentMatchId());
    }

  }

  @Schema(description = "Referencia a la sesion de revancha activa")
  public record ActiveRematchRef(
      @Schema(description = "ID de la sesion de revancha", example = "550e8400-e29b-41d4-a716-446655440003") String id,
      @Schema(description = "ID de la partida de origen", example = "550e8400-e29b-41d4-a716-446655440000") String originMatchId) {

    static ActiveRematchRef from(final ActiveRematchRefDTO dto) {

      return dto == null ? null : new ActiveRematchRef(dto.id(), dto.originMatchId());
    }

  }

}
