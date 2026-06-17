package com.villo.truco.domain.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface BotVsBotMatchRegistry {

  void register(MatchId matchId, PlayerId ownerId);

  boolean isBotVsBotMatch(MatchId matchId);

  Optional<PlayerId> findOwnerByMatchId(MatchId matchId);

  Optional<MatchId> findActiveOwnedMatchId(PlayerId ownerId);

}
