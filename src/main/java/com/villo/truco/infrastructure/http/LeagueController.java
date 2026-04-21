package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CreateLeagueCommand;
import com.villo.truco.application.commands.LeaveLeagueCommand;
import com.villo.truco.application.commands.StartLeagueCommand;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.GetPublicLeaguesUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.application.queries.GetLeagueStateQuery;
import com.villo.truco.application.queries.GetPublicLeaguesQuery;
import com.villo.truco.infrastructure.http.dto.request.CreateLeagueRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateLeagueResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.LeagueStateResponse;
import com.villo.truco.infrastructure.http.dto.response.PublicLeagueLobbyCollectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leagues")
@Tag(name = "Ligas", description = "Endpoints para crear y consultar ligas")
public class LeagueController {

  private static final Logger LOGGER = LoggerFactory.getLogger(LeagueController.class);

  private final CreateLeagueUseCase createLeague;
  private final LeaveLeagueUseCase leaveLeague;
  private final StartLeagueUseCase startLeague;
  private final GetLeagueStateUseCase getLeagueState;
  private final GetPublicLeaguesUseCase getPublicLeagues;

  public LeagueController(final CreateLeagueUseCase createLeague,
      final LeaveLeagueUseCase leaveLeague, final StartLeagueUseCase startLeague,
      final GetLeagueStateUseCase getLeagueState, final GetPublicLeaguesUseCase getPublicLeagues) {

    this.createLeague = Objects.requireNonNull(createLeague);
    this.leaveLeague = Objects.requireNonNull(leaveLeague);
    this.startLeague = Objects.requireNonNull(startLeague);
    this.getLeagueState = Objects.requireNonNull(getLeagueState);
    this.getPublicLeagues = Objects.requireNonNull(getPublicLeagues);
  }

  @PostMapping
  @Operation(summary = "Crear liga", description = "Crea un liga round-robin. El creador se une automáticamente.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Liga creado", content = @Content(schema = @Schema(implementation = CreateLeagueResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Datos inválidos para crear el liga", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateLeagueResponse> createLeague(
      @Valid @RequestBody final CreateLeagueRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP createLeague requested: numberOfPlayers={}, gamesToPlay={}",
        request.numberOfPlayers(), request.gamesToPlay());

    final var dto = this.createLeague.handle(
        new CreateLeagueCommand(jwt.getSubject(), request.numberOfPlayers(), request.gamesToPlay(),
            request.visibility()));

    return ResponseEntity.ok(CreateLeagueResponse.from(dto));
  }

  @GetMapping("/public")
  @Operation(summary = "Listar ligas publicas", description = "Devuelve ligas publicas esperando jugadores para el usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Listado de ligas publicas", content = @Content(schema = @Schema(implementation = PublicLeagueLobbyCollectionResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "400", description = "Cursor o limit invalidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "El usuario no puede usar el lobby publico", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<PublicLeagueLobbyCollectionResponse> getPublicLeagues(
      @Parameter(description = "Cantidad maxima de items por pagina", example = "20") @RequestParam(defaultValue = "20") final int limit,
      @Parameter(description = "Cursor opaco para pedir la siguiente pagina") @RequestParam(required = false) final String after,
      @AuthenticationPrincipal final Jwt jwt) {

    final var leagues = this.getPublicLeagues.handle(
        new GetPublicLeaguesQuery(jwt.getSubject(), limit, after));
    return ResponseEntity.ok(PublicLeagueLobbyCollectionResponse.from(leagues, limit, after));
  }

  @PostMapping("/{leagueId}/leave")
  @Operation(summary = "Salir de liga", description = "Remueve al jugador del liga (solo antes de que arranque)", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Jugador removido"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede salir en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> leaveLeague(
      @Parameter(description = "ID del liga", example = "league-123") @PathVariable final String leagueId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP leaveLeague requested: leagueId={}, playerId={}", leagueId, jwt.getSubject());

    this.leaveLeague.handle(new LeaveLeagueCommand(leagueId, jwt.getSubject()));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{leagueId}/start")
  @Operation(summary = "Iniciar liga", description = "El creador inicia el liga una vez que todos los jugadores se unieron", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Liga iniciado"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede iniciar en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> startLeague(
      @Parameter(description = "ID del liga", example = "league-123") @PathVariable final String leagueId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP startLeague requested: leagueId={}, playerId={}", leagueId, jwt.getSubject());

    this.startLeague.handle(new StartLeagueCommand(leagueId, jwt.getSubject()));

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{leagueId}")
  @Operation(summary = "Obtener estado de liga", description = "Devuelve estado completo, tabla y fixtures del liga. Solo participantes del liga pueden consultarlo.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado del liga", content = @Content(schema = @Schema(implementation = LeagueStateResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Liga no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Jugador no pertenece al liga", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<LeagueStateResponse> getLeagueState(
      @Parameter(description = "ID del liga", example = "league-123") @PathVariable final String leagueId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.debug("HTTP getLeagueState requested: leagueId={}", leagueId);

    final var dto = this.getLeagueState.handle(new GetLeagueStateQuery(leagueId, jwt.getSubject()));

    return ResponseEntity.ok(LeagueStateResponse.from(dto));
  }

}
