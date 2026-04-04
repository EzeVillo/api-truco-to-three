package com.villo.truco.domain.model.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.events.PublicLeagueLobbyOpenedEvent;
import com.villo.truco.domain.model.league.exceptions.PrivateLeagueVisibilityAccessException;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("League visibility")
class LeagueVisibilityTest {

  @Test
  @DisplayName("publica no genera invite code y privada si")
  void publicDoesNotGenerateInviteCode() {

    final var creator = PlayerId.generate();

    final var publicLeague = League.create(creator, 3, GamesToPlay.of(3), Visibility.PUBLIC);
    final var privateLeague = League.create(creator, 3, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThat(publicLeague.getInviteCode()).isNull();
    assertThat(privateLeague.getInviteCode()).isNotNull();
    assertThat(publicLeague.getLeagueDomainEvents().stream()
        .filter(PublicLeagueLobbyOpenedEvent.class::isInstance).count()).isEqualTo(1);
    assertThat(privateLeague.getLeagueDomainEvents().stream()
        .filter(PublicLeagueLobbyOpenedEvent.class::isInstance)).isEmpty();
  }

  @Test
  @DisplayName("joinPublic rechaza ligas privadas")
  void joinPublicRejectsPrivateLeagues() {

    final var league = League.create(PlayerId.generate(), 3, GamesToPlay.of(3), Visibility.PRIVATE);

    assertThatThrownBy(() -> league.joinPublic(PlayerId.generate())).isInstanceOf(
        PrivateLeagueVisibilityAccessException.class);
  }

  @Test
  @DisplayName("joinPublic devuelve activaciones iniciales y arranca la liga publica al completarse")
  void joinPublicStartsPublicLeagueWhenFull() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(creator, 3, GamesToPlay.of(3), Visibility.PUBLIC);

    final var firstJoin = league.joinPublic(p2);
    final var activations = league.joinPublic(p3);

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

    league.joinPublic(PlayerId.generate());

    assertThat(league.isPublicLobbyOpen()).isTrue();

    league.joinPublic(PlayerId.generate());

    assertThat(league.isPublicLobbyOpen()).isFalse();
  }

}
