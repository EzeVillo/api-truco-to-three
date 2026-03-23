package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.league.FixtureSnapshot;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.LeagueRehydrator;
import com.villo.truco.domain.model.league.LeagueSnapshot;
import com.villo.truco.domain.model.league.LeagueSnapshotExtractor;
import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.LeagueFixtureJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.LeagueJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.LeagueParticipantJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.LeagueWinJpaEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LeagueMapper {

  public LeagueJpaEntity toEntity(final League league) {

    final var snapshot = LeagueSnapshotExtractor.extract(league);
    final var entity = new LeagueJpaEntity();

    entity.setId(snapshot.id().value());
    entity.setNumberOfPlayers(snapshot.numberOfPlayers());
    entity.setGamesToPlay(snapshot.gamesToPlay().value());
    entity.setInviteCode(snapshot.inviteCode().value());
    entity.setStatus(snapshot.status().name());
    entity.setVersion((int) league.getVersion());

    for (int i = 0; i < snapshot.participants().size(); i++) {
      entity.addParticipant(
          new LeagueParticipantJpaEntity(entity, snapshot.participants().get(i).value(), i));
    }

    for (final var f : snapshot.fixtures()) {
      final var fe = new LeagueFixtureJpaEntity();
      fe.setId(f.id().value());
      fe.setMatchdayNumber(f.matchdayNumber());
      fe.setPlayerOne(f.playerOne() != null ? f.playerOne().value() : null);
      fe.setPlayerTwo(f.playerTwo() != null ? f.playerTwo().value() : null);
      fe.setMatchId(f.matchId() != null ? f.matchId().value() : null);
      fe.setWinner(f.winner() != null ? f.winner().value() : null);
      fe.setStatus(f.status().name());
      entity.addFixture(fe);
    }

    for (final var entry : snapshot.winsByPlayer().entrySet()) {
      entity.addWin(new LeagueWinJpaEntity(entity, entry.getKey().value(), entry.getValue()));
    }

    return entity;
  }

  public League toDomain(final LeagueJpaEntity entity) {

    final var participants = entity.getParticipants().stream()
        .map(p -> new PlayerId(p.getPlayerId())).toList();

    final var fixtures = entity.getFixtures().stream().map(
        f -> new FixtureSnapshot(new FixtureId(f.getId()), f.getMatchdayNumber(),
            f.getPlayerOne() != null ? new PlayerId(f.getPlayerOne()) : null,
            f.getPlayerTwo() != null ? new PlayerId(f.getPlayerTwo()) : null,
            f.getMatchId() != null ? new MatchId(f.getMatchId()) : null,
            f.getWinner() != null ? new PlayerId(f.getWinner()) : null,
            FixtureStatus.valueOf(f.getStatus()))).toList();

    final Map<PlayerId, Integer> winsByPlayer = new LinkedHashMap<>();
    for (final var w : entity.getWins()) {
      winsByPlayer.put(new PlayerId(w.getPlayerId()), w.getWins());
    }

    final var snapshot = new LeagueSnapshot(new LeagueId(entity.getId()),
        new ArrayList<>(participants), fixtures, winsByPlayer, entity.getNumberOfPlayers(),
        GamesToPlay.of(entity.getGamesToPlay()), InviteCode.of(entity.getInviteCode()),
        LeagueStatus.valueOf(entity.getStatus()));

    final var league = LeagueRehydrator.rehydrate(snapshot);
    league.setVersion(entity.getVersion());
    return league;
  }

}
