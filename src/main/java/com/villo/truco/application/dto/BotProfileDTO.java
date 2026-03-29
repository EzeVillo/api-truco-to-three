package com.villo.truco.application.dto;

import com.villo.truco.domain.model.bot.BotProfile;

public record BotProfileDTO(String botId, String name, BotPersonalityDTO personality) {

  public static BotProfileDTO of(final BotProfile profile) {

    return new BotProfileDTO(profile.playerId().value().toString(), profile.displayName(),
        BotPersonalityDTO.of(profile.personality()));
  }

}
