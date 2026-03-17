package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchRehydrator;
import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.HandId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.RoundId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.MatchJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.RoundData;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

  public MatchJpaEntity toEntity(final Match match) {

    final var snapshot = MatchSnapshotExtractor.extract(match);
    final var entity = new MatchJpaEntity();

    entity.setId(snapshot.id().value());
    entity.setPlayerOne(snapshot.playerOne().value());
    entity.setPlayerTwo(snapshot.playerTwo() != null ? snapshot.playerTwo().value() : null);
    entity.setInviteCode(snapshot.inviteCode() != null ? snapshot.inviteCode().value() : null);
    entity.setStatus(snapshot.status().name());
    entity.setGamesToWin(snapshot.rules().gamesToWin());
    entity.setGamesWonPlayerOne(snapshot.gamesWonPlayerOne());
    entity.setGamesWonPlayerTwo(snapshot.gamesWonPlayerTwo());
    entity.setGameNumber(snapshot.gameNumber());
    entity.setScorePlayerOne(snapshot.scorePlayerOne());
    entity.setScorePlayerTwo(snapshot.scorePlayerTwo());
    entity.setRoundNumber(snapshot.roundNumber());
    entity.setReadyPlayerOne(snapshot.readyPlayerOne());
    entity.setReadyPlayerTwo(snapshot.readyPlayerTwo());
    entity.setFirstManoOfGame(
        snapshot.firstManoOfGame() != null ? snapshot.firstManoOfGame().value() : null);
    entity.setCurrentRound(
        snapshot.currentRound() != null ? toRoundData(snapshot.currentRound()) : null);
    entity.setVersion((int) match.getVersion());

    return entity;
  }

  public Match toDomain(final MatchJpaEntity entity) {

    final MatchSnapshot.RoundData roundSnapshot =
        entity.getCurrentRound() != null ? toRoundSnapshot(entity.getCurrentRound()) : null;

    final var snapshot = new MatchSnapshot.MatchData(new MatchId(entity.getId()),
        new PlayerId(entity.getPlayerOne()),
        entity.getPlayerTwo() != null ? new PlayerId(entity.getPlayerTwo()) : null,
        entity.getInviteCode() != null ? InviteCode.of(entity.getInviteCode()) : null,
        new MatchRules(entity.getGamesToWin()), MatchStatus.valueOf(entity.getStatus()),
        entity.getGamesWonPlayerOne(), entity.getGamesWonPlayerTwo(), entity.getGameNumber(),
        entity.getScorePlayerOne(), entity.getScorePlayerTwo(), entity.getRoundNumber(),
        entity.isReadyPlayerOne(), entity.isReadyPlayerTwo(),
        entity.getFirstManoOfGame() != null ? new PlayerId(entity.getFirstManoOfGame()) : null,
        roundSnapshot);

    final var match = MatchRehydrator.rehydrate(snapshot);
    match.setVersion(entity.getVersion());
    return match;
  }

  private RoundData toRoundData(final MatchSnapshot.RoundData round) {

    return new RoundData(round.id().value(), round.roundNumber(), round.mano().value(),
        round.playerOne().value(), round.playerTwo().value(), toHandData(round.handPlayerOne()),
        toHandData(round.handPlayerTwo()),
        round.playedHands().stream().map(this::toPlayedHandData).toList(),
        round.currentHandCards().stream().map(this::toCardPlayData).toList(),
        toTrucoData(round.trucoStateMachine()), toEnvidoData(round.envidoStateMachine()),
        round.status().name(), round.currentTurn().value(),
        round.turnBeforeTrucoCall() != null ? round.turnBeforeTrucoCall().value() : null,
        round.turnBeforeEnvidoCall() != null ? round.turnBeforeEnvidoCall().value() : null);
  }

  private RoundData.HandData toHandData(final MatchSnapshot.HandData hand) {

    return new RoundData.HandData(hand.id().value(),
        hand.cards().stream().map(this::toCardData).toList());
  }

  private RoundData.CardData toCardData(final Card card) {

    return new RoundData.CardData(card.suit().name(), card.number());
  }

  private RoundData.PlayedHandData toPlayedHandData(final MatchSnapshot.PlayedHandData ph) {

    return new RoundData.PlayedHandData(toCardData(ph.cardMano()), toCardData(ph.cardPie()),
        ph.winner() != null ? ph.winner().value() : null);
  }

  private RoundData.CardPlayData toCardPlayData(final MatchSnapshot.CardPlayData cp) {

    return new RoundData.CardPlayData(cp.playerId().value(), toCardData(cp.card()));
  }

  private RoundData.TrucoData toTrucoData(final MatchSnapshot.TrucoData truco) {

    return new RoundData.TrucoData(truco.currentCall() != null ? truco.currentCall().name() : null,
        truco.caller() != null ? truco.caller().value() : null, truco.pointsAtStake());
  }

  private RoundData.EnvidoData toEnvidoData(final MatchSnapshot.EnvidoData envido) {

    return new RoundData.EnvidoData(envido.chain().stream().map(EnvidoCall::name).toList(),
        envido.resolved());
  }

  private MatchSnapshot.RoundData toRoundSnapshot(final RoundData data) {

    return new MatchSnapshot.RoundData(new RoundId(data.id()), data.roundNumber(),
        new PlayerId(data.mano()), new PlayerId(data.playerOne()), new PlayerId(data.playerTwo()),
        toHandSnapshot(data.handPlayerOne()), toHandSnapshot(data.handPlayerTwo()),
        data.playedHands().stream().map(this::toPlayedHandSnapshot).toList(),
        data.currentHandCards().stream().map(this::toCardPlaySnapshot).toList(),
        toTrucoSnapshot(data.trucoStateMachine()), toEnvidoSnapshot(data.envidoStateMachine()),
        RoundStatus.valueOf(data.status()), new PlayerId(data.currentTurn()),
        data.turnBeforeTrucoCall() != null ? new PlayerId(data.turnBeforeTrucoCall()) : null,
        data.turnBeforeEnvidoCall() != null ? new PlayerId(data.turnBeforeEnvidoCall()) : null);
  }

  private MatchSnapshot.HandData toHandSnapshot(final RoundData.HandData data) {

    return new MatchSnapshot.HandData(new HandId(data.id()),
        data.cards().stream().map(this::toCard).toList());
  }

  private Card toCard(final RoundData.CardData data) {

    return Card.of(Suit.valueOf(data.suit()), data.number());
  }

  private MatchSnapshot.PlayedHandData toPlayedHandSnapshot(final RoundData.PlayedHandData data) {

    return new MatchSnapshot.PlayedHandData(toCard(data.cardMano()), toCard(data.cardPie()),
        data.winner() != null ? new PlayerId(data.winner()) : null);
  }

  private MatchSnapshot.CardPlayData toCardPlaySnapshot(final RoundData.CardPlayData data) {

    return new MatchSnapshot.CardPlayData(new PlayerId(data.playerId()), toCard(data.card()));
  }

  private MatchSnapshot.TrucoData toTrucoSnapshot(final RoundData.TrucoData data) {

    return new MatchSnapshot.TrucoData(
        data.currentCall() != null ? TrucoCall.valueOf(data.currentCall()) : null,
        data.caller() != null ? new PlayerId(data.caller()) : null, data.pointsAtStake());
  }

  private MatchSnapshot.EnvidoData toEnvidoSnapshot(final RoundData.EnvidoData data) {

    return new MatchSnapshot.EnvidoData(data.chain().stream().map(EnvidoCall::valueOf).toList(),
        data.resolved());
  }

}
