package com.villo.truco.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.web")
public record TrucoWebProperties(List<String> allowedOrigins) {

  public String[] allowedOriginsArray() {

    return this.allowedOrigins.toArray(new String[0]);
  }

}
