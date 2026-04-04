package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.assemblers.SpectatorMatchStateDTOAssembler;
import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.SpectatorCountChanged;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.spectator.SpectatingEligibilityPolicy;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.exceptions.AlreadySpectatingException;
import com.villo.truco.domain.model.spectator.exceptions.CannotSpectateOwnMatchException;
import com.villo.truco.domain.model.spectator.exceptions.SpectateNotAllowedException;
import com.villo.truco.domain.ports.CompetitionMembershipResolver;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import com.villo.truco.support.TestPublicActorResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpectateMatchCommandHandler")
class SpectateMatchCommandHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private PlayerId spectator;
  private Match match;
  private InMemorySpectatorshipRepository repository;
  private List<ApplicationEvent> publishedEvents;
  private SpectateMatchCommandHandler handler;

  private static MatchQueryRepository stubMatchRepo(final Match match) {

    return new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return matchId.equals(match.getId()) ? Optional.of(match) : Optional.empty();
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
  }

  private static MatchQueryRepository emptyMatchRepo() {

    return new MatchQueryRepository() {

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
  }

  private static LeagueQueryRepository leagueRepoWithParticipants(final MatchId matchId,
      final PlayerId... participants) {

    return new LeagueQueryRepository() {

      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(final MatchId id) {

        if (id.equals(matchId)) {
          final var league = League.create(participants[0], participants.length, GamesToPlay.of(3),
              Visibility.PRIVATE);
          for (int i = 1; i < participants.length; i++) {
            league.join(participants[i], league.getInviteCode());
          }
          return Optional.of(league);
        }
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
  }

  private static LeagueQueryRepository emptyLeagueRepo() {

    return new LeagueQueryRepository() {

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
  }

  private static CupQueryRepository emptyCupRepo() {

    return new CupQueryRepository() {

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
  }

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.spectator = PlayerId.generate();
    this.match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    this.match.startMatch(playerOne);
    this.match.startMatch(playerTwo);

    this.repository = new InMemorySpectatorshipRepository();
    this.publishedEvents = new ArrayList<>();
    final var matchRepo = stubMatchRepo(this.match);
    final CompetitionMembershipResolver resolver = (matchId, playerId) -> leagueRepoWithParticipants(
        this.match.getId(), this.playerOne, this.playerTwo, this.spectator).findByMatchId(matchId)
        .map(league -> league.getParticipants().contains(playerId)).orElse(false);
    final var assembler = new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle());
    final var countPublisher = new SpectatorCountChangedPublisher(matchRepo, this.repository,
        this.publishedEvents::add);

    this.handler = new SpectateMatchCommandHandler(matchRepo, this.repository,
        new SpectatingEligibilityPolicy(resolver), countPublisher, assembler);
  }

  @Test
  @DisplayName("espectador se registra correctamente y recibe DTO con estado")
  void success() {

    final var result = this.handler.handle(
        new SpectateMatchCommand(this.match.getId(), this.spectator));

    assertThat(result).isNotNull();
    assertThat(result.matchId()).isEqualTo(this.match.getId().value().toString());
    assertThat(result.scorePlayerOne()).isZero();
    assertThat(result.scorePlayerTwo()).isZero();
    assertThat(result.spectatorCount()).isEqualTo(1);
    assertThat(this.repository.findBySpectatorId(this.spectator)
        .flatMap(Spectatorship::getActiveMatchId)).contains(this.match.getId());
    assertThat(this.publishedEvents).hasSize(1);
    assertThat(this.publishedEvents.getFirst()).isInstanceOf(SpectatorCountChanged.class);
  }

  @Test
  @DisplayName("falla si el match no existe")
  void failsWhenMatchNotFound() {

    final var emptyMatchRepo = emptyMatchRepo();
    final var countPublisher = new SpectatorCountChangedPublisher(emptyMatchRepo, this.repository,
        this.publishedEvents::add);
    final var h = new SpectateMatchCommandHandler(emptyMatchRepo, this.repository,
        new SpectatingEligibilityPolicy((matchId, playerId) -> true), countPublisher,
        new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle()));

    assertThatThrownBy(
        () -> h.handle(new SpectateMatchCommand(MatchId.generate(), this.spectator))).isInstanceOf(
        MatchNotFoundException.class);
  }

  @Test
  @DisplayName("falla si el match no esta en progreso")
  void failsWhenMatchNotInProgress() {

    final var readyMatch = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    final var matchRepo = stubMatchRepo(readyMatch);
    final var h = new SpectateMatchCommandHandler(matchRepo, this.repository,
        new SpectatingEligibilityPolicy((matchId, playerId) -> true),
        new SpectatorCountChangedPublisher(matchRepo, this.repository, this.publishedEvents::add),
        new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle()));

    assertThatThrownBy(
        () -> h.handle(new SpectateMatchCommand(readyMatch.getId(), this.spectator))).isInstanceOf(
        InvalidMatchStateException.class);
  }

  @Test
  @DisplayName("falla si el espectador es uno de los jugadores")
  void failsWhenSpectatorIsPlayer() {

    assertThatThrownBy(() -> this.handler.handle(
        new SpectateMatchCommand(this.match.getId(), this.playerOne))).isInstanceOf(
        CannotSpectateOwnMatchException.class);
  }

  @Test
  @DisplayName("falla si ya esta especteando otro match")
  void failsWhenAlreadySpectating() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(MatchId.generate());
    this.repository.save(spectatorship);

    assertThatThrownBy(() -> this.handler.handle(
        new SpectateMatchCommand(this.match.getId(), this.spectator))).isInstanceOf(
        AlreadySpectatingException.class);
  }

  @Test
  @DisplayName("falla si no pertenece a la misma competición")
  void failsWhenNotInSameCompetition() {

    final var matchRepo = stubMatchRepo(this.match);
    final var h = new SpectateMatchCommandHandler(matchRepo, this.repository,
        new SpectatingEligibilityPolicy((matchId, playerId) -> false),
        new SpectatorCountChangedPublisher(matchRepo, this.repository, this.publishedEvents::add),
        new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle()));

    assertThatThrownBy(() -> h.handle(
        new SpectateMatchCommand(this.match.getId(), PlayerId.generate()))).isInstanceOf(
        SpectateNotAllowedException.class);
  }

}
