package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInWaitingCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInWaitingLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.quickmatch.exceptions.PlayerAlreadyInQueueException;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerAvailabilityChecker")
class PlayerAvailabilityCheckerTest {

  private static PlayerAvailabilityChecker checker(final boolean hasUnfinishedMatch,
      final Optional<League> inProgressLeague, final Optional<Cup> inProgressCup) {

    return checker(hasUnfinishedMatch, false, inProgressLeague, Optional.empty(), inProgressCup,
        Optional.empty());
  }

  private static PlayerAvailabilityChecker checker(final boolean hasUnfinishedMatch,
      final Optional<League> inProgressLeague, final Optional<League> waitingLeague,
      final Optional<Cup> inProgressCup, final Optional<Cup> waitingCup) {

    return checker(hasUnfinishedMatch, false, inProgressLeague, waitingLeague, inProgressCup,
        waitingCup);
  }

  private static PlayerAvailabilityChecker checker(final boolean hasUnfinishedMatch,
      final boolean hasActiveMatch, final Optional<League> inProgressLeague,
      final Optional<League> waitingLeague, final Optional<Cup> inProgressCup,
      final Optional<Cup> waitingCup) {

    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.hasUnfinishedMatch(any())).thenReturn(hasUnfinishedMatch);
    when(matchRepo.hasActiveMatch(any())).thenReturn(hasActiveMatch);

    final var leagueRepo = mock(LeagueQueryRepository.class);
    when(leagueRepo.findInProgressByPlayer(any())).thenReturn(inProgressLeague);
    when(leagueRepo.findWaitingByPlayer(any())).thenReturn(waitingLeague);

    final var cupRepo = mock(CupQueryRepository.class);
    when(cupRepo.findInProgressByPlayer(any())).thenReturn(inProgressCup);
    when(cupRepo.findWaitingByPlayer(any())).thenReturn(waitingCup);

