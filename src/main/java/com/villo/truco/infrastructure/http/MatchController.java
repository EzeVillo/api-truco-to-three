package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.application.exceptions.UnauthorizedAccessException;
import com.villo.truco.application.ports.PlayerIdentity;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.infrastructure.http.dto.request.CallEnvidoRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.PlayCardRequest;
import com.villo.truco.infrastructure.http.dto.request.RespondEnvidoRequest;
import com.villo.truco.infrastructure.http.dto.request.RespondTrucoRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.JoinMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.MatchStateResponse;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public final class MatchController {

  private final CreateMatchUseCase createMatch;
  private final JoinMatchUseCase joinMatch;
  private final StartMatchUseCase startMatch;
  private final PlayCardUseCase playCard;
  private final CallTrucoUseCase callTruco;
  private final RespondTrucoUseCase respondTruco;
  private final CallEnvidoUseCase callEnvido;
  private final RespondEnvidoUseCase respondEnvido;
  private final FoldUseCase fold;
  private final GetMatchStateUseCase getMatchState;
  private final PlayerTokenProvider tokenProvider;

  public MatchController(final CreateMatchUseCase createMatch, final JoinMatchUseCase joinMatch,
      final StartMatchUseCase startMatch, final PlayCardUseCase playCard,
      final CallTrucoUseCase callTruco, final RespondTrucoUseCase respondTruco,
      final CallEnvidoUseCase callEnvido, final RespondEnvidoUseCase respondEnvido,
      final FoldUseCase fold, final GetMatchStateUseCase getMatchState,
      final PlayerTokenProvider tokenProvider) {

    this.createMatch = Objects.requireNonNull(createMatch);
    this.joinMatch = Objects.requireNonNull(joinMatch);
    this.startMatch = Objects.requireNonNull(startMatch);
    this.playCard = Objects.requireNonNull(playCard);
    this.callTruco = Objects.requireNonNull(callTruco);
    this.respondTruco = Objects.requireNonNull(respondTruco);
    this.callEnvido = Objects.requireNonNull(callEnvido);
    this.respondEnvido = Objects.requireNonNull(respondEnvido);
    this.fold = Objects.requireNonNull(fold);
    this.getMatchState = Objects.requireNonNull(getMatchState);
    this.tokenProvider = Objects.requireNonNull(tokenProvider);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<CreateMatchResponse> createMatch() {

    final var dto = this.createMatch.handle(new CreateMatchCommand());
    return ResponseEntity.ok(CreateMatchResponse.from(dto));
  }

  @PostMapping("/{matchId}/join")
  @Transactional
  public ResponseEntity<JoinMatchResponse> joinMatch(@PathVariable final String matchId,
      @RequestBody final JoinMatchRequest request) {

    final var dto = this.joinMatch.handle(new JoinMatchCommand(matchId, request.inviteCode()));
    return ResponseEntity.ok(JoinMatchResponse.from(dto));
  }

  @PostMapping("/{matchId}/start")
  @Transactional
  public ResponseEntity<Void> startMatch(@PathVariable final String matchId,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.startMatch.handle(new StartMatchCommand(matchId, identity.playerId().value().toString()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{matchId}")
  public ResponseEntity<MatchStateResponse> getMatchState(@PathVariable final String matchId,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    final var state = this.getMatchState.handle(
        new GetMatchStateQuery(matchId, identity.playerId().value().toString()));
    return ResponseEntity.ok(MatchStateResponse.from(state));
  }

  @PostMapping("/{matchId}/play-card")
  @Transactional
  public ResponseEntity<Void> playCard(@PathVariable final String matchId,
      @RequestBody final PlayCardRequest request,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.playCard.handle(
        new PlayCardCommand(matchId, identity.playerId().value().toString(), request.suit(),
            request.number()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/truco")
  @Transactional
  public ResponseEntity<Void> callTruco(@PathVariable final String matchId,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.callTruco.handle(new CallTrucoCommand(matchId, identity.playerId().value().toString()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/truco/respond")
  @Transactional
  public ResponseEntity<Void> respondTruco(@PathVariable final String matchId,
      @RequestBody final RespondTrucoRequest request,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.respondTruco.handle(
        new RespondTrucoCommand(matchId, identity.playerId().value().toString(),
            request.response()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/envido")
  @Transactional
  public ResponseEntity<Void> callEnvido(@PathVariable final String matchId,
      @RequestBody final CallEnvidoRequest request,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.callEnvido.handle(
        new CallEnvidoCommand(matchId, identity.playerId().value().toString(), request.call()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/envido/respond")
  @Transactional
  public ResponseEntity<Void> respondEnvido(@PathVariable final String matchId,
      @RequestBody final RespondEnvidoRequest request,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.respondEnvido.handle(
        new RespondEnvidoCommand(matchId, identity.playerId().value().toString(),
            request.response()));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/fold")
  @Transactional
  public ResponseEntity<Void> fold(@PathVariable final String matchId,
      @RequestHeader("Authorization") final String authHeader) {

    final var identity = this.authenticate(matchId, authHeader);
    this.fold.handle(new FoldCommand(matchId, identity.playerId().value().toString()));
    return ResponseEntity.noContent().build();
  }

  private PlayerIdentity authenticate(final String matchId, final String authHeader) {

    final var token = this.extractBearerToken(authHeader);
    final var identity = this.tokenProvider.validateAccessToken(token);

    if (!identity.matchId().value().toString().equals(matchId)) {
      throw new UnauthorizedAccessException("Token does not belong to this match");
    }

    return identity;
  }

  private String extractBearerToken(final String authHeader) {

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new UnauthorizedAccessException("Missing or invalid Authorization header");
    }
    return authHeader.substring(7);
  }

}
