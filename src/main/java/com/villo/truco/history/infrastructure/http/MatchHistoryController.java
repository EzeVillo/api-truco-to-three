package com.villo.truco.history.infrastructure.http;

import com.villo.truco.history.application.usecases.queries.GetPlayerMatchHistoryQuery;
import com.villo.truco.history.application.usecases.queries.GetPlayerMatchHistoryUseCase;
import com.villo.truco.history.infrastructure.http.dto.response.MatchHistoryResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match-history")
@Tag(name = "Match History", description = "Historial de las últimas partidas del jugador")
public class MatchHistoryController {

  private final GetPlayerMatchHistoryUseCase getPlayerMatchHistoryUseCase;

  public MatchHistoryController(final GetPlayerMatchHistoryUseCase getPlayerMatchHistoryUseCase) {

    this.getPlayerMatchHistoryUseCase = Objects.requireNonNull(getPlayerMatchHistoryUseCase);
  }

  @GetMapping
  @Operation(summary = "Obtener mi historial de partidas", description = "Devuelve las últimas partidas terminadas del usuario autenticado (máximo 5, más reciente primero), con el rival, el resultado, los juegos y cómo terminó cada una.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Historial de partidas", content = @Content(schema = @Schema(implementation = MatchHistoryResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<MatchHistoryResponse> getMyHistory(@AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.getPlayerMatchHistoryUseCase.handle(
        new GetPlayerMatchHistoryQuery(jwt.getSubject()));
    return ResponseEntity.ok(MatchHistoryResponse.from(dto));
  }

}
