package com.villo.truco.campaign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.campaign.domain.model.events.AllCampaignBotsUnlockedForCasualEvent;
import com.villo.truco.campaign.domain.model.events.CampaignAllRivalsDefeatedEvent;
import com.villo.truco.campaign.domain.model.events.CampaignBotUnlockedForCasualEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeLostEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeStartedEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeWonEvent;
import com.villo.truco.campaign.domain.model.events.CampaignTopOneReachedEvent;
import com.villo.truco.campaign.domain.model.exceptions.BotNotImmediatelyAboveException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignChallengeAlreadyActiveException;
import com.villo.truco.campaign.domain.model.exceptions.NoActiveCampaignChallengeException;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignProgress")
class CampaignProgressTest {

  private final CampaignBot top = new CampaignBot(PlayerId.generate(), "Cacho Medina", 1, 1000);
  private final CampaignBot middle = new CampaignBot(PlayerId.generate(), "Tito Toledo", 2, 500);
  private final CampaignBot bottom = new CampaignBot(PlayerId.generate(), "Rulo Suárez", 3, 100);
  private final CampaignLadder ladder = new CampaignLadder(List.of(top, middle, bottom));
  private final PlayerId playerId = PlayerId.generate();

  private static void beatRivalOn(final CampaignProgress progress, final CampaignBot rival,
      final CampaignLadder ladder) {

    final var matchId = MatchId.generate();
    progress.startChallenge(rival, matchId, ladder);
    progress.resolveChallengeWon(matchId, 3, 0, ladder);
  }

  @Test
  @DisplayName("solo puede desafiarse al bot inmediatamente superior antes de alcanzar el top 1")
  void onlyTheBotImmediatelyAboveCanBeChallenged() {

    final var progress = CampaignProgress.create(playerId);

    assertThatThrownBy(
        () -> progress.startChallenge(middle, MatchId.generate(), ladder)).isInstanceOf(
        BotNotImmediatelyAboveException.class);

    progress.startChallenge(bottom, MatchId.generate(), ladder);

    assertThat(progress.getCampaignDomainEvents()).singleElement()
        .isInstanceOf(CampaignChallengeStartedEvent.class);
  }

  @Test
  @DisplayName("no puede iniciarse un desafío con otro ya activo")
  void cannotStartChallengeWhileAnotherIsActive() {

    final var progress = CampaignProgress.create(playerId);
    progress.startChallenge(bottom, MatchId.generate(), ladder);

    assertThatThrownBy(
        () -> progress.startChallenge(bottom, MatchId.generate(), ladder)).isInstanceOf(
        CampaignChallengeAlreadyActiveException.class);
  }

  @Test
  @DisplayName("ganar 3-0 otorga 300 puntos y reporta el salto de posiciones")
  void winningThreeZeroAwardsThreeHundredPoints() {

    final var progress = CampaignProgress.create(playerId);
    final var matchId = MatchId.generate();
    progress.startChallenge(bottom, matchId, ladder);
    progress.clearDomainEvents();

    progress.resolveChallengeWon(matchId, 3, 0, ladder);

    assertThat(progress.getPoints().value()).isEqualTo(300);
    assertThat(progress.getActiveChallenge()).isNull();
    final var event = (CampaignChallengeWonEvent) progress.getCampaignDomainEvents().getFirst();
    assertThat(event.getPointsAwarded()).isEqualTo(300);
    assertThat(event.getPreviousPosition()).isEqualTo(4);
    assertThat(event.getNewPosition()).isEqualTo(3);
  }

  @Test
  @DisplayName("perder no descuenta puntos y registra la derrota contra el rival")
  void losingDoesNotSubtractPoints() {

    final var progress = CampaignProgress.create(playerId);
    final var matchId = MatchId.generate();
    progress.startChallenge(bottom, matchId, ladder);
    progress.clearDomainEvents();

    progress.resolveChallengeLost(matchId, ladder);

    assertThat(progress.getPoints().value()).isZero();
    assertThat(progress.getActiveChallenge()).isNull();
    assertThat(progress.getRivalRecords().get(bottom.playerId()).losses()).isEqualTo(1);
    assertThat(progress.getCampaignDomainEvents()).singleElement()
        .isInstanceOfSatisfying(CampaignChallengeLostEvent.class, event -> {
          assertThat(event.getRivalId()).isEqualTo(bottom.playerId());
          assertThat(event.getTotalPoints()).isZero();
          assertThat(event.getPreviousPosition()).isEqualTo(4);
          assertThat(event.getNewPosition()).isEqualTo(4);
        });
  }

