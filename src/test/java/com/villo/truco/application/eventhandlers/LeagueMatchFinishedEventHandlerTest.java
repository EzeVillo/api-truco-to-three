package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeagueMatchFinishedHandler")
class LeagueMatchFinishedEventHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  private League startedLeagueWithLinkedMatch(final MatchId matchId) {

    final var league = League.create(this.playerOne, 3, GamesToPlay.of(3));
    league.join(this.playerTwo, league.getInviteCode());
    league.join(PlayerId.generate(), league.getInviteCode());
    league.start(this.playerOne);

    final var fixture = league.getFixtures().stream().filter(
            f -> (this.playerOne.equals(f.playerOne()) || this.playerOne.equals(f.playerTwo())) && (
                this.playerTwo.equals(f.playerOne()) || this.playerTwo.equals(f.playerTwo())))
        .findFirst().orElseThrow();
    league.linkFixtureMatch(fixture.fixtureId(), matchId);
    return league;
  }

  private LeagueMatchFinishedEventHandler handler(final League league,
      final AtomicReference<League> saved) {

    final LeagueQueryRepository queryRepo = new LeagueQueryRepository() {

      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(final MatchId matchId) {

        return Optional.ofNullable(league);
      }
    };

    final LeagueRepository repo = saved::set;
    return new LeagueMatchFinishedEventHandler(queryRepo, repo);
  }

  @Test
  @DisplayName("PLAYER_ONE gana → recordMatchWinner con playerOne y save")
  void playerOneWins_recordsWinnerAndSaves() {

    final var matchId = MatchId.generate();
    final var league = startedLeagueWithLinkedMatch(matchId);
    final var saved = new AtomicReference<League>();
    final var handler = handler(league, saved);

    final var event = new MatchFinishedEvent(PlayerSeat.PLAYER_ONE, 2, 1);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getFixtures()).anyMatch(
        f -> matchId.equals(f.matchId()) && this.playerOne.equals(f.winner()));
  }

  @Test
  @DisplayName("PLAYER_TWO gana → recordMatchWinner con playerTwo y save")
  void playerTwoWins_recordsWinnerAndSaves() {

    final var matchId = MatchId.generate();
    final var league = startedLeagueWithLinkedMatch(matchId);
    final var saved = new AtomicReference<League>();
    final var handler = handler(league, saved);

    final var event = new MatchFinishedEvent(PlayerSeat.PLAYER_TWO, 0, 2);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getFixtures()).anyMatch(
        f -> matchId.equals(f.matchId()) && this.playerTwo.equals(f.winner()));
  }

  @Test
  @DisplayName("no hay liga → no-op sin error")
  void noLeague_isNoOp() {

    final var saved = new AtomicReference<League>();
    final var handler = handler(null, saved);

    final var matchId = MatchId.generate();
    final var event = new MatchFinishedEvent(PlayerSeat.PLAYER_ONE, 2, 0);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNull();
  }

}
