package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.exceptions.MatchNotFullException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StartMatchCommandHandler")
class StartMatchCommandHandlerTest {

  private PlayerId playerOne;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
  }

  private StartMatchCommandHandler handlerWith(final Match match,
      final AtomicReference<Match> savedMatch, final List<MatchDomainEvent> publishedEvents) {

    final MatchQueryRepository queryRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.of(match);
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

      @Override
      public List<Match> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };

    final MatchRepository matchRepository = savedMatch::set;
    final MatchEventNotifier notifier = publishedEvents::addAll;
    final var resolver = new MatchResolver(queryRepo);
    final LeagueQueryRepository leagueQueryRepo = new LeagueQueryRepository() {
      @Override
      public Optional<League> findById(LeagueId id) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByInviteCode(InviteCode c) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(MatchId id) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(PlayerId p) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findWaitingByPlayer(PlayerId p) {

        return Optional.empty();
      }

      @Override
      public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

        return List.of();
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
    final CupQueryRepository cupQueryRepo = new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(CupId id) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByInviteCode(InviteCode c) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(MatchId id) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(PlayerId p) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(PlayerId p) {

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
    final BotRegistry noBotRegistry = new BotRegistry() {

      @Override
      public boolean isBot(final PlayerId p) {

        return false;
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId p) {

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
    final var checker = new PlayerAvailabilityChecker(queryRepo, leagueQueryRepo, cupQueryRepo,
        noBotRegistry);
    return new StartMatchCommandHandler(resolver, matchRepository, notifier, checker);
  }

  @Test
  @DisplayName("lanza MatchNotFullException cuando playerTwo no se unió")
  void throwsMatchNotFullExceptionWhenPlayerTwoIsAbsent() {

    final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)),
        Visibility.PRIVATE);
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);

    assertThatThrownBy(
        () -> handler.handle(new StartMatchCommand(match.getId(), playerOne))).isInstanceOf(
        MatchNotFullException.class);
  }

}
