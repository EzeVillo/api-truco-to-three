package com.villo.truco.application.assemblers;

import com.villo.truco.application.dto.CardDTO;
import com.villo.truco.application.dto.CurrentHandDTO;
import com.villo.truco.application.dto.PlayedHandDTO;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.dto.SpectatorRoundStateDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SpectatorMatchStateDTOAssembler {

  private final PublicActorResolver publicActorResolver;
  private final BotVsBotMatchRegistry botVsBotMatchRegistry;
  private final long idleTimeoutMillis;

  public SpectatorMatchStateDTOAssembler(final PublicActorResolver publicActorResolver,
      final BotVsBotMatchRegistry botVsBotMatchRegistry, final long idleTimeoutMillis) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
    this.idleTimeoutMillis = idleTimeoutMillis;
  }

  private static List<CardDTO> cardsOf(final Match match, final PlayerId playerId) {

    return match.getCardsOf(playerId).stream().map(CardDTO::from).toList();
  }

  public SpectatorMatchStateDTO toDto(final Match match, final int spectatorCount) {

    final var actorNames = this.resolveActorNames(match);

    final var roundState = match.getStatus() == MatchStatus.IN_PROGRESS && !match.isFinished()
        ? this.toSpectatorRoundState(match, actorNames) : null;

    final var matchWinner = Optional.ofNullable(match.getMatchWinner()).map(actorNames::get)
        .orElse(null);

    final var playerOneUsername = actorNames.get(match.getPlayerOne());
    final var playerTwoUsername =
        match.getPlayerTwo() != null ? actorNames.get(match.getPlayerTwo()) : null;

    return new SpectatorMatchStateDTO(match.getId().value().toString(), match.getStatus().name(),
        playerOneUsername, playerTwoUsername, match.getScorePlayerOne(), match.getScorePlayerTwo(),
        match.getGamesWonPlayerOne(), match.getGamesWonPlayerTwo(), match.getGamesToPlay(),
        matchWinner, roundState, spectatorCount, match.getStateVersion());
  }

  private Map<PlayerId, String> resolveActorNames(final Match match) {

    final var playerIds = new LinkedHashSet<PlayerId>();
    playerIds.add(match.getPlayerOne());
    if (match.getPlayerTwo() != null) {
      playerIds.add(match.getPlayerTwo());
    }
    return this.publicActorResolver.resolveAll(playerIds);
  }

  private SpectatorRoundStateDTO toSpectatorRoundState(final Match match,
      final Map<PlayerId, String> actorNames) {

    final var currentTurn = Optional.ofNullable(match.getCurrentTurn()).map(actorNames::get)
        .orElse(null);

    final var roundStatus = Optional.ofNullable(match.getRoundStatus()).map(Enum::name)
        .orElse(null);

    final var currentTrucoCall = Optional.ofNullable(match.getCurrentTrucoCall()).map(Enum::name)
        .orElse(null);

    final var currentTrucoCaller = Optional.ofNullable(match.getTrucoCaller()).map(actorNames::get)
        .orElse(null);

    final var currentEnvidoCall = Optional.ofNullable(match.getCurrentEnvidoCall()).map(Enum::name)
        .orElse(null);

    final var currentEnvidoCaller = Optional.ofNullable(match.getEnvidoCaller())
        .map(actorNames::get).orElse(null);

    final var matchWinner = Optional.ofNullable(match.getMatchWinner()).map(actorNames::get)
        .orElse(null);

    final var playedHands = match.getPlayedHands().stream().map(ph -> new PlayedHandDTO(
        ph.cardPlayerOne() != null ? CardDTO.from(ph.cardPlayerOne()) : null,
        ph.cardPlayerTwo() != null ? CardDTO.from(ph.cardPlayerTwo()) : null,
        ph.winner() != null ? actorNames.get(ph.winner()) : null)).toList();

    final var handInfo = match.getCurrentHandInfo();

    final var currentHand = new CurrentHandDTO(
        handInfo.cardPlayerOne() != null ? CardDTO.from(handInfo.cardPlayerOne()) : null,
        handInfo.cardPlayerTwo() != null ? CardDTO.from(handInfo.cardPlayerTwo()) : null,
        handInfo.mano() != null ? actorNames.get(handInfo.mano()) : null);

    final var deadline = ActionDeadlineProjection.of(match, this.idleTimeoutMillis);

    final var botVsBot = this.botVsBotMatchRegistry.isBotVsBotMatch(match.getId());
    final var handPlayerOne = botVsBot ? cardsOf(match, match.getPlayerOne()) : null;
    final var handPlayerTwo = botVsBot ? cardsOf(match, match.getPlayerTwo()) : null;

    return new SpectatorRoundStateDTO(match.getStatus().name(), currentTurn, roundStatus,
        currentTrucoCall, currentTrucoCaller, currentEnvidoCall, currentEnvidoCaller, matchWinner,
        playedHands, currentHand, deadline.actionDeadline(), deadline.turnDurationMillis(),
        deadline.actionDeadlineSeat(), handPlayerOne, handPlayerTwo);
  }

}
