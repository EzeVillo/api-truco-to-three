package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

final class Fixture extends EntityBase<FixtureId> {

  private final int matchdayNumber;
  private final PlayerId playerOne;
  private final PlayerId playerTwo;
  private MatchId matchId;
  private PlayerId winner;
  private FixtureStatus status;

  private Fixture(final FixtureId id, final int matchdayNumber, final PlayerId playerOne,
      final PlayerId playerTwo, final FixtureStatus status) {

    super(id);
    this.matchdayNumber = matchdayNumber;
    this.playerOne = playerOne;
    this.playerTwo = playerTwo;
    this.status = status;
  }

  static Fixture scheduled(final FixtureId id, final int matchdayNumber, final PlayerId playerOne,
      final PlayerId playerTwo) {

    return new Fixture(id, matchdayNumber, playerOne, playerTwo, FixtureStatus.SCHEDULED);
  }

  static Fixture free(final FixtureId id, final int matchdayNumber, final PlayerId freePlayer) {

    return new Fixture(id, matchdayNumber, freePlayer, null, FixtureStatus.LIBRE);
  }

  static Fixture reconstruct(final FixtureId id, final int matchdayNumber, final PlayerId playerOne,
      final PlayerId playerTwo, final MatchId matchId, final PlayerId winner,
      final FixtureStatus status) {

    final var fixture = new Fixture(id, matchdayNumber, playerOne, playerTwo, status);
    fixture.matchId = matchId;
    fixture.winner = winner;
    return fixture;
  }

  FixtureId id() {

    return this.id;
  }

  MatchId matchId() {

    return this.matchId;
  }

  FixtureStatus status() {

    return this.status;
  }

  int matchdayNumber() {

    return this.matchdayNumber;
  }

  PlayerId playerOne() {

    return this.playerOne;
  }

  PlayerId playerTwo() {

    return this.playerTwo;
  }

  boolean containsPlayer(final PlayerId playerId) {

    final var playerTwoMatches = this.playerTwo != null && this.playerTwo.equals(playerId);
    return this.playerOne.equals(playerId) || playerTwoMatches;
  }

  void activate() {

    this.status = FixtureStatus.PENDING;
  }

  void linkMatch(final MatchId matchId) {

    this.matchId = matchId;
  }

  void resolve(final PlayerId winner) {

    this.winner = winner;
    this.status = FixtureStatus.FINISHED;
  }

  PlayerId winner() {

    return this.winner;
  }

  FixtureView toView() {

    return new FixtureView(this.id, this.matchdayNumber, this.playerOne, this.playerTwo,
        this.matchId, this.winner, this.status);
  }

}
