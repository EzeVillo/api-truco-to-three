package com.villo.truco.domain.model.cup;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.events.PublicCupLobbyOpenedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cup visibility")
class CupVisibilityTest {

  @Test
  @DisplayName("publica y privada generan join code")
  void bothVisibilitiesGenerateJoinCode() {

    final var creator = PlayerId.generate();

    final var publicCup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);
    final var privateCup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThat(publicCup.getJoinCode()).isNotNull();
    assertThat(privateCup.getJoinCode()).isNotNull();
    assertThat(
        publicCup.getCupDomainEvents().stream().filter(PublicCupLobbyOpenedEvent.class::isInstance)
            .count()).isEqualTo(1);
    assertThat(privateCup.getCupDomainEvents().stream()
        .filter(PublicCupLobbyOpenedEvent.class::isInstance)).isEmpty();
  }

  @Test
  @DisplayName("join unificado completa cup privada y la deja esperando inicio manual")
  void joinKeepsPrivateCupManualFlowWhenFull() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThat(cup.join(p2)).isEmpty();
    assertThat(cup.join(p3)).isEmpty();

    final var pairings = cup.join(p4);

    assertThat(cup.getStatus()).isEqualTo(CupStatus.WAITING_FOR_START);
    assertThat(pairings).isEmpty();
    assertThat(cup.getBouts()).isEmpty();
  }

  @Test
  @DisplayName("joinPublic devuelve cruces iniciales y arranca la copa publica al completarse")
  void joinPublicStartsPublicCupWhenFull() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);

    final var firstJoin = cup.join(p2);
    final var secondJoin = cup.join(p3);
    final var pairings = cup.join(p4);

    assertThat(cup.getStatus()).isEqualTo(CupStatus.IN_PROGRESS);
    assertThat(firstJoin).isEmpty();
    assertThat(secondJoin).isEmpty();
    assertThat(pairings).isNotEmpty();
    assertThat(cup.getBouts()).isNotEmpty();
    assertThat(cup.getCupDomainEvents().stream().filter(CupStartedEvent.class::isInstance)
        .count()).isEqualTo(1);
  }

  @Test
  @DisplayName("el lobby publico abierto depende solo del estado del agregado")
  void publicLobbyOpenDependsOnlyOnAggregateState() {

    final var cup = Cup.create(PlayerId.generate(), 4, GamesToPlay.of(3), Visibility.PUBLIC);

    assertThat(cup.isPublicLobbyOpen()).isTrue();

    cup.join(PlayerId.generate());

    assertThat(cup.isPublicLobbyOpen()).isTrue();

    cup.join(PlayerId.generate());
    cup.join(PlayerId.generate());

    assertThat(cup.isPublicLobbyOpen()).isFalse();
  }

}
