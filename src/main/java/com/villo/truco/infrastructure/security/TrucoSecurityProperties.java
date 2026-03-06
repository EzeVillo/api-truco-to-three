package com.villo.truco.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.security")
public record TrucoSecurityProperties(String jwtSecret, String issuer, String audience,
                                      long accessTokenExpirationSeconds) {

}
