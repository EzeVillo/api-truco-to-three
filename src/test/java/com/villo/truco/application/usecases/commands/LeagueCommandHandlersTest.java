package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.CreateLeagueCommand;
import com.villo.truco.application.commands.JoinLeagueCommand;
import com.villo.truco.application.commands.LeaveLeagueCommand;
import com.villo.truco.application.commands.StartLeagueCommand;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("League command handlers")
class LeagueCommandHandlersTest {

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
        return new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
            cupQueryRepository);
    }

    @Test
    @DisplayName("CreateLeagueCommandHandler crea y persiste league")
    void createLeagueHandlerCreatesAndSaves() {

        final var saved = new AtomicReference<League>();
        final var handler = new CreateLeagueCommandHandler(saved::set, availableChecker());
        final var creator = PlayerId.generate();

        final var result = handler.handle(new CreateLeagueCommand(creator, 3, GamesToPlay.of(3)));

        assertThat(saved.get()).isNotNull();
        assertThat(result.leagueId()).isEqualTo(saved.get().getId().value().toString());
        assertThat(result.inviteCode()).isEqualTo(saved.get().getInviteCode().value());
    }

    @Test
    @DisplayName("JoinLeagueCommandHandler une jugador y persiste")
    void joinLeagueHandlerJoinsAndSaves() {

        final var creator = PlayerId.generate();
        final var joiner = PlayerId.generate();
        final var league = League.create(creator, 3, GamesToPlay.of(3));

        final LeagueQueryRepository queryRepository = new LeagueQueryRepository() {
            @Override
            public Optional<League> findById(final LeagueId leagueId) {

                return Optional.empty();
            }

            @Override
            public Optional<League> findByInviteCode(final InviteCode inviteCode) {

                return Optional.of(league);
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

        final var saved = new AtomicReference<League>();
        final var handler = new JoinLeagueCommandHandler(new LeagueResolver(queryRepository),
            saved::set, availableChecker());

        final var result = handler.handle(new JoinLeagueCommand(joiner, league.getInviteCode()));

        assertThat(result.leagueId()).isEqualTo(league.getId().value().toString());
        assertThat(saved.get()).isSameAs(league);
    }

    @Test
    @DisplayName("LeaveLeagueCommandHandler saca jugador y persiste")
    void leaveLeagueHandlerLeavesAndSaves() {

        final var creator = PlayerId.generate();
        final var leaver = PlayerId.generate();
        final var league = League.create(creator, 3, GamesToPlay.of(3));
        league.join(leaver, league.getInviteCode());

        final LeagueQueryRepository queryRepository = new LeagueQueryRepository() {
            @Override
            public Optional<League> findById(final LeagueId leagueId) {

                return Optional.of(league);
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

        final var saved = new AtomicReference<League>();
        final var handler = new LeaveLeagueCommandHandler(new LeagueResolver(queryRepository),
            saved::set);

        handler.handle(new LeaveLeagueCommand(league.getId(), leaver));

        assertThat(saved.get()).isSameAs(league);
    }

    @Test
    @DisplayName("StartLeagueCommandHandler crea matches para fixtures pendientes y persiste")
    void startLeagueHandlerCreatesMatchesAndSaves() {

        final var p1 = PlayerId.generate();
        final var p2 = PlayerId.generate();
        final var p3 = PlayerId.generate();
        final var league = League.create(p1, 3, GamesToPlay.of(3));
        league.join(p2, league.getInviteCode());
        league.join(p3, league.getInviteCode());

        final LeagueQueryRepository queryRepository = new LeagueQueryRepository() {
            @Override
            public Optional<League> findById(final LeagueId leagueId) {

                return Optional.of(league);
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

        final var leagueSaved = new AtomicReference<League>();
        final var matchSaves = new AtomicInteger();
        final MatchRepository matchRepository = match -> matchSaves.incrementAndGet();

        final var handler = new StartLeagueCommandHandler(new LeagueResolver(queryRepository),
            leagueSaved::set, matchRepository);

        handler.handle(new StartLeagueCommand(league.getId(), p1));

        final var pendingCount = league.getFixtures().stream()
            .filter(
                f -> f.status() == FixtureStatus.PENDING || f.status() == FixtureStatus.FINISHED)
            .count();

        assertThat(leagueSaved.get()).isSameAs(league);
        assertThat(matchSaves.get()).isEqualTo((int) pendingCount);
        assertThat(league.getFixtures().stream().filter(f -> f.status() == FixtureStatus.PENDING)
            .allMatch(f -> f.matchId() != null)).isTrue();
    }

}
