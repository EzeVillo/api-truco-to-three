package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.MatchNotFullException;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
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
      final AtomicReference<Match> savedMatch, final List<DomainEventBase> publishedEvents) {

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
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }
    };

    final MatchRepository matchRepository = savedMatch::set;
    final MatchEventNotifier notifier = (matchId, p1, p2, events) -> publishedEvents.addAll(events);
    final var resolver = new MatchResolver(queryRepo);
    return new StartMatchCommandHandler(resolver, matchRepository, queryRepo, notifier);
  }

  @Test
  @DisplayName("lanza MatchNotFullException cuando playerTwo no se unió")
  void throwsMatchNotFullExceptionWhenPlayerTwoIsAbsent() {

    final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);

    assertThatThrownBy(
        () -> handler.handle(new StartMatchCommand(match.getId(), playerOne))).isInstanceOf(
        MatchNotFullException.class);
  }

}
