package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;

public record LeagueSnapshot(LeagueId id, List<PlayerId> participants,
                             List<FixtureSnapshot> fixtures, Map<PlayerId, Integer> winsByPlayer,
                             int numberOfPlayers, GamesToPlay gamesToPlay, InviteCode inviteCode,
                             LeagueStatus status) {

}
