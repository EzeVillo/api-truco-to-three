package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.MatchTimeoutEntry;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Match command handlers")
class MatchCommandHandlersTest {

  private static PlayerId otherPlayer(final Match match, final PlayerId onePlayer) {

    return onePlayer.equals(match.getPlayerOne()) ? match.getPlayerTwo() : match.getPlayerOne();
  }

  private static MatchRepository repoSaving(final AtomicReference<Match> ref) {

    return new MatchRepository() {
      @Override
      public void save(final Match match) {

        ref.set(match);
      }

      @Override
      public Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
  }

  private MatchResolver resolverFor(final Match match) {

    final MatchQueryRepository queryRepository = new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId matchId) {

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
      public Optional<Match> findUnfinishedByPlayer(final PlayerId playerId) {

        return Optional.empty();
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

      @Override
      public Set<PlayerId> findPlayersWithUnfinishedMatch(final Set<PlayerId> playerIds) {

        return Set.of();
      }

      @Override
      public Map<PlayerId, Match> findUnfinishedByPlayers(final Set<PlayerId> playerIds) {

        return Map.of();
      }
    };
    return new MatchResolver(queryRepository);
  }

  @Test
  @DisplayName("CallTrucoCommandHandler canta truco, persiste y publica")
  void callTrucoHandlerCallsAndPublishes() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new CallTrucoCommandHandler(resolverFor(match), repoSaving(saved),
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
    final var match = Match.createReady(p1, p2,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new CallEnvidoCommandHandler(resolverFor(match), repoSaving(saved),
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
    final var match = Match.createReady(p1, p2,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new PlayCardCommandHandler(resolverFor(match), repoSaving(saved),
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
    final var match = Match.createReady(p1, p2,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(p1);
    match.startMatch(p2);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new FoldCommandHandler(resolverFor(match), repoSaving(saved),
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
    final var match = Match.createReady(p1, p2,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(p1);
    match.startMatch(p2);

    final var caller = match.getCurrentTurn();
    final var responder = otherPlayer(match, caller);
    match.callTruco(caller);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new RespondTrucoCommandHandler(resolverFor(match), repoSaving(saved),
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
    final var match = Match.createReady(p1, p2,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(p1);
    match.startMatch(p2);

    final var caller = match.getCurrentTurn();
    final var responder = otherPlayer(match, caller);
    match.callEnvido(caller, EnvidoCall.ENVIDO);

    final var saved = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = new RespondEnvidoCommandHandler(resolverFor(match), repoSaving(saved),
        publishedEvents::addAll);

    handler.handle(new RespondEnvidoCommand(match.getId(), responder, EnvidoResponse.QUIERO));

    assertThat(saved.get()).isSameAs(match);
    assertThat(match.getDomainEvents()).isEmpty();
    assertThat(match.getScorePlayerOne() + match.getScorePlayerTwo()).isEqualTo(2);
    assertThat(publishedEvents).isNotEmpty();
  }

}
