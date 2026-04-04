package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent;
import com.villo.truco.domain.model.match.exceptions.PrivateMatchVisibilityAccessException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Match visibility")
class MatchVisibilityTest {

  @Test
  @DisplayName("publico no genera invite code y privado si")
  void publicDoesNotGenerateInviteCode() {

    final var player = PlayerId.generate();

    final var publicMatch = Match.create(player, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PUBLIC);
    final var privateMatch = Match.create(player, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PRIVATE);

    assertThat(publicMatch.getInviteCode()).isNull();
    assertThat(privateMatch.getInviteCode()).isNotNull();
    assertThat(publicMatch.getMatchDomainEvents().stream()
        .filter(PublicMatchLobbyOpenedEvent.class::isInstance).count()).isEqualTo(1);
    assertThat(privateMatch.getMatchDomainEvents().stream()
        .filter(PublicMatchLobbyOpenedEvent.class::isInstance)).isEmpty();
  }

  @Test
  @DisplayName("joinPublic rechaza partidas privadas")
  void joinPublicRejectsPrivateMatches() {

    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)), Visibility.PRIVATE);

    assertThatThrownBy(() -> match.joinPublic(PlayerId.generate())).isInstanceOf(
        PrivateMatchVisibilityAccessException.class);
  }

  @Test
  @DisplayName("joinPublic completa y arranca el match publico en un solo paso")
  void joinPublicStartsPublicMatchWhenFull() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PUBLIC);

    match.joinPublic(playerTwo);

    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    assertThat(match.isReadyPlayerOne()).isTrue();
    assertThat(match.isReadyPlayerTwo()).isTrue();
    assertThat(match.getCurrentTurn()).isNotNull();
    assertThat(match.getDomainEvents().stream().filter(GameStartedEvent.class::isInstance)
        .count()).isEqualTo(1);
    assertThat(match.getDomainEvents().stream().filter(PlayerReadyEvent.class::isInstance)
        .count()).isZero();
  }

  @Test
  @DisplayName("el lobby publico abierto depende solo del estado del agregado")
  void publicLobbyOpenDependsOnlyOnAggregateState() {

    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)), Visibility.PUBLIC);

    assertThat(match.isPublicLobbyOpen()).isTrue();

    match.joinPublic(PlayerId.generate());

    assertThat(match.isPublicLobbyOpen()).isFalse();
  }

}
