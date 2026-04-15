package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CreateCupCommand;
import com.villo.truco.application.commands.LeaveCupCommand;
import com.villo.truco.application.commands.StartCupCommand;
import com.villo.truco.application.ports.in.CreateCupUseCase;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.ports.in.GetPublicCupsUseCase;
import com.villo.truco.application.ports.in.LeaveCupUseCase;
import com.villo.truco.application.ports.in.StartCupUseCase;
import com.villo.truco.application.queries.GetCupStateQuery;
import com.villo.truco.application.queries.GetPublicCupsQuery;
import com.villo.truco.infrastructure.http.dto.request.CreateCupRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateCupResponse;
import com.villo.truco.infrastructure.http.dto.response.CupStateResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.PublicCupLobbyCollectionResponse;
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
@RequestMapping("/api/cups")
@Tag(name = "Copas", description = "Endpoints para crear y consultar copas de eliminación directa")
public class CupController {

  private static final Logger LOGGER = LoggerFactory.getLogger(CupController.class);

  private final CreateCupUseCase createCup;
  private final LeaveCupUseCase leaveCup;
  private final StartCupUseCase startCup;
  private final GetCupStateUseCase getCupState;
  private final GetPublicCupsUseCase getPublicCups;

  public CupController(final CreateCupUseCase createCup, final LeaveCupUseCase leaveCup,
      final StartCupUseCase startCup, final GetCupStateUseCase getCupState,
      final GetPublicCupsUseCase getPublicCups) {

    this.createCup = Objects.requireNonNull(createCup);
    this.leaveCup = Objects.requireNonNull(leaveCup);
    this.startCup = Objects.requireNonNull(startCup);
    this.getCupState = Objects.requireNonNull(getCupState);
    this.getPublicCups = Objects.requireNonNull(getPublicCups);
  }

  @PostMapping
  @Operation(summary = "Crear copa", description = "Crea una copa de eliminación directa. El creador se une automáticamente.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Copa creada", content = @Content(schema = @Schema(implementation = CreateCupResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Datos inválidos para crear la copa", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateCupResponse> createCup(
      @Valid @RequestBody final CreateCupRequest request, @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP createCup requested: numberOfPlayers={}, gamesToPlay={}",
        request.numberOfPlayers(), request.gamesToPlay());

    final var dto = this.createCup.handle(
        new CreateCupCommand(jwt.getSubject(), request.numberOfPlayers(), request.gamesToPlay(),
            request.visibility()));

    return ResponseEntity.ok(CreateCupResponse.from(dto));
  }

  @GetMapping("/public")
  @Operation(summary = "Listar copas publicas", description = "Devuelve copas publicas esperando jugadores para el usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Listado de copas publicas", content = @Content(schema = @Schema(implementation = PublicCupLobbyCollectionResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "400", description = "Cursor o limit invalidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "El usuario no puede usar el lobby publico", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<PublicCupLobbyCollectionResponse> getPublicCups(
      @Parameter(description = "Cantidad maxima de items por pagina", example = "20") @RequestParam(defaultValue = "20") final int limit,
      @Parameter(description = "Cursor opaco para pedir la siguiente pagina") @RequestParam(required = false) final String after,
      @AuthenticationPrincipal final Jwt jwt) {

    final var cups = this.getPublicCups.handle(
        new GetPublicCupsQuery(jwt.getSubject(), limit, after));
    return ResponseEntity.ok(PublicCupLobbyCollectionResponse.from(cups, limit, after));
  }

  @PostMapping("/{cupId}/leave")
  @Operation(summary = "Salir de copa", description = "Remueve al jugador de la copa (solo antes de que arranque)", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Jugador removido"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede salir en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> leaveCup(
      @Parameter(description = "ID de la copa", example = "cup-123") @PathVariable final String cupId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP leaveCup requested: cupId={}, playerId={}", cupId, jwt.getSubject());

    this.leaveCup.handle(new LeaveCupCommand(cupId, jwt.getSubject()));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{cupId}/start")
  @Operation(summary = "Iniciar copa", description = "El creador inicia la copa una vez que todos los jugadores se unieron", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Copa iniciada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede iniciar en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> startCup(
      @Parameter(description = "ID de la copa", example = "cup-123") @PathVariable final String cupId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP startCup requested: cupId={}, playerId={}", cupId, jwt.getSubject());

    this.startCup.handle(new StartCupCommand(cupId, jwt.getSubject()));

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{cupId}")
  @Operation(summary = "Obtener estado de copa", description = "Devuelve estado completo y bracket de la copa. Solo participantes pueden consultarla.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado de la copa", content = @Content(schema = @Schema(implementation = CupStateResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Copa no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Jugador no pertenece a la copa", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CupStateResponse> getCupState(
      @Parameter(description = "ID de la copa", example = "cup-123") @PathVariable final String cupId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.debug("HTTP getCupState requested: cupId={}", cupId);

    final var dto = this.getCupState.handle(new GetCupStateQuery(cupId, jwt.getSubject()));

    return ResponseEntity.ok(CupStateResponse.from(dto));
  }

}
