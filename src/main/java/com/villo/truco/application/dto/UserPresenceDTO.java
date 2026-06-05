package com.villo.truco.application.dto;

public record UserPresenceDTO(boolean busy, ActiveMatchRefDTO match, ActiveLeagueRefDTO league,
                              ActiveCupRefDTO cup, ActiveRematchRefDTO rematch) {

  public static UserPresenceDTO of(final ActiveMatchRefDTO match, final ActiveLeagueRefDTO league,
      final ActiveCupRefDTO cup, final ActiveRematchRefDTO rematch) {

    final var busy = match != null || league != null || cup != null || rematch != null;
    return new UserPresenceDTO(busy, match, league, cup, rematch);
  }

}
