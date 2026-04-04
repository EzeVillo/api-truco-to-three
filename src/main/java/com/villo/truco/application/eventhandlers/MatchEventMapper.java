package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.match.events.AvailableActionsUpdatedEvent;
import com.villo.truco.domain.model.match.events.CardPlayedEvent;
import com.villo.truco.domain.model.match.events.EnvidoCalledEvent;
import com.villo.truco.domain.model.match.events.EnvidoResolvedEvent;
import com.villo.truco.domain.model.match.events.FoldedEvent;
import com.villo.truco.domain.model.match.events.GameScoreChangedEvent;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.HandResolvedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.MatchPlayerLeftEvent;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.events.RoundEndedEvent;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.ScoreChangedEvent;
import com.villo.truco.domain.model.match.events.TrucoCalledEvent;
import com.villo.truco.domain.model.match.events.TrucoCancelledByEnvidoEvent;
import com.villo.truco.domain.model.match.events.TrucoRespondedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MatchEventMapper {

  private static Map<String, Object> mapCardPlayed(final CardPlayedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("seat", event.getSeat().name());
    map.put("card", mapCard(event.getCard()));
    return map;
  }

  private static Map<String, Object> mapHandResolved(final HandResolvedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("cardPlayerOne", mapCard(event.getCardPlayerOne()));
    map.put("cardPlayerTwo", mapCard(event.getCardPlayerTwo()));
    map.put("winnerSeat", event.getWinnerSeat() != null ? event.getWinnerSeat().name() : null);
    return map;
  }

  private static Map<String, Object> mapTurnChanged(final TurnChangedEvent event) {

    return Map.of("seat", event.getSeat().name());
  }

  private static Map<String, Object> mapTrucoCalled(final TrucoCalledEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("callerSeat", event.getCallerSeat().name());
    map.put("call", event.getCall().name());
    return map;
  }

  private static Map<String, Object> mapTrucoResponded(final TrucoRespondedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("responderSeat", event.getResponderSeat().name());
    map.put("response", event.getResponse().name());
    map.put("call", event.getCall().name());
    return map;
  }

  private static Map<String, Object> mapEnvidoCalled(final EnvidoCalledEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("callerSeat", event.getCallerSeat().name());
    map.put("call", event.getCall().name());
    return map;
  }

  private static Map<String, Object> mapEnvidoResolved(final EnvidoResolvedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("response", event.getResponse().name());
    map.put("winnerSeat", event.getWinnerSeat().name());
    if (event.getPointsMano() != null) {
      map.put("pointsMano", event.getPointsMano());
    }
    if (event.getPointsPie() != null) {
      map.put("pointsPie", event.getPointsPie());
    }
    return map;
  }

  private static Map<String, Object> mapScoreChanged(final ScoreChangedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("scorePlayerOne", event.getScorePlayerOne());
    map.put("scorePlayerTwo", event.getScorePlayerTwo());
    return map;
  }

  private static Map<String, Object> mapRoundStarted(final RoundStartedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("roundNumber", event.getRoundNumber());
    map.put("manoSeat", event.getManoSeat().name());
    return map;
  }

  private static Map<String, Object> mapRoundEnded(final RoundEndedEvent event) {

    return Map.of("winnerSeat", event.getWinnerSeat().name());
  }

  private static Map<String, Object> mapGameStarted(final GameStartedEvent event) {

    return Map.of("gameNumber", event.getGameNumber());
  }

  private static Map<String, Object> mapGameScoreChanged(final GameScoreChangedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("gamesWonPlayerOne", event.getGamesWonPlayerOne());
    map.put("gamesWonPlayerTwo", event.getGamesWonPlayerTwo());
    return map;
  }

  private static Map<String, Object> mapMatchFinished(final MatchFinishedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("winnerSeat", event.getWinnerSeat().name());
    map.put("gamesWonPlayerOne", event.getGamesWonPlayerOne());
    map.put("gamesWonPlayerTwo", event.getGamesWonPlayerTwo());
    return map;
  }

  private static Map<String, Object> mapFolded(final FoldedEvent event) {

    return Map.of("seat", event.getSeat().name());
  }

  private static Map<String, Object> mapPlayerHandUpdated(final PlayerHandUpdatedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("seat", event.getSeat().name());
    map.put("cards", event.getCards().stream().map(MatchEventMapper::mapCard).toList());
    return map;
  }

  private static Map<String, Object> mapAvailableActionsUpdated(
      final AvailableActionsUpdatedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("seat", event.getSeat().name());
    map.put("availableActions",
        event.getAvailableActions().stream().map(MatchEventMapper::mapAvailableAction).toList());
    return map;
  }

  private static Map<String, Object> mapAvailableAction(final AvailableAction action) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("type", action.type().name());
    action.getParameter().ifPresent(p -> map.put("parameter", p));
    return map;
  }

  private static Map<String, Object> mapCard(final Card card) {

    if (card == null) {
      return null;
    }

    final var map = new LinkedHashMap<String, Object>();
    map.put("suit", card.suit().name());
    map.put("number", card.number());
    return map;
  }

  private static Map<String, Object> mapMatchAbandoned(final MatchAbandonedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("winnerSeat", event.getWinnerSeat().name());
    map.put("abandonerSeat", event.getAbandonerSeat().name());
    map.put("gamesWonPlayerOne", event.getGamesWonPlayerOne());
    map.put("gamesWonPlayerTwo", event.getGamesWonPlayerTwo());
    return map;
  }

  private static Map<String, Object> mapMatchForfeited(final MatchForfeitedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put("winnerSeat", event.getWinnerSeat().name());
    map.put("loserSeat",
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? PlayerSeat.PLAYER_TWO.name()
            : PlayerSeat.PLAYER_ONE.name());
    map.put("gamesWonPlayerOne", event.getGamesWonPlayerOne());
    map.put("gamesWonPlayerTwo", event.getGamesWonPlayerTwo());
    return map;
  }

  private static Map<String, Object> mapPlayerReady(final PlayerReadyEvent event) {

    return Map.of("seat", event.getSeat().name());
  }

  public Map<String, Object> map(final DomainEventBase event) {

    return switch (event) {
      case CardPlayedEvent e -> mapCardPlayed(e);
      case HandResolvedEvent e -> mapHandResolved(e);
      case TurnChangedEvent e -> mapTurnChanged(e);
      case TrucoCalledEvent e -> mapTrucoCalled(e);
      case TrucoCancelledByEnvidoEvent e -> Map.of();
      case TrucoRespondedEvent e -> mapTrucoResponded(e);
      case EnvidoCalledEvent e -> mapEnvidoCalled(e);
      case EnvidoResolvedEvent e -> mapEnvidoResolved(e);
      case ScoreChangedEvent e -> mapScoreChanged(e);
      case RoundStartedEvent e -> mapRoundStarted(e);
      case RoundEndedEvent e -> mapRoundEnded(e);
      case GameStartedEvent e -> mapGameStarted(e);
      case GameScoreChangedEvent e -> mapGameScoreChanged(e);
      case MatchAbandonedEvent e -> mapMatchAbandoned(e);
      case MatchCancelledEvent e -> Map.of();
      case MatchPlayerLeftEvent e -> Map.of("leaverSeat", e.getLeaverSeat().name());
      case MatchFinishedEvent e -> mapMatchFinished(e);
      case MatchForfeitedEvent e -> mapMatchForfeited(e);
      case FoldedEvent e -> mapFolded(e);
      case PlayerHandUpdatedEvent e -> mapPlayerHandUpdated(e);
      case AvailableActionsUpdatedEvent e -> mapAvailableActionsUpdated(e);
      case PlayerJoinedEvent e -> Map.of();
      case PlayerReadyEvent e -> mapPlayerReady(e);
      default -> Map.of();
    };
  }

}
