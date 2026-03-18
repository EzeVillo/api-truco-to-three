package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;

public final class LeagueSnapshot {

  private LeagueSnapshot() {

  }

  public record LeagueData(LeagueId id, List<PlayerId> participants, List<FixtureData> fixtures,
                           Map<PlayerId, Integer> winsByPlayer, int numberOfPlayers,
                           GamesToPlay gamesToPlay, InviteCode inviteCode, LeagueStatus status) {

  }

  public record FixtureData(FixtureId id, int matchdayNumber, PlayerId playerOne,
                            PlayerId playerTwo, MatchId matchId, PlayerId winner,
                            FixtureStatus status) {

  }

}
