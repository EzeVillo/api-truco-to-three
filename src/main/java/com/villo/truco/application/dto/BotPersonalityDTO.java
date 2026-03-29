package com.villo.truco.application.dto;

import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;

public record BotPersonalityDTO(int mentiroso, int pescador, int temerario, int envidoso,
                                int aguantador) {

  public static BotPersonalityDTO of(final BotPersonality personality) {

    return new BotPersonalityDTO(personality.mentiroso(), personality.pescador(),
        personality.temerario(), personality.envidoso(), personality.aguantador());
  }

}
