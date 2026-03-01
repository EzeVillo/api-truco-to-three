package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.infrastructure.http.dto.request.CallEnvidoRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.PlayCardRequest;
import com.villo.truco.infrastructure.http.dto.request.PlayerActionRequest;
import com.villo.truco.infrastructure.http.dto.request.RespondEnvidoRequest;
import com.villo.truco.infrastructure.http.dto.request.RespondTrucoRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.EnvidoResultResponse;
import com.villo.truco.infrastructure.http.dto.response.MatchStateResponse;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public final class MatchController {

    private final CreateMatchUseCase createMatch;
    private final JoinMatchUseCase joinMatch;
    private final PlayCardUseCase playCard;
    private final CallTrucoUseCase callTruco;
    private final RespondTrucoUseCase respondTruco;
    private final CallEnvidoUseCase callEnvido;
    private final RespondEnvidoUseCase respondEnvido;
    private final FoldUseCase fold;
    private final GetMatchStateUseCase getMatchState;

    public MatchController(final CreateMatchUseCase createMatch,
        final JoinMatchUseCase joinMatch, final PlayCardUseCase playCard,
        final CallTrucoUseCase callTruco, final RespondTrucoUseCase respondTruco,
        final CallEnvidoUseCase callEnvido, final RespondEnvidoUseCase respondEnvido,
        final FoldUseCase fold, final GetMatchStateUseCase getMatchState) {

        this.createMatch = Objects.requireNonNull(createMatch);
        this.joinMatch = Objects.requireNonNull(joinMatch);
        this.playCard = Objects.requireNonNull(playCard);
        this.callTruco = Objects.requireNonNull(callTruco);
        this.respondTruco = Objects.requireNonNull(respondTruco);
        this.callEnvido = Objects.requireNonNull(callEnvido);
        this.respondEnvido = Objects.requireNonNull(respondEnvido);
        this.fold = Objects.requireNonNull(fold);
        this.getMatchState = Objects.requireNonNull(getMatchState);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CreateMatchResponse> createMatch() {

        final var createMatchDTO = this.createMatch.handle(new CreateMatchCommand());
        return ResponseEntity.ok(
            new CreateMatchResponse(createMatchDTO.matchId(), createMatchDTO.playerOneId(),
                createMatchDTO.playerTwoId()));
    }

    @PostMapping("/{matchId}/join")
    @Transactional
    public ResponseEntity<Void> joinMatch(@PathVariable final String matchId,
        @RequestBody final JoinMatchRequest request) {

        this.joinMatch.handle(new JoinMatchCommand(matchId, request.playerTwoId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchStateResponse> getMatchState(@PathVariable final String matchId,
        @RequestParam final String playerId) {

        final var state = this.getMatchState.handle(new GetMatchStateQuery(matchId, playerId));
        return ResponseEntity.ok(MatchStateResponse.from(state));
    }

    @PostMapping("/{matchId}/play-card")
    @Transactional
    public ResponseEntity<Void> playCard(@PathVariable final String matchId,
        @RequestBody final PlayCardRequest request) {

        this.playCard.handle(
            new PlayCardCommand(matchId, request.playerId(), request.suit(), request.number()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/truco")
    @Transactional
    public ResponseEntity<Void> callTruco(@PathVariable final String matchId,
        @RequestBody final PlayerActionRequest request) {

        this.callTruco.handle(new CallTrucoCommand(matchId, request.playerId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/truco/respond")
    @Transactional
    public ResponseEntity<Void> respondTruco(@PathVariable final String matchId,
        @RequestBody final RespondTrucoRequest request) {

        this.respondTruco.handle(
            new RespondTrucoCommand(matchId, request.playerId(), request.response()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/envido")
    @Transactional
    public ResponseEntity<Void> callEnvido(@PathVariable final String matchId,
        @RequestBody final CallEnvidoRequest request) {

        this.callEnvido.handle(new CallEnvidoCommand(matchId, request.playerId(), request.call()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/envido/respond")
    @Transactional
    public ResponseEntity<EnvidoResultResponse> respondEnvido(@PathVariable final String matchId,
        @RequestBody final RespondEnvidoRequest request) {

        final var result = this.respondEnvido.handle(
            new RespondEnvidoCommand(matchId, request.playerId(), request.response()));

        return result.map(EnvidoResultResponse::from).map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{matchId}/fold")
    @Transactional
    public ResponseEntity<Void> fold(@PathVariable final String matchId,
        @RequestBody final PlayerActionRequest request) {

        this.fold.handle(new FoldCommand(matchId, request.playerId()));
        return ResponseEntity.noContent().build();
    }

}
