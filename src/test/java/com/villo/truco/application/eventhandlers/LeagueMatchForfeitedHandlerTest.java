package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
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

@DisplayName("LeagueMatchForfeitedHandler")
class LeagueMatchForfeitedHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  private League startedLeague() {

    final var league = League.create(this.playerOne, 3, GamesToPlay.of(3));
    league.join(this.playerTwo, league.getInviteCode());
    league.join(PlayerId.generate(), league.getInviteCode());
    league.start(this.playerOne);
    return league;
  }

  private LeagueMatchForfeitedHandler handler(final League league,
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
    return new LeagueMatchForfeitedHandler(queryRepo, repo);
  }

  @Test
  @DisplayName("PLAYER_ONE gana (PLAYER_TWO forfeit) → forfeitPlayer(playerTwo) y save")
  void playerOneWins_forfeitsPlayerTwo() {

    final var league = startedLeague();
    final var saved = new AtomicReference<League>();
    final var handler = handler(league, saved);

    final var matchId = MatchId.generate();
    final var event = new MatchForfeitedEvent(PlayerSeat.PLAYER_ONE, 0, 0);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getFixtures()).filteredOn(
            f -> this.playerTwo.equals(f.playerOne()) || this.playerTwo.equals(f.playerTwo()))
        .noneMatch(f -> f.status() == FixtureStatus.PENDING);
  }

  @Test
  @DisplayName("PLAYER_TWO gana (PLAYER_ONE forfeit) → forfeitPlayer(playerOne) y save")
  void playerTwoWins_forfeitsPlayerOne() {

    final var league = startedLeague();
    final var saved = new AtomicReference<League>();
    final var handler = handler(league, saved);

    final var matchId = MatchId.generate();
    final var event = new MatchForfeitedEvent(PlayerSeat.PLAYER_TWO, 0, 0);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getFixtures()).filteredOn(
            f -> this.playerOne.equals(f.playerOne()) || this.playerOne.equals(f.playerTwo()))
        .noneMatch(f -> f.status() == FixtureStatus.PENDING);
  }

  @Test
  @DisplayName("no hay liga → no-op sin error")
  void noLeague_isNoOp() {

    final var saved = new AtomicReference<League>();
    final var handler = handler(null, saved);

    final var matchId = MatchId.generate();
    final var event = new MatchForfeitedEvent(PlayerSeat.PLAYER_ONE, 0, 0);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNull();
  }

}
