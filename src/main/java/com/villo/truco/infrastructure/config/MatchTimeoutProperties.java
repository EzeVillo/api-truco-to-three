package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.match")
public class MatchTimeoutProperties {

  private int idleTimeoutSeconds = 300;
  private long timeoutCheckIntervalMs = 30000;

  public int getIdleTimeoutSeconds() {

    return idleTimeoutSeconds;
  }

  public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {

    this.idleTimeoutSeconds = idleTimeoutSeconds;
  }

  public long getTimeoutCheckIntervalMs() {

    return timeoutCheckIntervalMs;
  }

  public void setTimeoutCheckIntervalMs(long timeoutCheckIntervalMs) {

    this.timeoutCheckIntervalMs = timeoutCheckIntervalMs;
  }

}
