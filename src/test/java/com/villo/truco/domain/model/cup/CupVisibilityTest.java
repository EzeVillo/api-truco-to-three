package com.villo.truco.domain.model.cup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.events.PublicCupLobbyOpenedEvent;
import com.villo.truco.domain.model.cup.exceptions.PrivateCupVisibilityAccessException;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cup visibility")
class CupVisibilityTest {

  @Test
  @DisplayName("publica no genera invite code y privada si")
  void publicDoesNotGenerateInviteCode() {

    final var creator = PlayerId.generate();

    final var publicCup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);
    final var privateCup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThat(publicCup.getInviteCode()).isNull();
    assertThat(privateCup.getInviteCode()).isNotNull();
    assertThat(
        publicCup.getCupDomainEvents().stream().filter(PublicCupLobbyOpenedEvent.class::isInstance)
            .count()).isEqualTo(1);
    assertThat(privateCup.getCupDomainEvents().stream()
        .filter(PublicCupLobbyOpenedEvent.class::isInstance)).isEmpty();
  }

  @Test
  @DisplayName("joinPublic rechaza copas privadas")
  void joinPublicRejectsPrivateCups() {

    final var cup = Cup.create(PlayerId.generate(), 4, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThatThrownBy(() -> cup.joinPublic(PlayerId.generate())).isInstanceOf(
        PrivateCupVisibilityAccessException.class);
  }

  @Test
  @DisplayName("joinPublic devuelve cruces iniciales y arranca la copa publica al completarse")
  void joinPublicStartsPublicCupWhenFull() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);

    final var firstJoin = cup.joinPublic(p2);
    final var secondJoin = cup.joinPublic(p3);
    final var pairings = cup.joinPublic(p4);

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

    cup.joinPublic(PlayerId.generate());

    assertThat(cup.isPublicLobbyOpen()).isTrue();

    cup.joinPublic(PlayerId.generate());
    cup.joinPublic(PlayerId.generate());

    assertThat(cup.isPublicLobbyOpen()).isFalse();
  }

}
