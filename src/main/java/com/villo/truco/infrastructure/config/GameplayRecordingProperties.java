package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.recording")
public class GameplayRecordingProperties {

  private boolean enabled = true;

  public boolean isEnabled() {

    return enabled;
  }

  public void setEnabled(boolean enabled) {

    this.enabled = enabled;
  }

}
