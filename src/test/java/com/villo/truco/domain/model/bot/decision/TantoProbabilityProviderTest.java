package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.match.CardEvaluationService;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class TantoProbabilityProviderTest {

  private static final com.villo.truco.domain.model.bot.EnvidoScoring SCORING =
      CardEvaluationService::envidoScore;

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard ANCHO_BASTO = new BotCard(13, Card.of(Suit.BASTO, 1));
  private static final BotCard SIETE_ESPADA = new BotCard(10, Card.of(Suit.ESPADA, 7));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));
  private static final BotCard CUATRO_ORO = new BotCard(1, Card.of(Suit.ORO, 4));
  private static final BotCard CUATRO_BASTO = new BotCard(1, Card.of(Suit.BASTO, 4));

  @Test
  void probabilidadBotGanaConManoMuyFuerte_esMayorA0punto5() {

    // 33 puntos: mano imbatible
    final var provider = new TantoProbabilityProvider(SCORING,
        List.of(ANCHO_ESPADA, ANCHO_BASTO, SIETE_ESPADA), 33, false, null);
    assertThat(provider.probabilityBotWinsTanto()).isGreaterThan(0.5);
  }

  @Test
  void probabilidadBotGanaConManoMuyDebil_esMenorA0punto5() {

    final var provider = new TantoProbabilityProvider(SCORING,
        List.of(CUATRO_COPA, CUATRO_ORO, CUATRO_BASTO), 0, false, null);
    assertThat(provider.probabilityBotWinsTanto()).isLessThan(0.5);
  }

  @Test
  void moreLikelyToWinTanto_cuandoProbabilidadMayorA0punto5() {

    final var provider = new TantoProbabilityProvider(SCORING,
        List.of(ANCHO_ESPADA, ANCHO_BASTO, SIETE_ESPADA), 33, false, null);
    assertThat(provider.moreLikelyToWinTanto()).isTrue();
    assertThat(provider.moreLikelyToLoseTanto()).isFalse();
  }

  @Test
  void moreLikelyToLoseTanto_cuandoProbabilidadMenorA0punto5() {

    final var provider = new TantoProbabilityProvider(SCORING,
        List.of(CUATRO_COPA, CUATRO_ORO, CUATRO_BASTO), 0, false, null);
    assertThat(provider.moreLikelyToLoseTanto()).isTrue();
    assertThat(provider.moreLikelyToWinTanto()).isFalse();
  }

  @Test
  void delegaEnEnvidoProbabilityCalculator_conCartaJugadaPorRival() {

    // Rival mostró un 7 espada: bot tiene el ancho espada (mano alta), debería ganar con alta prob
    final var provider = new TantoProbabilityProvider(SCORING,
        List.of(ANCHO_ESPADA, ANCHO_BASTO), 27, false, SIETE_ESPADA);
    assertThat(provider.probabilityBotWinsTanto()).isGreaterThan(0.5);
  }

  @Test
  void tie_cuandoProbabilidadEsExactamente0punto5() {

    // Construir un escenario de tie es difícil con cartas reales; verificamos solo la lógica.
    // Un provider con prob 0.5 exacto reporta tie=true y moreLikelyToWin/Lose=false.
    final var provider = TantoProbabilityProvider.withKnownProbability(0.5);
    assertThat(provider.tie()).isTrue();
    assertThat(provider.moreLikelyToWinTanto()).isFalse();
    assertThat(provider.moreLikelyToLoseTanto()).isFalse();
  }

}
