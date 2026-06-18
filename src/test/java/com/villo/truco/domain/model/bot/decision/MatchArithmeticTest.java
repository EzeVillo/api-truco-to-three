package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchArithmeticTest {

  private static final int POINTS_TO_WIN = 3;

  @Test
  void rivalBustsIfAccepts_cuandoSumaExcedeLimite() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalBustsIfAccepts(2)).isTrue();
  }

  @Test
  void rivalBustsIfAccepts_cuandoSumaEsExacta_noEsTrue() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalBustsIfAccepts(1)).isFalse();
  }

  @Test
  void rivalBustsIfRejects_cuandoRechazarExcedeLimite() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalBustsIfRejects(2)).isTrue();
  }

  @Test
  void rivalBustsIfRejects_cuandoRechazarNoExcede_esFalse() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalBustsIfRejects(1)).isFalse();
  }

  @Test
  void botBustsIfAccepts_cuandoBotExcedeLimite() {

    final var a = new MatchArithmetic(2, 0, POINTS_TO_WIN);
    assertThat(a.botBustsIfAccepts(2)).isTrue();
  }

  @Test
  void botBustsIfAccepts_cuandoBotNoExcede_esFalse() {

    final var a = new MatchArithmetic(0, 0, POINTS_TO_WIN);
    assertThat(a.botBustsIfAccepts(2)).isFalse();
  }

  @Test
  void botReachesExact_cuandoSumaEsExacta() {

    final var a = new MatchArithmetic(2, 0, POINTS_TO_WIN);
    assertThat(a.botReachesExact(1)).isTrue();
  }

  @Test
  void botReachesExact_cuandoNoEsExacta_esFalse() {

    final var a = new MatchArithmetic(2, 0, POINTS_TO_WIN);
    assertThat(a.botReachesExact(2)).isFalse();
  }

  @Test
  void rivalReachesExact_cuandoSumaEsExacta() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalReachesExact(1)).isTrue();
  }

  @Test
  void rivalReachesExact_cuandoNoEsExacta_esFalse() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalReachesExact(2)).isFalse();
  }

  @Test
  void rivalWinsMatchIfBotLoses_cuandoRivalGanaConLaApuesta() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalWinsMatchIfBotLoses(1)).isTrue();
  }

  @Test
  void rivalWinsMatchIfBotLoses_cuandoRivalExcedeConLaApuesta_tambienEsTrue() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalWinsMatchIfBotLoses(2)).isTrue();
  }

  @Test
  void rivalWinsMatchIfBotLoses_cuandoRivalNoAlcanza_esFalse() {

    final var a = new MatchArithmetic(0, 1, POINTS_TO_WIN);
    assertThat(a.rivalWinsMatchIfBotLoses(1)).isFalse();
  }

  @Test
  void rivalBustsIfWins_equivalenteARivalBustsIfAccepts() {

    final var a = new MatchArithmetic(0, 2, POINTS_TO_WIN);
    assertThat(a.rivalBustsIfWins(2)).isEqualTo(a.rivalBustsIfAccepts(2));
  }

  @Test
  void botBustsIfWins_equivalenteABotBustsIfAccepts() {

    final var a = new MatchArithmetic(2, 0, POINTS_TO_WIN);
    assertThat(a.botBustsIfWins(2)).isEqualTo(a.botBustsIfAccepts(2));
  }

  @Test
  void funcionaConFormatoDeOtroPuntaje_noHardcodea3() {

    final var a = new MatchArithmetic(4, 4, 6);
    assertThat(a.rivalBustsIfAccepts(3)).isTrue();
    assertThat(a.rivalBustsIfAccepts(2)).isFalse();
  }

}
