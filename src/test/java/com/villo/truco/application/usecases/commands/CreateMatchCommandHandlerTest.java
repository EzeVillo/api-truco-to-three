package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateMatchCommandHandler")
class CreateMatchCommandHandlerTest {

  private static final MatchQueryRepository NO_ACTIVE_MATCH_REPO = new MatchQueryRepository() {

    @Override
    public Optional<Match> findById(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

      return Optional.empty();
    }

    @Override
    public boolean hasActiveMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public boolean hasUnfinishedMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public List<MatchId> findIdleMatchIds(final Instant idleSince) {

      return List.of();
    }
  };

  private static final LeagueQueryRepository NO_LEAGUE_REPO = new LeagueQueryRepository() {

    @Override
    public Optional<League> findById(final LeagueId leagueId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findByInviteCode(final InviteCode inviteCode) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findByMatchId(
        final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findInProgressByPlayer(
        final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findWaitingByPlayer(
        final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

      return java.util.List.of();
    }
  };

  private static final CupQueryRepository NO_CUP_REPO = new CupQueryRepository() {

    @Override
    public Optional<Cup> findById(final CupId cupId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findInProgressByPlayer(
        final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findWaitingByPlayer(
        final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<CupId> findIdleCupIds(final Instant idleSince) {

      return List.of();
    }
  };

  private static final PlayerAvailabilityChecker FREE_CHECKER = new PlayerAvailabilityChecker(
      NO_ACTIVE_MATCH_REPO, NO_LEAGUE_REPO, NO_CUP_REPO);

  @Test
  @DisplayName("crea partida con gamesToPlay valido")
  void createsMatch() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, FREE_CHECKER);

    handler.handle(new CreateMatchCommand(PlayerId.generate().value().toString(), 3));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getId()).isNotNull();
    assertThat(savedMatch.get().getInviteCode()).isNotNull();
  }

  @Test
  @DisplayName("falla si gamesToPlay no es 1, 3 o 5")
  void failsWhenGamesToPlayIsInvalid() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, FREE_CHECKER);

    assertThatThrownBy(() -> handler.handle(
        new CreateMatchCommand(PlayerId.generate().value().toString(), 7))).isInstanceOf(
        DomainException.class);

    assertThat(savedMatch.get()).isNull();
  }

  @Test
  @DisplayName("el match creado pertenece al jugador del comando")
  void matchBelongsToCommandPlayer() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, FREE_CHECKER);
    final var playerId = PlayerId.generate();

    handler.handle(new CreateMatchCommand(playerId.value().toString(), 5));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getPlayerOne()).isEqualTo(playerId);
  }

  @Test
  @DisplayName("falla si el jugador ya tiene un match sin finalizar")
  void failsWhenPlayerHasUnfinishedMatch() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;

    final MatchQueryRepository busyMatchRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public boolean hasUnfinishedMatch(final PlayerId playerId) {

        return true;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }
    };

    final var busyChecker = new PlayerAvailabilityChecker(busyMatchRepo, NO_LEAGUE_REPO,
        NO_CUP_REPO);
    final var handler = new CreateMatchCommandHandler(repository, busyChecker);

    assertThatThrownBy(() -> handler.handle(
        new CreateMatchCommand(PlayerId.generate().value().toString(), 3))).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);

    assertThat(savedMatch.get()).isNull();
  }

}
