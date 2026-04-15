package com.villo.truco.domain.model.league;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.events.PublicLeagueLobbyOpenedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("League visibility")
class LeagueVisibilityTest {

  @Test
  @DisplayName("publica y privada generan join code")
  void bothVisibilitiesGenerateJoinCode() {

    final var creator = PlayerId.generate();

    final var publicLeague = League.create(creator, 3, GamesToPlay.of(3), Visibility.PUBLIC);
    final var privateLeague = League.create(creator, 3, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThat(publicLeague.getJoinCode()).isNotNull();
    assertThat(privateLeague.getJoinCode()).isNotNull();
    assertThat(publicLeague.getLeagueDomainEvents().stream()
        .filter(PublicLeagueLobbyOpenedEvent.class::isInstance).count()).isEqualTo(1);
    assertThat(privateLeague.getLeagueDomainEvents().stream()
        .filter(PublicLeagueLobbyOpenedEvent.class::isInstance)).isEmpty();
  }

  @Test
  @DisplayName("join unificado completa liga privada y la deja esperando inicio manual")
  void joinKeepsPrivateLeagueManualFlowWhenFull() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(creator, 3, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThat(league.join(p2)).isEmpty();

    final var activations = league.join(p3);

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.WAITING_FOR_START);
    assertThat(activations).isEmpty();
    assertThat(league.getFixtures()).isEmpty();
  }

  @Test
  @DisplayName("joinPublic devuelve activaciones iniciales y arranca la liga publica al completarse")
  void joinPublicStartsPublicLeagueWhenFull() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(creator, 3, GamesToPlay.of(3), Visibility.PUBLIC);

    final var firstJoin = league.join(p2);
    final var activations = league.join(p3);

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.IN_PROGRESS);
    assertThat(firstJoin).isEmpty();
    assertThat(activations).isNotEmpty();
    assertThat(league.getFixtures()).isNotEmpty();
    assertThat(league.getLeagueDomainEvents().stream().filter(LeagueStartedEvent.class::isInstance)
        .count()).isEqualTo(1);
  }

  @Test
  @DisplayName("el lobby publico abierto depende solo del estado del agregado")
  void publicLobbyOpenDependsOnlyOnAggregateState() {

    final var league = League.create(PlayerId.generate(), 3, GamesToPlay.of(3), Visibility.PUBLIC);

    assertThat(league.isPublicLobbyOpen()).isTrue();

    league.join(PlayerId.generate());

    assertThat(league.isPublicLobbyOpen()).isTrue();

    league.join(PlayerId.generate());

    assertThat(league.isPublicLobbyOpen()).isFalse();
  }

}
