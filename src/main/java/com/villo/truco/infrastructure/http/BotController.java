package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.AbandonBotVsBotMatchCommand;
import com.villo.truco.application.commands.AdvanceBotVsBotMatchCommand;
import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.commands.CreateBotVsBotMatchCommand;
import com.villo.truco.application.ports.in.AbandonBotVsBotMatchUseCase;
import com.villo.truco.application.ports.in.AdvanceBotVsBotMatchUseCase;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.application.ports.in.CreateBotVsBotMatchUseCase;
import com.villo.truco.application.ports.in.GetBotsUseCase;
import com.villo.truco.application.queries.GetBotsQuery;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.http.dto.request.CreateBotMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.CreateBotVsBotMatchRequest;
import com.villo.truco.infrastructure.http.dto.response.BotCatalogResponse;
import com.villo.truco.infrastructure.http.dto.response.CreateBotMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.CreateBotVsBotMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final CreateBotVsBotMatchUseCase createBotVsBotMatch;
  private final AbandonBotVsBotMatchUseCase abandonBotVsBotMatch;
  private final AdvanceBotVsBotMatchUseCase advanceBotVsBotMatch;

  public BotController(final GetBotsUseCase getBots, final CreateBotMatchUseCase createBotMatch,
      final CreateBotVsBotMatchUseCase createBotVsBotMatch,
      final AbandonBotVsBotMatchUseCase abandonBotVsBotMatch,
      final AdvanceBotVsBotMatchUseCase advanceBotVsBotMatch) {

    this.getBots = Objects.requireNonNull(getBots);
    this.createBotMatch = Objects.requireNonNull(createBotMatch);
    this.createBotVsBotMatch = Objects.requireNonNull(createBotVsBotMatch);
    this.abandonBotVsBotMatch = Objects.requireNonNull(abandonBotVsBotMatch);
    this.advanceBotVsBotMatch = Objects.requireNonNull(advanceBotVsBotMatch);
  }

  @GetMapping("/bots")
  @Operation(summary = "Listar bots disponibles", description = "Devuelve los bots casuales y los bots de campaña desbloqueados por el jugador (historial neto ≥ 3 a favor)", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Catálogo de bots", content = @Content(schema = @Schema(implementation = BotCatalogResponse.class)))})
  public ResponseEntity<BotCatalogResponse> getBots(@AuthenticationPrincipal final Jwt jwt) {

    final var catalog = this.getBots.handle(new GetBotsQuery(PlayerId.of(jwt.getSubject())));
    return ResponseEntity.ok(BotCatalogResponse.from(catalog));
  }

  @PostMapping("/matches/bot")
  @Operation(summary = "Crear partida contra bot", description = "Crea una partida ya iniciada contra el bot elegido. Las partidas contra bot no generan un chat asociado al match.", security = @SecurityRequirement(name = "bearerAuth"))
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

  @PostMapping("/matches/bot-vs-bot")
  @Operation(summary = "Crear partida entre dos bots", description = "Crea una partida ya iniciada entre dos bots que juegan solos. Solo el creador puede espectarla y queda ocupado por autoría hasta que termine. No genera chat ni revancha.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Partida creada", content = @Content(schema = @Schema(implementation = CreateBotVsBotMatchResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Alguno de los bots no existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "El usuario ya está ocupado, o ambos bots son iguales", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateBotVsBotMatchResponse> createBotVsBotMatch(
      @Valid @RequestBody final CreateBotVsBotMatchRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.createBotVsBotMatch.handle(
        new CreateBotVsBotMatchCommand(jwt.getSubject(), request.gamesToPlay(), request.botOneId(),
            request.botTwoId()));
    return ResponseEntity.ok(CreateBotVsBotMatchResponse.from(dto));
  }

  @PostMapping("/matches/bot-vs-bot/{matchId}/abandon")
  @Operation(summary = "Abandonar partida entre bots", description = "El creador corta anticipadamente su partida bot-vs-bot en curso; la serie termina y queda liberado. Solo el creador puede abandonarla.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Partida abandonada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Partida no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "El usuario no es el creador de la partida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> abandonBotVsBotMatch(@PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.abandonBotVsBotMatch.handle(new AbandonBotVsBotMatchCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/matches/bot-vs-bot/{matchId}/advance")
  @Operation(summary = "Avanzar una jugada de la partida entre bots", description = "Las partidas bot-vs-bot no avanzan solas: cada llamada ejecuta exactamente la próxima acción del bot al que le toca (jugar carta, cantar o responder). Solo el creador puede avanzarla. El nuevo estado llega por el canal de espectado en tiempo real.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Jugada avanzada (o sin acción pendiente)"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Partida no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "El usuario no es el creador de la partida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> advanceBotVsBotMatch(@PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.advanceBotVsBotMatch.handle(new AdvanceBotVsBotMatchCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

}