  @Test
  @DisplayName("resolver un match que no es el desafío activo es un error de dominio")
  void resolvingAMatchThatIsNotTheActiveChallengeFails() {

    final var progress = CampaignProgress.create(playerId);

    assertThatThrownBy(
        () -> progress.resolveChallengeWon(MatchId.generate(), 3, 0, ladder)).isInstanceOf(
        NoActiveCampaignChallengeException.class);
  }

  @Test
  @DisplayName("al superar todos los puntos del ranking se alcanza el top 1 una única vez")
  void reachingTopOneIsEmittedOnce() {

    final var progress = CampaignProgress.create(playerId);
    beatRival(progress, bottom, 3, 0);
    beatRival(progress, middle, 3, 0);
    beatRival(progress, top, 3, 0);
    progress.clearDomainEvents();

    assertThat(progress.isTopOneReached()).isFalse();

    beatRival(progress, top, 3, 0);

    assertThat(progress.getPoints().value()).isEqualTo(1200);
    assertThat(progress.isTopOneReached()).isTrue();
    assertThat(progress.getCampaignDomainEvents()).filteredOn(
        CampaignTopOneReachedEvent.class::isInstance).hasSize(1);

    progress.clearDomainEvents();
    progress.startChallenge(bottom, MatchId.generate(), ladder);

    assertThat(progress.getCampaignDomainEvents()).filteredOn(
        CampaignTopOneReachedEvent.class::isInstance).isEmpty();
  }

  @Test
  @DisplayName("llegar al top 1 salteando rivales no marca todos vencidos; hay que volver y ganarles")
  void defeatingEveryRivalRequiresReturningForSkippedBots() {

    final var closeTop = new CampaignBot(PlayerId.generate(), "Cacho Medina", 1, 250);
    final var closeMiddle = new CampaignBot(PlayerId.generate(), "Tito Toledo", 2, 200);
    final var closeBottom = new CampaignBot(PlayerId.generate(), "Rulo Suárez", 3, 100);
    final var skipLadder = new CampaignLadder(List.of(closeTop, closeMiddle, closeBottom));
    final var progress = CampaignProgress.create(playerId);

    final var firstMatch = MatchId.generate();
    progress.startChallenge(closeBottom, firstMatch, skipLadder);
    progress.resolveChallengeWon(firstMatch, 3, 0, skipLadder);

    assertThat(progress.isTopOneReached()).isTrue();
    assertThat(progress.isAllRivalsDefeated()).isFalse();

    beatRivalOn(progress, closeMiddle, skipLadder);

    assertThat(progress.isAllRivalsDefeated()).isFalse();

    beatRivalOn(progress, closeTop, skipLadder);

    assertThat(progress.isAllRivalsDefeated()).isTrue();
    assertThat(progress.getCampaignDomainEvents()).filteredOn(
        CampaignAllRivalsDefeatedEvent.class::isInstance).hasSize(1);
  }

  @Test
  @DisplayName("canChallenge: antes del top 1 solo es desafiable el bot inmediatamente superior")
  void canChallengeOnlyTheBotImmediatelyAbove() {

    final var progress = CampaignProgress.create(playerId);

    assertThat(progress.canChallenge(bottom, ladder)).isTrue();
    assertThat(progress.canChallenge(middle, ladder)).isFalse();
    assertThat(progress.canChallenge(top, ladder)).isFalse();
  }

  @Test
  @DisplayName("canChallenge: con un desafío activo ningún bot es desafiable")
  void canChallengeIsFalseWhileAChallengeIsActive() {

    final var progress = CampaignProgress.create(playerId);
    progress.startChallenge(bottom, MatchId.generate(), ladder);

    assertThat(progress.canChallenge(bottom, ladder)).isFalse();
  }

  @Test
  @DisplayName("canChallenge: alcanzado el top 1 cualquier bot es desafiable")
  void canChallengeAnyBotAfterTopOne() {

    final var progress = CampaignProgress.create(playerId);
    beatRival(progress, bottom, 3, 0);
    beatRival(progress, middle, 3, 0);
    beatRival(progress, top, 3, 0);
    beatRival(progress, top, 3, 0);

    assertThat(progress.isTopOneReached()).isTrue();
    assertThat(progress.canChallenge(top, ladder)).isTrue();
    assertThat(progress.canChallenge(middle, ladder)).isTrue();
    assertThat(progress.canChallenge(bottom, ladder)).isTrue();
  }

