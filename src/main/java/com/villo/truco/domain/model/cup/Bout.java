package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

final class Bout extends EntityBase<BoutId> {

  private final int roundNumber;
  private final int bracketPosition;
  private PlayerId playerOne;
  private PlayerId playerTwo;
  private MatchId matchId;
  private PlayerId winner;
  private BoutStatus status;

  private Bout(final BoutId id, final int roundNumber, final int bracketPosition,
      final BoutStatus status) {

    super(id);
    this.roundNumber = roundNumber;
    this.bracketPosition = bracketPosition;
    this.status = status;
  }

  static Bout awaiting(final BoutId id, final int roundNumber, final int bracketPosition) {

    return new Bout(id, roundNumber, bracketPosition, BoutStatus.AWAITING);
  }

  static Bout reconstruct(final BoutId id, final int roundNumber, final int bracketPosition,
      final PlayerId playerOne, final PlayerId playerTwo, final MatchId matchId,
      final PlayerId winner, final BoutStatus status) {

    final var bout = new Bout(id, roundNumber, bracketPosition, status);
    bout.playerOne = playerOne;
    bout.playerTwo = playerTwo;
    bout.matchId = matchId;
    bout.winner = winner;
    return bout;
  }

  void assignPlayerOne(final PlayerId player) {

    this.playerOne = player;
  }

  void assignPlayerTwo(final PlayerId player) {

    this.playerTwo = player;
  }

  boolean hasBothPlayers() {

    return this.playerOne != null && this.playerTwo != null;
  }

  void transitionToPending() {

    this.status = BoutStatus.PENDING;
  }

  void resolveAsBye(final PlayerId player) {

    this.playerOne = player;
    this.winner = player;
    this.status = BoutStatus.BYE;
  }

  void resolve(final PlayerId winner) {

    this.winner = winner;
    this.status = BoutStatus.FINISHED;
  }

  void linkMatch(final MatchId matchId) {

    this.matchId = matchId;
  }

  boolean containsPlayer(final PlayerId playerId) {

    return playerId.equals(this.playerOne) || (playerId.equals(this.playerTwo));
  }

  BoutId id() {

    return this.id;
  }

  int roundNumber() {

    return this.roundNumber;
  }

  int bracketPosition() {

    return this.bracketPosition;
  }

  PlayerId playerOne() {

    return this.playerOne;
  }

  PlayerId playerTwo() {

    return this.playerTwo;
  }

  MatchId matchId() {

    return this.matchId;
  }

  PlayerId winner() {

    return this.winner;
  }

  BoutStatus status() {

    return this.status;
  }

  BoutView toView() {

    return new BoutView(this.id, this.roundNumber, this.bracketPosition, this.playerOne,
        this.playerTwo, this.matchId, this.winner, this.status);
  }

}
