package com.villo.truco.social.domain.model.preferences;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record SocialPreferencesSnapshot(PlayerId playerId, boolean acceptsFriendRequests) {

}