    final var botRegistry = mock(BotRegistry.class);
    final var rematchRepo = mock(RematchSessionRepository.class);
    when(rematchRepo.findOpenByPlayer(any())).thenReturn(Optional.empty());
    final var quickMatchQueuePort = mock(QuickMatchQueuePort.class);
    when(quickMatchQueuePort.isPlayerQueued(any())).thenReturn(false);
    return new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo, botRegistry, rematchRepo,
        quickMatchQueuePort);
  }

  @Test
  @DisplayName("jugador libre pasa sin excepción")
  void freePlayerPassesWithNoException() {

    final var checker = checker(false, Optional.empty(), Optional.empty());

    assertThatCode(() -> checker.ensureAvailable(PlayerId.generate())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("jugador con match sin finalizar → PlayerAlreadyInActiveMatchException")
  void throwsWhenPlayerHasUnfinishedMatch() {

    final var checker = checker(true, Optional.empty(), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(PlayerId.generate())).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  @Test
  @DisplayName("jugador con fixtures pendientes en liga → PlayerBusyInLeagueException")
  void throwsWhenPlayerBusyInLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);
    league.start(p1);

    final var checker = checker(false, Optional.of(league), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(p1)).isInstanceOf(
        PlayerBusyInLeagueException.class);
  }

  @Test
  @DisplayName("jugador que forfeitó en liga no está bloqueado")
  void forfeitedLeaguePlayerIsNotBlocked() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);
    league.start(p1);
    league.forfeitPlayer(p1);

    final var checker = checker(false, Optional.of(league), Optional.empty());

    assertThatCode(() -> checker.ensureAvailable(p1)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("jugador activo en copa → PlayerBusyInCupException")
  void throwsWhenPlayerStillCompetingInCup() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    cup.join(p2);
    cup.join(p3);
    cup.join(p4);
    cup.start(p1);

    final var checker = checker(false, Optional.empty(), Optional.of(cup));

    assertThatThrownBy(() -> checker.ensureAvailable(p1)).isInstanceOf(
        PlayerBusyInCupException.class);
  }

  @Test
  @DisplayName("jugador forfeitado en copa no está bloqueado")
  void forfeitedCupPlayerIsNotBlocked() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    cup.join(p2);
    cup.join(p3);
    cup.join(p4);
    cup.start(p1);
    cup.forfeitPlayer(p1);

    final var checker = checker(false, Optional.empty(), Optional.of(cup));

    assertThatCode(() -> checker.ensureAvailable(p1)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("short-circuit: match bloqueante no evalúa liga ni copa")
  void shortCircuitsOnMatchCheck() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);
    league.start(p1);

    final var checker = checker(true, Optional.of(league), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(p1)).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  @Test
  @DisplayName("jugador en liga WAITING_FOR_PLAYERS → PlayerAlreadyInWaitingLeagueException")
  void throwsWhenPlayerInWaitingLeague() {

    final var creator = PlayerId.generate();
    final var waitingLeague = League.create(creator, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    final var checker = checker(false, Optional.empty(), Optional.of(waitingLeague),
        Optional.empty(), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(creator)).isInstanceOf(
        PlayerAlreadyInWaitingLeagueException.class);
  }

  @Test
  @DisplayName("jugador en copa WAITING_FOR_PLAYERS → PlayerAlreadyInWaitingCupException")
  void throwsWhenPlayerInWaitingCup() {

    final var creator = PlayerId.generate();
    final var waitingCup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    final var checker = checker(false, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.of(waitingCup));

    assertThatThrownBy(() -> checker.ensureAvailable(creator)).isInstanceOf(
        PlayerAlreadyInWaitingCupException.class);
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador libre pasa sin excepción")
  void canStartMatchWhenFree() {

    final var checker = checker(false, false, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.empty());

    assertThatCode(
        () -> checker.ensureCanStartMatch(PlayerId.generate())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador con partido activo → PlayerAlreadyInActiveMatchException")
  void cannotStartMatchWhenHasActiveMatch() {

    final var checker = checker(false, true, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.empty());

    assertThatThrownBy(() -> checker.ensureCanStartMatch(PlayerId.generate())).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador en torneo activo (IN_PROGRESS) puede iniciar su partida de torneo")
  void canStartTournamentMatchWhenInActiveTournament() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);
    league.start(p1);

    final var checker = checker(false, false, Optional.of(league), Optional.empty(),
        Optional.empty(), Optional.empty());

    assertThatCode(() -> checker.ensureCanStartMatch(p1)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador en torneo en espera no puede iniciar un match")
  void cannotStartMatchWhenInWaitingTournament() {

    final var creator = PlayerId.generate();
    final var waitingLeague = League.create(creator, 3, GamesToPlay.of(3), Visibility.PRIVATE);

    final var checker = checker(false, false, Optional.empty(), Optional.of(waitingLeague),
        Optional.empty(), Optional.empty());

    assertThatThrownBy(() -> checker.ensureCanStartMatch(creator)).isInstanceOf(
        PlayerAlreadyInWaitingLeagueException.class);
  }

  @Test
  @DisplayName("jugador en cola de quick match → PlayerAlreadyInQueueException")
  void throwsWhenPlayerInQuickMatchQueue() {

    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.hasUnfinishedMatch(any())).thenReturn(false);
    when(matchRepo.hasActiveMatch(any())).thenReturn(false);
    final var leagueRepo = mock(LeagueQueryRepository.class);
    when(leagueRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(leagueRepo.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    final var cupRepo = mock(CupQueryRepository.class);
    when(cupRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(cupRepo.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    final var botRegistry = mock(BotRegistry.class);
    final var rematchRepo = mock(RematchSessionRepository.class);
    when(rematchRepo.findOpenByPlayer(any())).thenReturn(Optional.empty());
    final var quickMatchQueuePort = mock(QuickMatchQueuePort.class);
    when(quickMatchQueuePort.isPlayerQueued(any())).thenReturn(true);
    final var checker = new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo, botRegistry,
        rematchRepo, quickMatchQueuePort);

    assertThatThrownBy(() -> checker.ensureAvailable(PlayerId.generate())).isInstanceOf(
        PlayerAlreadyInQueueException.class);
  }

  @Test
  @DisplayName("jugador libre sin cola → ensureAvailable no lanza excepción")
  void noQueueEntryDoesNotBlock() {

    final var checker = checker(false, Optional.empty(), Optional.empty());

    assertThatCode(() -> checker.ensureAvailable(PlayerId.generate())).doesNotThrowAnyException();
  }

}
