package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.util.List;
import java.util.Map;

public record LeagueSnapshot(LeagueId id, List<PlayerId> participants,
                             List<FixtureSnapshot> fixtures, Map<PlayerId, Integer> winsByPlayer,
                             int numberOfPlayers, GamesToPlay gamesToPlay, Visibility visibility,
                             JoinCode joinCode, LeagueStatus status) {

}
