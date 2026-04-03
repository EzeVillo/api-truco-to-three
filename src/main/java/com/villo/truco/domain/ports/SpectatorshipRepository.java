package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SpectatorshipRepository {

  Optional<Spectatorship> findBySpectatorId(PlayerId spectatorId);

  List<Spectatorship> findActiveByMatchId(MatchId matchId);

  Set<PlayerId> findActiveSpectatorIdsByMatchId(MatchId matchId);

  int countActiveByMatchId(MatchId matchId);

  void save(Spectatorship spectatorship);

}
