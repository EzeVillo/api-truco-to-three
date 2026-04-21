package com.villo.truco.social.domain.model.friendship;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;

public record FriendshipSnapshot(FriendshipId id, PlayerId requesterId, PlayerId addresseeId,
                                 FriendshipStatus status) {

}
