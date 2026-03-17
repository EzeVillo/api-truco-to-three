package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TournamentMatchFinishedHandler")
class TournamentMatchFinishedHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  private Tournament startedTournamentWithLinkedMatch(final MatchId matchId) {

    final var tournament = Tournament.create(this.playerOne, 3, GamesToPlay.of(3));
    tournament.join(this.playerTwo, tournament.getInviteCode());
    tournament.join(PlayerId.generate(), tournament.getInviteCode());
    tournament.start(this.playerOne);

    final var fixture = tournament.getFixtures().stream().filter(
            f -> (this.playerOne.equals(f.playerOne()) || this.playerOne.equals(f.playerTwo())) && (
                this.playerTwo.equals(f.playerOne()) || this.playerTwo.equals(f.playerTwo())))
        .findFirst().orElseThrow();
    tournament.linkFixtureMatch(fixture.fixtureId(), matchId);
    return tournament;
  }

  private TournamentMatchFinishedHandler handler(final Tournament tournament,
      final AtomicReference<Tournament> saved) {

    final TournamentQueryRepository queryRepo = new TournamentQueryRepository() {

      @Override
      public Optional<Tournament> findById(final TournamentId tournamentId) {

        return Optional.empty();
      }

      @Override
      public Optional<Tournament> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Tournament> findByMatchId(final MatchId matchId) {

        return Optional.ofNullable(tournament);
      }
    };

    final TournamentRepository repo = saved::set;
    return new TournamentMatchFinishedHandler(queryRepo, repo);
  }

  @Test
  @DisplayName("PLAYER_ONE gana → recordMatchWinner con playerOne y save")
  void playerOneWins_recordsWinnerAndSaves() {

    final var matchId = MatchId.generate();
    final var tournament = startedTournamentWithLinkedMatch(matchId);
    final var saved = new AtomicReference<Tournament>();
    final var handler = handler(tournament, saved);

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
    final var tournament = startedTournamentWithLinkedMatch(matchId);
    final var saved = new AtomicReference<Tournament>();
    final var handler = handler(tournament, saved);

    final var event = new MatchFinishedEvent(PlayerSeat.PLAYER_TWO, 0, 2);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getFixtures()).anyMatch(
        f -> matchId.equals(f.matchId()) && this.playerTwo.equals(f.winner()));
  }

  @Test
  @DisplayName("no hay torneo → no-op sin error")
  void noTournament_isNoOp() {

    final var saved = new AtomicReference<Tournament>();
    final var handler = handler(null, saved);

    final var matchId = MatchId.generate();
    final var event = new MatchFinishedEvent(PlayerSeat.PLAYER_ONE, 2, 0);
    final var context = new MatchEventContext(matchId, this.playerOne, this.playerTwo);

    handler.handle(event, context);

    assertThat(saved.get()).isNull();
  }

}
