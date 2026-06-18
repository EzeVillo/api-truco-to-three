package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardLockAnalyzerTest {

  private static final BotCard ANCHO = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard BASTO = new BotCard(13, Card.of(Suit.BASTO, 1));
  private static final BotCard SIETE = new BotCard(10, Card.of(Suit.ESPADA, 7));
  private static final BotCard CUATRO = new BotCard(1, Card.of(Suit.COPA, 4));

  private static GameContext ctx(final List<BotCard> myCards, final BotCard rivalCardPlayed,
      final int rivalCardsInHand) {

    return new GameContext(myCards, 0, 0, rivalCardPlayed, 0, 0, false, true, false, false, 3,
        rivalCardsInHand);
  }

  @Test
  void rivalIsOutOfCards_cuandoRivalCardsInHandEsCero() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), CUATRO, 0));
    assertThat(analyzer.rivalIsOutOfCards()).isTrue();
  }

  @Test
  void rivalIsOutOfCards_cuandoRivalTieneCarta_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), null, 1));
    assertThat(analyzer.rivalIsOutOfCards()).isFalse();
  }

  @Test
  void rivalCannotQYMVAM_equivalenteARivalIsOutOfCards() {

    final var conCartas = new CardLockAnalyzer(ctx(List.of(ANCHO), null, 1));
    final var sinCartas = new CardLockAnalyzer(ctx(List.of(ANCHO), CUATRO, 0));
    assertThat(conCartas.rivalCannotQYMVAM()).isFalse();
    assertThat(sinCartas.rivalCannotQYMVAM()).isTrue();
  }

  @Test
  void botBeatsPlayedCard_cuandoBotTieneCartaMayor() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), CUATRO, 0));
    assertThat(analyzer.botBeatsPlayedCard()).isTrue();
  }

  @Test
  void botBeatsPlayedCard_cuandoBotNoPuedeMatar_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(CUATRO), ANCHO, 0));
    assertThat(analyzer.botBeatsPlayedCard()).isFalse();
  }

  @Test
  void botBeatsPlayedCard_cuandoRivalNoJugoNada_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), null, 1));
    assertThat(analyzer.botBeatsPlayedCard()).isFalse();
  }

  @Test
  void botHasGuaranteedTrick_cuandoRivalSinCartasYBotMata() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), CUATRO, 0));
    assertThat(analyzer.botHasGuaranteedTrick()).isTrue();
  }

  @Test
  void botHasGuaranteedTrick_cuandoRivalAunTieneCartas_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), CUATRO, 1));
    assertThat(analyzer.botHasGuaranteedTrick()).isFalse();
  }

  @Test
  void botHasGuaranteedTrick_cuandoBotNoPuedeMatar_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(CUATRO), ANCHO, 0));
    assertThat(analyzer.botHasGuaranteedTrick()).isFalse();
  }

  @Test
  void leadsToLockIfAdvance_cuandoBotMataYTieneCartaRestante() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO, SIETE), CUATRO, 0));
    assertThat(analyzer.leadsToLockIfAdvance()).isTrue();
  }

  @Test
  void leadsToLockIfAdvance_cuandoBotSoloTieneUnaCartaNoGeneraEncierro() {

    // bot tiene solo 1 carta: al jugarla queda sin cartas, no hay encierro
    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO), CUATRO, 0));
    assertThat(analyzer.leadsToLockIfAdvance()).isFalse();
  }

  @Test
  void leadsToLockIfAdvance_cuandoRivalAunTieneCartas_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(ANCHO, SIETE), CUATRO, 1));
    assertThat(analyzer.leadsToLockIfAdvance()).isFalse();
  }

  @Test
  void leadsToLockIfAdvance_cuandoBotNoPuedeMatar_esFalse() {

    final var analyzer = new CardLockAnalyzer(ctx(List.of(CUATRO, SIETE), ANCHO, 0));
    assertThat(analyzer.leadsToLockIfAdvance()).isFalse();
  }

  @Test
  void parda_cuandoBotYRivalTienenMismoRango_botNoPuedeMatar() {

    final var empate = new BotCard(14, Card.of(Suit.BASTO, 1));
    final var analyzer = new CardLockAnalyzer(ctx(List.of(empate), ANCHO, 0));
    assertThat(analyzer.botBeatsPlayedCard()).isFalse();
  }

}
