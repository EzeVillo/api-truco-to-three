package com.villo.truco.domain.model.bot.valueobjects;

import com.villo.truco.domain.model.bot.exceptions.InvalidBotPersonalityException;

public record BotPersonality(int mentiroso, int pescador, int temerario, int envidoso,
                             int aguantador) {

  private static final String MENTIROSO = "mentiroso";
  private static final String PESCADOR = "pescador";
  private static final String TEMERARIO = "temerario";
  private static final String ENVIDOSO = "envidoso";
  private static final String AGUANTADOR = "aguantador";

  public BotPersonality {

    validate(MENTIROSO, mentiroso);
    validate(PESCADOR, pescador);
    validate(TEMERARIO, temerario);
    validate(ENVIDOSO, envidoso);
    validate(AGUANTADOR, aguantador);
  }

  private static void validate(final String name, final int value) {

    if (value < 1 || value > 100) {
      throw new InvalidBotPersonalityException(name, value);
    }
  }

}
