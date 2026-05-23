package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.league")
public class LeagueTimeoutProperties {

  private int idleTimeoutSeconds = 600;

  public int getIdleTimeoutSeconds() {

    return idleTimeoutSeconds;
  }

  public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {

    this.idleTimeoutSeconds = idleTimeoutSeconds;
  }

}
