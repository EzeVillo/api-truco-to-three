package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;

public interface FriendshipParticipantsPort {

  Optional<List<PlayerId>> findParticipantsIfAccepted(String friendshipId, PlayerId requesterId);

}
