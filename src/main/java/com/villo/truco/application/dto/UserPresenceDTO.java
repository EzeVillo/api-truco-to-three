package com.villo.truco.application.dto;

public record UserPresenceDTO(boolean busy, ActiveMatchRefDTO match, ActiveLeagueRefDTO league,
                              ActiveCupRefDTO cup, ActiveRematchRefDTO rematch,
                              ActiveQuickMatchRefDTO quickMatch,
                              ActiveSpectatingRefDTO spectating,
                              ActiveOwnedBotMatchRefDTO ownedBotMatch) {

  public static UserPresenceDTO of(final ActiveMatchRefDTO match, final ActiveLeagueRefDTO league,
      final ActiveCupRefDTO cup, final ActiveRematchRefDTO rematch,
      final ActiveQuickMatchRefDTO quickMatch, final ActiveSpectatingRefDTO spectating,
      final ActiveOwnedBotMatchRefDTO ownedBotMatch) {

    final var busy =
        match != null || league != null || cup != null || rematch != null || quickMatch != null
            || spectating != null || ownedBotMatch != null;
    return new UserPresenceDTO(busy, match, league, cup, rematch, quickMatch, spectating,
        ownedBotMatch);
  }

}
