package com.villo.truco.infrastructure.http;

import com.villo.truco.application.ports.in.GetUserPresenceUseCase;
import com.villo.truco.application.queries.GetUserPresenceQuery;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.UserPresenceResponse;
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
@RequestMapping("/api/me")
@Tag(name = "Presencia", description = "Estado de ocupacion del usuario autenticado para reconexion")
public class PresenceController {

  private final GetUserPresenceUseCase getUserPresence;

  public PresenceController(final GetUserPresenceUseCase getUserPresence) {

    this.getUserPresence = Objects.requireNonNull(getUserPresence);
  }

  @GetMapping("/presence")
  @Operation(summary = "Obtener presencia del usuario", description = "Devuelve, para el usuario autenticado, en que partida, liga, copa o revancha esta ocupado, con los identificadores necesarios para reconectarse. Operacion de solo lectura.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado de presencia del usuario", content = @Content(schema = @Schema(implementation = UserPresenceResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<UserPresenceResponse> getPresence(@AuthenticationPrincipal final Jwt jwt) {

    final var presence = this.getUserPresence.handle(new GetUserPresenceQuery(jwt.getSubject()));
    return ResponseEntity.ok(UserPresenceResponse.from(presence));
  }

}
