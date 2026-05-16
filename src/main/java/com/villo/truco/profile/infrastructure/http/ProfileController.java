package com.villo.truco.profile.infrastructure.http;

import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.profile.application.usecases.queries.GetPlayerProfileQuery;
import com.villo.truco.profile.application.usecases.queries.GetPlayerProfileUseCase;
import com.villo.truco.profile.infrastructure.http.dto.response.PlayerProfileResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Perfil de jugador con logros y estadísticas")
public class ProfileController {

  private final GetPlayerProfileUseCase getPlayerProfileUseCase;

  public ProfileController(final GetPlayerProfileUseCase getPlayerProfileUseCase) {

    this.getPlayerProfileUseCase = Objects.requireNonNull(getPlayerProfileUseCase);
  }

  @GetMapping("/{username}")
  @Operation(summary = "Obtener perfil de jugador", description = "Devuelve el username, logros desbloqueados y estadísticas agregadas del jugador indicado. Accesible para cualquier usuario autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Perfil del jugador", content = @Content(schema = @Schema(implementation = PlayerProfileResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Jugador no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<PlayerProfileResponse> getProfile(
      @Parameter(description = "Username del jugador", example = "juancho") @PathVariable final String username,
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.getPlayerProfileUseCase.handle(new GetPlayerProfileQuery(username));
    return ResponseEntity.ok(PlayerProfileResponse.from(dto));
  }

}
