package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.league")
public class LeagueTimeoutProperties {

  private int lobbyTimeoutSeconds = 600;

  public int getLobbyTimeoutSeconds() {

    return lobbyTimeoutSeconds;
  }

  public void setLobbyTimeoutSeconds(int lobbyTimeoutSeconds) {

    this.lobbyTimeoutSeconds = lobbyTimeoutSeconds;
  }

}
