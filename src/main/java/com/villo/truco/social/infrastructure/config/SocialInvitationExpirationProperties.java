package com.villo.truco.social.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.social.invitation-expiration")
public record SocialInvitationExpirationProperties(Duration match, Duration league, Duration cup) {

}
