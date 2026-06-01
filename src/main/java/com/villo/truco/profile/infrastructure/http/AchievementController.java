package com.villo.truco.profile.infrastructure.http;

import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.profile.application.usecases.queries.GetAchievementCatalogUseCase;
import com.villo.truco.profile.infrastructure.http.dto.response.AchievementCatalogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
@Tag(name = "Achievements", description = "Catálogo de logros existentes en el juego")
public class AchievementController {

  private final GetAchievementCatalogUseCase getAchievementCatalogUseCase;

  public AchievementController(final GetAchievementCatalogUseCase getAchievementCatalogUseCase) {

    this.getAchievementCatalogUseCase = Objects.requireNonNull(getAchievementCatalogUseCase);
  }

  @GetMapping
  @Operation(summary = "Obtener catálogo de logros", description = "Devuelve la lista completa de logros existentes en el juego (sus códigos). Es idéntica para todos los jugadores e independiente del progreso. Accesible para cualquier usuario autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Catálogo de logros", content = @Content(schema = @Schema(implementation = AchievementCatalogResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<AchievementCatalogResponse> getCatalog() {

    final var dto = this.getAchievementCatalogUseCase.get();
    return ResponseEntity.ok(AchievementCatalogResponse.from(dto));
  }

}