  @Test
  @DisplayName("alcanzado el top 1 puede desafiarse a cualquier bot del ranking")
  void afterTopOneAnyBotCanBeChallenged() {

    final var progress = CampaignProgress.create(playerId);
    beatRival(progress, bottom, 3, 0);
    beatRival(progress, middle, 3, 0);
    beatRival(progress, top, 3, 0);
    beatRival(progress, top, 3, 0);

    assertThat(progress.isTopOneReached()).isTrue();

    progress.startChallenge(bottom, MatchId.generate(), ladder);

    assertThat(progress.getActiveChallenge().rivalId()).isEqualTo(bottom.playerId());
  }

  @Test
  @DisplayName("un bot se desbloquea para casual al llegar a neto +3 a favor, una sola vez")
  void botUnlockedForCasualWhenNetReachesThree() {

    final var progress = CampaignProgress.create(playerId);
    reachTopOne(progress);

    beatRivalOn(progress, bottom, ladder);
    progress.clearDomainEvents();
    assertThat(progress.getUnlockedCasualBots()).doesNotContain(bottom.playerId());

    beatRivalOn(progress, bottom, ladder);

    assertThat(progress.getUnlockedCasualBots()).contains(bottom.playerId());
    assertThat(progress.getCampaignDomainEvents()).filteredOn(
            CampaignBotUnlockedForCasualEvent.class::isInstance).singleElement()
        .isInstanceOfSatisfying(CampaignBotUnlockedForCasualEvent.class,
            event -> assertThat(event.getRivalId()).isEqualTo(bottom.playerId()));

    progress.clearDomainEvents();
    beatRivalOn(progress, bottom, ladder);

    assertThat(progress.getCampaignDomainEvents()).filteredOn(
        CampaignBotUnlockedForCasualEvent.class::isInstance).isEmpty();
  }

  @Test
  @DisplayName("perder tras desbloquear un bot no revierte el desbloqueo aunque el neto baje de 3")
  void unlockIsPermanentEvenIfNetDropsBelowThree() {

    final var progress = CampaignProgress.create(playerId);
    reachTopOne(progress);
    beatRivalOn(progress, bottom, ladder);
    beatRivalOn(progress, bottom, ladder);

    assertThat(progress.getUnlockedCasualBots()).contains(bottom.playerId());

    final var lossMatch = MatchId.generate();
    progress.startChallenge(bottom, lossMatch, ladder);
    progress.resolveChallengeLost(lossMatch, ladder);

    assertThat(progress.getRivalRecords().get(bottom.playerId()).net()).isLessThan(3);
    assertThat(progress.getUnlockedCasualBots()).contains(bottom.playerId());
  }

  @Test
  @DisplayName("desbloquear a todos los rivales emite el evento de todos desbloqueados una sola vez")
  void allBotsUnlockedEmittedOnce() {

    final var progress = CampaignProgress.create(playerId);
    reachTopOne(progress);

    grindToUnlock(progress, bottom);
    grindToUnlock(progress, middle);
    assertThat(progress.isAllCasualBotsUnlocked()).isFalse();

    grindToUnlock(progress, top);

    assertThat(progress.isAllCasualBotsUnlocked()).isTrue();
    assertThat(progress.getCampaignDomainEvents()).filteredOn(
        AllCampaignBotsUnlockedForCasualEvent.class::isInstance).hasSize(1);

    progress.clearDomainEvents();
    beatRivalOn(progress, bottom, ladder);

    assertThat(progress.getCampaignDomainEvents()).filteredOn(
        AllCampaignBotsUnlockedForCasualEvent.class::isInstance).isEmpty();
  }

  private void reachTopOne(final CampaignProgress progress) {

    beatRival(progress, bottom, 3, 0);
    beatRival(progress, middle, 3, 0);
    beatRival(progress, top, 3, 0);
    beatRival(progress, top, 3, 0);
    progress.clearDomainEvents();
  }

  private void grindToUnlock(final CampaignProgress progress, final CampaignBot rival) {

    while (!progress.getUnlockedCasualBots().contains(rival.playerId())) {
      beatRivalOn(progress, rival, ladder);
    }
  }

  private void beatRival(final CampaignProgress progress, final CampaignBot rival,
      final int gamesWonPlayer, final int gamesWonRival) {

    final var matchId = MatchId.generate();
    progress.startChallenge(rival, matchId, ladder);
    progress.resolveChallengeWon(matchId, gamesWonPlayer, gamesWonRival, ladder);
  }

}
