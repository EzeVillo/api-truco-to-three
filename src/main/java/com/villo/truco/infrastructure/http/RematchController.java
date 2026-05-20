package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.ChooseRematchCommand;
import com.villo.truco.application.commands.LeaveRematchCommand;
import com.villo.truco.application.ports.in.ChooseRematchUseCase;
import com.villo.truco.application.ports.in.GetRematchSessionUseCase;
import com.villo.truco.application.ports.in.LeaveRematchUseCase;
import com.villo.truco.application.queries.GetRematchSessionQuery;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.RematchSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Revancha", description = "Endpoints para gestionar sesiones de revancha")
public class RematchController {

  private final ChooseRematchUseCase chooseRematch;
  private final LeaveRematchUseCase leaveRematch;
  private final GetRematchSessionUseCase getRematchSession;

  public RematchController(final ChooseRematchUseCase chooseRematch,
      final LeaveRematchUseCase leaveRematch, final GetRematchSessionUseCase getRematchSession) {

    this.chooseRematch = Objects.requireNonNull(chooseRematch);
    this.leaveRematch = Objects.requireNonNull(leaveRematch);
    this.getRematchSession = Objects.requireNonNull(getRematchSession);
  }

  @PostMapping("/{matchId}/rematch/choose")
  @Operation(summary = "Elegir revancha", description = "El jugador autenticado acepta jugar la revancha", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Eleccion registrada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Session de revancha no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Accion invalida en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> chooseRematch(
      @Parameter(description = "ID de la partida original") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.chooseRematch.handle(new ChooseRematchCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/rematch/leave")
  @Operation(summary = "Salir de revancha", description = "El jugador autenticado rechaza o abandona la sesion de revancha", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Salida registrada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Session de revancha no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Accion invalida en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> leaveRematch(
      @Parameter(description = "ID de la partida original") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.leaveRematch.handle(new LeaveRematchCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{matchId}/rematch")
  @Operation(summary = "Obtener estado de revancha", description = "Devuelve el estado actual de la sesion de revancha para la partida dada", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado de la revancha", content = @Content(schema = @Schema(implementation = RematchSessionResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Session de revancha no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<RematchSessionResponse> getRematchSession(
      @Parameter(description = "ID de la partida original") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    final var snapshot = this.getRematchSession.handle(
        new GetRematchSessionQuery(matchId, jwt.getSubject()));
    return ResponseEntity.ok(RematchSessionResponse.from(snapshot));
  }

}
