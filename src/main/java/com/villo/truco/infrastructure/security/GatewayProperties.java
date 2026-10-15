package com.villo.truco.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.gateway")
public record GatewayProperties(boolean enabled, String secret) {

  @Override
  public String toString() {
    return "GatewayProperties[enabled=" + enabled + ", secret=[redacted]]";
  }
}
