package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.AdvanceCupCommand;
import com.villo.truco.application.commands.CreateCupCommand;
import com.villo.truco.application.commands.ForfeitCupCommand;
import com.villo.truco.application.commands.JoinCupCommand;
import com.villo.truco.application.commands.StartCupCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cup command handlers")
class CupCommandHandlersTest {

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
    return new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
        cupQueryRepository, noBotRegistry);
  }

  @Test
  @DisplayName("CreateCupCommandHandler crea y persiste cup")
  void createCupHandlerCreatesAndSaves() {

    final var saved = new AtomicReference<Cup>();
    final var handler = new CreateCupCommandHandler(saved::set, availableChecker());
    final var creator = PlayerId.generate();

    final var result = handler.handle(new CreateCupCommand(creator, 4, GamesToPlay.of(3)));

    assertThat(saved.get()).isNotNull();
    assertThat(result.cupId()).isEqualTo(saved.get().getId().value().toString());
    assertThat(result.inviteCode()).isEqualTo(saved.get().getInviteCode().value());
  }

  @Test
  @DisplayName("JoinCupCommandHandler une jugador y persiste")
  void joinCupHandlerJoinsAndSaves() {

    final var creator = PlayerId.generate();
    final var joiner = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3));

    final CupQueryRepository queryRepository = new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

        return Optional.of(cup);
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
    };

    final var saved = new AtomicReference<Cup>();
    final var handler = new JoinCupCommandHandler(new CupResolver(queryRepository), saved::set,
        availableChecker(), events -> {
    });

    final var result = handler.handle(new JoinCupCommand(joiner, cup.getInviteCode()));

    assertThat(result.cupId()).isEqualTo(cup.getId().value().toString());
    assertThat(saved.get()).isSameAs(cup);
  }

  @Test
  @DisplayName("StartCupCommandHandler crea matches para bouts pendientes y persiste")
  void startCupHandlerCreatesMatchesAndSaves() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());

    final CupQueryRepository queryRepository = new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.of(cup);
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
    };

    final var cupSaved = new AtomicReference<Cup>();
    final var matchSaves = new AtomicInteger();
    final MatchRepository matchRepository = match -> matchSaves.incrementAndGet();

    final var handler = new StartCupCommandHandler(new CupResolver(queryRepository), cupSaved::set,
        matchRepository, events -> {
    });

    handler.handle(new StartCupCommand(cup.getId(), p1));

    assertThat(cupSaved.get()).isSameAs(cup);
    assertThat(matchSaves.get()).isGreaterThan(0);
    assertThat(cup.getBouts().stream().filter(b -> b.matchId() != null).count())
        .isEqualTo(matchSaves.get());
  }

  @Test
  @DisplayName("AdvanceCupCommandHandler registra ganador y persiste")
  void advanceCupHandlerRecordsWinnerAndSaves() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());
    cup.start(p1);

    final var firstPending = cup.getBouts().stream().filter(b -> b.status() == BoutStatus.PENDING)
        .findFirst().orElseThrow();
    final var matchId = MatchId.generate();
    cup.linkBoutMatch(firstPending.boutId(), matchId);

    final CupQueryRepository queryRepository = new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.of(cup);
      }

      @Override
      public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(final MatchId id) {

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
    };

    final var saved = new AtomicReference<Cup>();
    final var createdMatches = new AtomicInteger();
    final var handler = new AdvanceCupCommandHandler(new CupResolver(queryRepository), saved::set,
        match -> createdMatches.incrementAndGet(), events -> {
    });

    handler.handle(new AdvanceCupCommand(cup.getId(), matchId, firstPending.playerOne()));

    assertThat(saved.get()).isSameAs(cup);
    assertThat(
        cup.getBouts().stream().filter(b -> matchId.equals(b.matchId())).findFirst().orElseThrow()
            .winner()).isEqualTo(firstPending.playerOne());
    assertThat(createdMatches.get()).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("ForfeitCupCommandHandler registra abandono y persiste")
  void forfeitCupHandlerForfeitsAndSaves() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());
    cup.start(p1);

    final CupQueryRepository queryRepository = new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.of(cup);
      }

      @Override
      public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(final MatchId id) {

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
    };

    final var saved = new AtomicReference<Cup>();
    final var createdMatches = new AtomicInteger();
    final var handler = new ForfeitCupCommandHandler(new CupResolver(queryRepository), saved::set,
        match -> createdMatches.incrementAndGet(), events -> {
    });

    handler.handle(new ForfeitCupCommand(cup.getId(), p2));

    assertThat(saved.get()).isSameAs(cup);
    assertThat(cup.getBouts().stream().anyMatch(b -> b.winner() != null)).isTrue();
    assertThat(createdMatches.get()).isGreaterThanOrEqualTo(0);
  }

}
