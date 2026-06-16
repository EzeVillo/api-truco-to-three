package com.villo.truco.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotVsBotRematchVeto")
class BotVsBotRematchVetoTest {

  @Test
  @DisplayName("vetea la revancha si el match es bot-vs-bot")
  void vetoesRematchForBotVsBotMatch() {

    final var registry = new InMemoryBotVsBotMatchRegistry();
    final var matchId = MatchId.generate();
    registry.register(matchId, PlayerId.generate());
    final var veto = new BotVsBotRematchVeto(registry);

    assertThat(veto.vetoesRematch(matchId)).isTrue();
  }

  @Test
  @DisplayName("no vetea la revancha si el match no es bot-vs-bot")
  void doesNotVetoRematchForNonBotVsBotMatch() {

    final var veto = new BotVsBotRematchVeto(new InMemoryBotVsBotMatchRegistry());

    assertThat(veto.vetoesRematch(MatchId.generate())).isFalse();
  }

}
