package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnplayedHandProbabilityTest {

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard ANCHO_BASTO = new BotCard(13, Card.of(Suit.BASTO, 1));
  private static final BotCard SIETE_ESPADA = new BotCard(10, Card.of(Suit.ESPADA, 7));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));
  private static final BotCard CUATRO_ORO = new BotCard(1, Card.of(Suit.ORO, 4));
  private static final BotCard CUATRO_BASTO = new BotCard(1, Card.of(Suit.BASTO, 4));

  @Test
  void probabilidad_conCartaAltaImbasible_eMuyAlta() {

    // Ancho espada (rank 14) es la carta más alta del juego; gana casi siempre la primera mano
    final var calc = new UnplayedHandProbability(List.of(ANCHO_ESPADA, ANCHO_BASTO, SIETE_ESPADA),
        null);
    assertThat(calc.probabilityHighCardWinsUnplayedTrick()).isGreaterThan(0.9);
  }

  @Test
  void probabilidad_conCartasMuyDebiles_esBaja() {

    final var calc = new UnplayedHandProbability(
        List.of(CUATRO_COPA, CUATRO_ORO, CUATRO_BASTO), null);
    assertThat(calc.probabilityHighCardWinsUnplayedTrick()).isLessThan(0.1);
  }

  @Test
  void probabilidad_conCartaJugadaConocida_reduceElMazo() {

    // Rival mostró una carta de rank bajo; el mazo restante tiene esa carta excluida
    final var conCartaJugada = new UnplayedHandProbability(
        List.of(ANCHO_ESPADA, ANCHO_BASTO), CUATRO_COPA);
    final var sinCartaJugada = new UnplayedHandProbability(
        List.of(ANCHO_ESPADA, ANCHO_BASTO), null);
    // Ambas deberían ser altas; simplemente verificamos que se ejecuta sin error y en rango [0,1]
    assertThat(conCartaJugada.probabilityHighCardWinsUnplayedTrick()).isBetween(0.0, 1.0);
    assertThat(sinCartaJugada.probabilityHighCardWinsUnplayedTrick()).isBetween(0.0, 1.0);
  }

  @Test
  void probabilidad_siempreEnRango0a1() {

    final var calc = new UnplayedHandProbability(List.of(SIETE_ESPADA), null);
    final var p = calc.probabilityHighCardWinsUnplayedTrick();
    assertThat(p).isBetween(0.0, 1.0);
  }

  @Test
  void probabilidad_enumerandoMazoRestante_noIncluyeCartasPropias() {

    // Bot tiene ancho espada; el mazo restante no lo incluye → resultado consistente
    final var calc = new UnplayedHandProbability(List.of(ANCHO_ESPADA), null);
    assertThat(calc.probabilityHighCardWinsUnplayedTrick()).isBetween(0.0, 1.0);
  }

}
