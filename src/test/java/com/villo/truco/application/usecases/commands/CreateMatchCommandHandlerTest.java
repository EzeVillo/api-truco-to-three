package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
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

    @Override
    public List<Match> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(findPublicWaiting(), null);
    }
  };

  private static final LeagueQueryRepository NO_LEAGUE_REPO = new LeagueQueryRepository() {

    @Override
    public Optional<League> findById(final LeagueId leagueId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

      return java.util.List.of();
    }

    @Override
    public List<League> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<League> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(findPublicWaiting(), null);
    }
  };

  private static final CupQueryRepository NO_CUP_REPO = new CupQueryRepository() {

    @Override
    public Optional<Cup> findById(final CupId cupId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<CupId> findIdleCupIds(final Instant idleSince) {

      return List.of();
    }

    private List<Cup> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(findPublicWaiting(), null);
    }
  };

  private static final BotRegistry NO_BOT_REGISTRY = new BotRegistry() {

    @Override
    public boolean isBot(final PlayerId playerId) {

      return false;
    }

    @Override
    public Optional<BotProfile> getProfile(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<BotProfile> getAll() {

      return List.of();
    }

    @Override
    public void register(final BotProfile profile) {

    }
  };

  private static final PlayerAvailabilityChecker FREE_CHECKER = new PlayerAvailabilityChecker(
      NO_ACTIVE_MATCH_REPO, NO_LEAGUE_REPO, NO_CUP_REPO, NO_BOT_REGISTRY);
  private static final MatchEventNotifier NO_OP_MATCH_EVENT_NOTIFIER = events -> {
  };

  @Test
  @DisplayName("crea partida con gamesToPlay valido")
  void createsMatch() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, NO_OP_MATCH_EVENT_NOTIFIER,
        FREE_CHECKER);

    handler.handle(new CreateMatchCommand(PlayerId.generate().value().toString(), 3,
        Visibility.PRIVATE.name()));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getId()).isNotNull();
    assertThat(savedMatch.get().getJoinCode()).isNotNull();
  }

  @Test
  @DisplayName("falla si gamesToPlay no es 1, 3 o 5")
  void failsWhenGamesToPlayIsInvalid() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, NO_OP_MATCH_EVENT_NOTIFIER,
        FREE_CHECKER);

    assertThatThrownBy(() -> handler.handle(
        new CreateMatchCommand(PlayerId.generate().value().toString(), 7,
            Visibility.PRIVATE.name()))).isInstanceOf(DomainException.class);

    assertThat(savedMatch.get()).isNull();
  }

  @Test
  @DisplayName("el match creado pertenece al jugador del comando")
  void matchBelongsToCommandPlayer() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, NO_OP_MATCH_EVENT_NOTIFIER,
        FREE_CHECKER);
    final var playerId = PlayerId.generate();

    handler.handle(
        new CreateMatchCommand(playerId.value().toString(), 5, Visibility.PRIVATE.name()));

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

      @Override
      public List<Match> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };

    final var busyChecker = new PlayerAvailabilityChecker(busyMatchRepo, NO_LEAGUE_REPO,
        NO_CUP_REPO, NO_BOT_REGISTRY);
    final var handler = new CreateMatchCommandHandler(repository, NO_OP_MATCH_EVENT_NOTIFIER,
        busyChecker);

    assertThatThrownBy(() -> handler.handle(
        new CreateMatchCommand(PlayerId.generate().value().toString(), 3,
            Visibility.PRIVATE.name()))).isInstanceOf(PlayerAlreadyInActiveMatchException.class);

    assertThat(savedMatch.get()).isNull();
  }

  @Test
  @DisplayName("publica y limpia domain events al crear un lobby publico")
  void publishesAndClearsDomainEventsForPublicMatchCreation() {

    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new java.util.ArrayList<MatchDomainEvent>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository, publishedEvents::addAll,
        FREE_CHECKER);

    handler.handle(new CreateMatchCommand(PlayerId.generate().value().toString(), 3,
        Visibility.PUBLIC.name()));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(publishedEvents).hasSize(1);
    assertThat(publishedEvents.getFirst()).isInstanceOf(PublicMatchLobbyOpenedEvent.class);
    assertThat(savedMatch.get().getMatchDomainEvents()).isEmpty();
  }

}
