package com.villo.truco.profile.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.MatchAchievementTracker;
import com.villo.truco.profile.domain.model.MatchAchievementTrackerRehydrator;
import com.villo.truco.profile.domain.model.MatchAchievementTrackerSnapshot;
import com.villo.truco.profile.infrastructure.persistence.entities.MatchAchievementTrackerJpaEntity;
import com.villo.truco.profile.infrastructure.persistence.entities.MatchAchievementTrackerStateData;
import org.springframework.stereotype.Component;

@Component
public class MatchAchievementTrackerMapper {

  public MatchAchievementTrackerJpaEntity toEntity(final MatchAchievementTracker tracker) {

    final var snapshot = tracker.snapshot();
    final var entity = new MatchAchievementTrackerJpaEntity();
    entity.setMatchId(snapshot.matchId().value());
    entity.setPlayerOneId(snapshot.playerOne().value());
    entity.setPlayerTwoId(snapshot.playerTwo().value());
    entity.setHumanVsHuman(snapshot.humanVsHuman());
    entity.setState(this.toStateData(snapshot));
    entity.setVersion((int) tracker.getVersion());
    return entity;
  }

  public MatchAchievementTracker toDomain(final MatchAchievementTrackerJpaEntity entity) {

    final var state = entity.getState();
    final var snapshot = new MatchAchievementTrackerSnapshot(new MatchId(entity.getMatchId()),
        new PlayerId(entity.getPlayerOneId()), new PlayerId(entity.getPlayerTwoId()),
        entity.isHumanVsHuman(), state.currentGameNumber(), state.currentRoundNumber(),
        parsePlayerSeat(state.manoSeat()), state.previousScorePlayerOne(),
        state.previousScorePlayerTwo(), state.scorePlayerOne(), state.scorePlayerTwo(),
        state.playedHandsInRound(), state.roundHadCalls(),
        state.envidoCallsInRound().stream().map(EnvidoCall::valueOf).toList(),
        parseEnvidoResponse(state.lastEnvidoResponse()), parsePlayerSeat(state.lastEnvidoWinnerSeat()),
        state.lastEnvidoPointsMano(), state.lastEnvidoPointsPie(),
        parseTrucoResponse(state.lastTrucoResponse()), parseTrucoCall(state.lastTrucoResponseCall()),
        parsePlayerSeat(state.lastTrucoResponderSeat()), toCard(state.lastHandCardPlayerOne()),
        toCard(state.lastHandCardPlayerTwo()), parsePlayerSeat(state.lastHandWinnerSeat()),
        parsePlayerSeat(state.lastFoldedSeat()), parsePlayerSeat(state.lastRoundWinnerSeat()),
        state.pendingAcceptedValeCuatro(), state.playerOneLostAcceptedValeCuatroByBust(),
        state.playerTwoLostAcceptedValeCuatroByBust());

    final var tracker = MatchAchievementTrackerRehydrator.rehydrate(snapshot);
    tracker.setVersion(entity.getVersion());
    return tracker;
  }

  private MatchAchievementTrackerStateData toStateData(final MatchAchievementTrackerSnapshot snapshot) {

    return new MatchAchievementTrackerStateData(snapshot.currentGameNumber(),
        snapshot.currentRoundNumber(),
        snapshot.manoSeat() != null ? snapshot.manoSeat().name() : null,
        snapshot.previousScorePlayerOne(), snapshot.previousScorePlayerTwo(),
        snapshot.scorePlayerOne(), snapshot.scorePlayerTwo(), snapshot.playedHandsInRound(),
        snapshot.roundHadCalls(), snapshot.envidoCallsInRound().stream().map(EnvidoCall::name).toList(),
        snapshot.lastEnvidoResponse() != null ? snapshot.lastEnvidoResponse().name() : null,
        snapshot.lastEnvidoWinnerSeat() != null ? snapshot.lastEnvidoWinnerSeat().name() : null,
        snapshot.lastEnvidoPointsMano(), snapshot.lastEnvidoPointsPie(),
        snapshot.lastTrucoResponse() != null ? snapshot.lastTrucoResponse().name() : null,
        snapshot.lastTrucoResponseCall() != null ? snapshot.lastTrucoResponseCall().name() : null,
        snapshot.lastTrucoResponderSeat() != null ? snapshot.lastTrucoResponderSeat().name() : null,
        toCardData(snapshot.lastHandCardPlayerOne()), toCardData(snapshot.lastHandCardPlayerTwo()),
        snapshot.lastHandWinnerSeat() != null ? snapshot.lastHandWinnerSeat().name() : null,
        snapshot.lastFoldedSeat() != null ? snapshot.lastFoldedSeat().name() : null,
        snapshot.lastRoundWinnerSeat() != null ? snapshot.lastRoundWinnerSeat().name() : null,
        snapshot.pendingAcceptedValeCuatro(), snapshot.playerOneLostAcceptedValeCuatroByBust(),
        snapshot.playerTwoLostAcceptedValeCuatroByBust());
  }

  private MatchAchievementTrackerStateData.CardData toCardData(final Card card) {

    if (card == null) {
      return null;
    }
    return new MatchAchievementTrackerStateData.CardData(card.suit().name(), card.number());
  }

  private Card toCard(final MatchAchievementTrackerStateData.CardData cardData) {

    if (cardData == null || cardData.suit() == null || cardData.number() == null) {
      return null;
    }
    return Card.of(Suit.valueOf(cardData.suit()), cardData.number());
  }

  private PlayerSeat parsePlayerSeat(final String value) {

    return value == null ? null : PlayerSeat.valueOf(value);
  }

  private EnvidoResponse parseEnvidoResponse(final String value) {

    return value == null ? null : EnvidoResponse.valueOf(value);
  }

  private TrucoResponse parseTrucoResponse(final String value) {

    return value == null ? null : TrucoResponse.valueOf(value);
  }

  private TrucoCall parseTrucoCall(final String value) {

    return value == null ? null : TrucoCall.valueOf(value);
  }
}
