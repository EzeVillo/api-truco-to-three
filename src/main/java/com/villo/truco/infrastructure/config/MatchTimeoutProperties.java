package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.match")
public class MatchTimeoutProperties {

  private int lobbyTimeoutSeconds = 300;
  private int playTimeoutSeconds = 30;

  public int getLobbyTimeoutSeconds() {

    return lobbyTimeoutSeconds;
  }

  public void setLobbyTimeoutSeconds(int lobbyTimeoutSeconds) {

    this.lobbyTimeoutSeconds = lobbyTimeoutSeconds;
  }

  public int getPlayTimeoutSeconds() {

    return playTimeoutSeconds;
  }

  public void setPlayTimeoutSeconds(int playTimeoutSeconds) {

    this.playTimeoutSeconds = playTimeoutSeconds;
  }

}
