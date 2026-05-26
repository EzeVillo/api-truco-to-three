package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchStateVersionTest {

  @Test
  @DisplayName("El stateVersion inicia en 0 para un match nuevo")
  void stateVersionStartsAtZero() {

    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)), Visibility.PRIVATE);

    assertThat(match.getStateVersion()).isZero();
  }

  @Test
  @DisplayName("Cada evento transicional incrementa stateVersion en exactamente uno")
  void transitionalEventIncrementsStateVersionByOne() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

    assertThat(match.getStateVersion()).isZero();

    match.startMatch(playerOne);
    final var afterFirst = match.getStateVersion();
    assertThat(afterFirst).isEqualTo(1);

    final var eventsBefore = match.getMatchDomainEvents();
    assertThat(eventsBefore).hasSize(1);
    assertThat(eventsBefore.getFirst().getStateVersion()).isEqualTo(1);

    match.startMatch(playerTwo);
    final var afterSecond = match.getStateVersion();
    assertThat(afterSecond).isGreaterThan(afterFirst);
  }

  @Test
  @DisplayName("Los eventos transicionales llevan el stateVersion resultante tras incrementar")
  void transitionalEventsCarryStateVersion() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

    match.startMatch(playerOne);

    final var events = match.getMatchDomainEvents();
    assertThat(events).isNotEmpty();
    assertThat(events.getFirst().getStateVersion()).isEqualTo(1);
  }

  @Test
  @DisplayName("La reconstruccion restaura el stateVersion persistido")
  void reconstructionRestoresStateVersion() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var original = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    original.startMatch(playerOne);
    original.startMatch(playerTwo);

    final var snapshot = MatchSnapshotExtractor.extract(original);
    final var rehydrated = MatchRehydrator.rehydrate(snapshot);

    assertThat(rehydrated.getStateVersion()).isEqualTo(original.getStateVersion());
  }

  @Test
  @DisplayName("El stateVersion es monotono creciente y nunca decrece")
  void stateVersionIsMonotonicallyIncreasing() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

    match.startMatch(playerOne);
    final var v1 = match.getStateVersion();

    match.startMatch(playerTwo);
    final var v2 = match.getStateVersion();

    assertThat(v2).isGreaterThan(v1);
    assertThat(v1).isGreaterThan(0);
  }

  @Test
  @DisplayName("El evento de lobby publico no incrementa stateVersion")
  void publicLobbyEventDoesNotIncrementStateVersion() {

    final var playerOne = PlayerId.generate();
    final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PUBLIC);

    assertThat(match.getStateVersion()).isZero();

    final var events = match.getMatchDomainEvents();
    final var lobbyEvent = events.stream().filter(
            e -> e instanceof com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent)
        .findFirst();

    assertThat(lobbyEvent).isPresent();
    assertThat(lobbyEvent.get().getStateVersion()).isZero();
  }

}
