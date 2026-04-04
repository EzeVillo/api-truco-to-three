package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.commands.JoinPublicMatchCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Match command handlers")
class MatchCommandHandlersTest {

  private static PlayerId otherPlayer(final Match match, final PlayerId onePlayer) {

    return onePlayer.equals(match.getPlayerOne()) ? match.getPlayerTwo() : match.getPlayerOne();
  }

  private PlayerAvailabilityChecker availableChecker() {

    final MatchQueryRepository matchQueryRepository = new MatchQueryRepository() {
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

      @Override
      public List<Match> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
    final LeagueQueryRepository leagueQueryRepository = new LeagueQueryRepository() {
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
    final CupQueryRepository cupQueryRepository = new CupQueryRepository() {
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
    final BotRegistry noBotRegistry = new BotRegistry() {

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
    return new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
        cupQueryRepository, noBotRegistry);
  }

  private MatchResolver resolverFor(final Match match) {

    final MatchQueryRepository queryRepository = new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.of(match);
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.of(match);
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
    return new MatchResolver(queryRepository);
  }

  @Test
  @DisplayName("JoinMatchCommandHandler une jugador y persiste")
  void joinMatchHandlerJoinsAndSaves() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.create(p1, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PRIVATE);

    final MatchQueryRepository queryRepository = new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.of(match);
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

    final var saved = new AtomicReference<Match>();
    final MatchRepository repo = saved::set;
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final MatchEventNotifier notifier = publishedEvents::addAll;

    final var handler = new JoinMatchCommandHandler(new MatchResolver(queryRepository), repo,
        notifier, availableChecker());
    final var result = handler.handle(new JoinMatchCommand(p2, match.getInviteCode()));

    assertThat(result.matchId()).isEqualTo(match.getId().value().toString());
    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getPlayerTwo()).isEqualTo(p2);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("JoinPublicMatchCommandHandler une jugador, arranca y persiste")
  void joinPublicMatchHandlerStartsMatchWhenFull() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.create(p1, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PUBLIC);

    final MatchQueryRepository queryRepository = new MatchQueryRepository() {
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

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new JoinPublicMatchCommandHandler(new MatchResolver(queryRepository),
        saved::set, publishedEvents::addAll, availableChecker());

    final var result = handler.handle(new JoinPublicMatchCommand(match.getId(), p2));

    assertThat(result.matchId()).isEqualTo(match.getId().value().toString());
    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    assertThat(match.isReadyPlayerOne()).isTrue();
    assertThat(match.isReadyPlayerTwo()).isTrue();
    assertThat(match.getCurrentTurn()).isNotNull();
    assertThat(
        publishedEvents.stream().filter(GameStartedEvent.class::isInstance).count()).isEqualTo(1);
    assertThat(
        publishedEvents.stream().filter(PlayerReadyEvent.class::isInstance).count()).isZero();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("CallTrucoCommandHandler canta truco, persiste y publica")
  void callTrucoHandlerCallsAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new CallTrucoCommandHandler(resolverFor(match), saved::set,
        publishedEvents::addAll);

    final var currentTurn = match.getCurrentTurn();
    final var returnedId = handler.handle(new CallTrucoCommand(match.getId(), currentTurn));

    assertThat(returnedId).isEqualTo(match.getId());
    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("CallEnvidoCommandHandler canta envido, persiste y publica")
  void callEnvidoHandlerCallsAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new CallEnvidoCommandHandler(resolverFor(match), saved::set,
        publishedEvents::addAll);

    final var currentTurn = match.getCurrentTurn();
    final var returnedId = handler.handle(
        new CallEnvidoCommand(match.getId(), currentTurn, EnvidoCall.ENVIDO));

    assertThat(returnedId).isEqualTo(match.getId());
    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("PlayCardCommandHandler juega carta, persiste y publica")
  void playCardHandlerPlaysAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new PlayCardCommandHandler(resolverFor(match), saved::set,
        publishedEvents::addAll);

    final var currentTurn = match.getCurrentTurn();
    final var snapshot = MatchSnapshotExtractor.extract(match);
    final Card card =
        currentTurn.equals(snapshot.playerOne()) ? snapshot.currentRound().handPlayerOne().cards()
            .getFirst() : snapshot.currentRound().handPlayerTwo().cards().getFirst();
    final var returnedId = handler.handle(new PlayCardCommand(match.getId(), currentTurn, card));

    assertThat(returnedId).isEqualTo(match.getId());
    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("FoldCommandHandler se va al mazo, persiste y publica")
  void foldHandlerFoldsAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new FoldCommandHandler(resolverFor(match), saved::set,
        publishedEvents::addAll);

    final var firstTurn = match.getCurrentTurn();
    final var snapshot = MatchSnapshotExtractor.extract(match);
    final Card firstCard =
        firstTurn.equals(snapshot.playerOne()) ? snapshot.currentRound().handPlayerOne().cards()
            .getFirst() : snapshot.currentRound().handPlayerTwo().cards().getFirst();
    match.playCard(firstTurn, firstCard);

    final var returnedId = handler.handle(new FoldCommand(match.getId(), match.getCurrentTurn()));

    assertThat(returnedId).isEqualTo(match.getId());
    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("RespondTrucoCommandHandler responde QUIERO, persiste y publica")
  void respondTrucoHandlerAcceptsAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(p1);
    match.startMatch(p2);

    final var caller = match.getCurrentTurn();
    final var responder = otherPlayer(match, caller);
    match.callTruco(caller);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new RespondTrucoCommandHandler(resolverFor(match), saved::set,
        publishedEvents::addAll);

    final var returnedId = handler.handle(
        new RespondTrucoCommand(match.getId(), responder, TrucoResponse.QUIERO));

    assertThat(returnedId).isEqualTo(match.getId());
    assertThat(saved.get()).isSameAs(match);
    final var snapshot = MatchSnapshotExtractor.extract(match);
    assertThat(snapshot.currentRound().trucoStateMachine().pointsAtStake()).isEqualTo(2);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(publishedEvents).isNotEmpty();
  }

  @Test
  @DisplayName("RespondEnvidoCommandHandler responde QUIERO, persiste y publica")
  void respondEnvidoHandlerAcceptsAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(p1);
    match.startMatch(p2);

    final var caller = match.getCurrentTurn();
    final var responder = otherPlayer(match, caller);
    match.callEnvido(caller, EnvidoCall.ENVIDO);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new RespondEnvidoCommandHandler(resolverFor(match), saved::set,
        publishedEvents::addAll);

    handler.handle(new RespondEnvidoCommand(match.getId(), responder, EnvidoResponse.QUIERO));

    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(match.getScorePlayerOne() + match.getScorePlayerTwo()).isEqualTo(2);
    assertThat(publishedEvents).isNotEmpty();
  }

}
