package com.villo.truco.application.dto;

public record UserPresenceDTO(boolean busy, ActiveMatchRefDTO match, ActiveLeagueRefDTO league,
                              ActiveCupRefDTO cup, ActiveRematchRefDTO rematch,
                              ActiveQuickMatchRefDTO quickMatch) {

  public static UserPresenceDTO of(final ActiveMatchRefDTO match, final ActiveLeagueRefDTO league,
      final ActiveCupRefDTO cup, final ActiveRematchRefDTO rematch,
      final ActiveQuickMatchRefDTO quickMatch) {

    final var busy =
        match != null || league != null || cup != null || rematch != null || quickMatch != null;
    return new UserPresenceDTO(busy, match, league, cup, rematch, quickMatch);
  }

}
