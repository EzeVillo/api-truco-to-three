package com.villo.truco.application.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Set;

public record SpectatorCountChanged(MatchId matchId, List<PlayerId> players,
                                    Set<PlayerId> spectators, int count) implements
    ApplicationEvent {

}
