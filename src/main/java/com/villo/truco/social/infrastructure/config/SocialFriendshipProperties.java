package com.villo.truco.social.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.social.friendship")
public record SocialFriendshipProperties(int maxFriends) {

}
