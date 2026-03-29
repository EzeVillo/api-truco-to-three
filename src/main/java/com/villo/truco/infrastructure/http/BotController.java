package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.application.ports.in.GetBotsUseCase;
import com.villo.truco.application.queries.GetBotsQuery;
import com.villo.truco.infrastructure.http.dto.request.CreateBotMatchRequest;
import com.villo.truco.infrastructure.http.dto.response.BotProfileResponse;
import com.villo.truco.infrastructure.http.dto.response.CreateBotMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Bots", description = "Endpoints para consultar bots y crear partidas contra ellos")
public class BotController {

  private final GetBotsUseCase getBots;
  private final CreateBotMatchUseCase createBotMatch;

  public BotController(final GetBotsUseCase getBots, final CreateBotMatchUseCase createBotMatch) {

    this.getBots = Objects.requireNonNull(getBots);
    this.createBotMatch = Objects.requireNonNull(createBotMatch);
  }

  @GetMapping("/bots")
  @Operation(summary = "Listar bots disponibles", description = "Devuelve todos los bots predefinidos con sus stats de personalidad", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de bots", content = @Content(schema = @Schema(implementation = BotProfileResponse.class)))})
  public ResponseEntity<List<BotProfileResponse>> getBots() {

    final var bots = this.getBots.handle(new GetBotsQuery()).stream().map(BotProfileResponse::from)
        .toList();
    return ResponseEntity.ok(bots);
  }

  @PostMapping("/matches/bot")
  @Operation(summary = "Crear partida contra bot", description = "Crea una partida ya iniciada contra el bot elegido", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Partida creada", content = @Content(schema = @Schema(implementation = CreateBotMatchResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Bot no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo crear la partida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateBotMatchResponse> createBotMatch(
      @Valid @RequestBody final CreateBotMatchRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.createBotMatch.handle(
        new CreateBotMatchCommand(jwt.getSubject(), request.gamesToPlay(), request.botId()));
    return ResponseEntity.ok(CreateBotMatchResponse.from(dto));
  }

}
