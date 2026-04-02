package com.villo.truco.application.dto;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public record MatchStateDTO(String matchId, String status, int gamesWonPlayerOne,
                            int gamesWonPlayerTwo, String matchWinner, RoundStateDTO currentRound) {

  public static MatchStateDTO of(final Match match, final PlayerId requestingPlayer,
      final PublicActorResolver publicActorResolver) {

    final var roundStateDTO =
        match.getStatus() == MatchStatus.IN_PROGRESS && !match.isFinished() ? toRoundStateDTO(match,
            requestingPlayer, publicActorResolver) : null;

    final var matchWinner = Optional.ofNullable(match.getMatchWinner())
        .map(publicActorResolver::resolve).orElse(null);

    return new MatchStateDTO(match.getId().value().toString(), match.getStatus().name(),
        match.getGamesWonPlayerOne(), match.getGamesWonPlayerTwo(), matchWinner, roundStateDTO);
  }

  private static RoundStateDTO toRoundStateDTO(final Match match, final PlayerId requestingPlayer,
      final PublicActorResolver publicActorResolver) {

    final var myCards = match.getCardsOf(requestingPlayer).stream().map(CardDTO::from).toList();

    final var currentTurn = Optional.ofNullable(match.getCurrentTurn())
        .map(publicActorResolver::resolve).orElse(null);

    final var roundStatus = Optional.ofNullable(match.getRoundStatus()).map(Enum::name)
        .orElse(null);

    final var currentTrucoCall = Optional.ofNullable(match.getCurrentTrucoCall()).map(Enum::name)
        .orElse(null);

    final var matchWinner = Optional.ofNullable(match.getMatchWinner())
        .map(publicActorResolver::resolve).orElse(null);

    final var availableActions = AvailableActionDTO.fromActions(
        match.getAvailableActions(requestingPlayer));

    final var playedHands = match.getPlayedHands().stream().map(ph -> new PlayedHandDTO(
        ph.cardPlayerOne() != null ? CardDTO.from(ph.cardPlayerOne()) : null,
        ph.cardPlayerTwo() != null ? CardDTO.from(ph.cardPlayerTwo()) : null,
        ph.winner() != null ? publicActorResolver.resolve(ph.winner()) : null)).toList();

    final var handInfo = match.getCurrentHandInfo();

    final var currentHand = new CurrentHandDTO(
        handInfo.cardPlayerOne() != null ? CardDTO.from(handInfo.cardPlayerOne()) : null,
        handInfo.cardPlayerTwo() != null ? CardDTO.from(handInfo.cardPlayerTwo()) : null,
        handInfo.mano() != null ? publicActorResolver.resolve(handInfo.mano()) : null);

    return new RoundStateDTO(match.getStatus().name(), currentTurn, match.getScorePlayerOne(),
        match.getScorePlayerTwo(), myCards, roundStatus, currentTrucoCall, matchWinner,
        availableActions, playedHands, currentHand);
  }

}
