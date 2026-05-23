package com.villo.truco.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.rematch")
public record RematchExpirationProperties(Duration duration, int batchSize) {

}
