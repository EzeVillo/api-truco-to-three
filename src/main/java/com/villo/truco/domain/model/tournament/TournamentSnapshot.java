package com.villo.truco.domain.model.tournament;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureId;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;

public final class TournamentSnapshot {

  private TournamentSnapshot() {

  }

  public record TournamentData(TournamentId id, List<PlayerId> participants,
                               List<FixtureData> fixtures, Map<PlayerId, Integer> winsByPlayer,
                               int numberOfPlayers, GamesToPlay gamesToPlay, InviteCode inviteCode,
                               TournamentStatus status) {

  }

  public record FixtureData(FixtureId id, int matchdayNumber, PlayerId playerOne,
                            PlayerId playerTwo, MatchId matchId, PlayerId winner,
                            FixtureStatus status) {

  }

}
