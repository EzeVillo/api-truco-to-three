package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.TournamentRehydrator;
import com.villo.truco.domain.model.tournament.TournamentSnapshot;
import com.villo.truco.domain.model.tournament.TournamentSnapshotExtractor;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureId;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.TournamentFixtureJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.TournamentJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.TournamentParticipantJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.TournamentWinJpaEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

  public TournamentJpaEntity toEntity(final Tournament tournament) {

    final var snapshot = TournamentSnapshotExtractor.extract(tournament);
    final var entity = new TournamentJpaEntity();
    final var tournamentId = snapshot.id().value();

    entity.setId(tournamentId);
    entity.setNumberOfPlayers(snapshot.numberOfPlayers());
    entity.setGamesToPlay(snapshot.gamesToPlay().value());
    entity.setInviteCode(snapshot.inviteCode().value());
    entity.setStatus(snapshot.status().name());
    entity.setVersion((int) tournament.getVersion());

    final var participants = new ArrayList<TournamentParticipantJpaEntity>();
    for (int i = 0; i < snapshot.participants().size(); i++) {
      participants.add(
          new TournamentParticipantJpaEntity(tournamentId, snapshot.participants().get(i).value(),
              i));
    }
    entity.setParticipants(participants);

    final var fixtures = new ArrayList<TournamentFixtureJpaEntity>();
    for (final var f : snapshot.fixtures()) {
      final var fe = new TournamentFixtureJpaEntity();
      fe.setId(f.id().value());
      fe.setTournamentId(tournamentId);
      fe.setMatchdayNumber(f.matchdayNumber());
      fe.setPlayerOne(f.playerOne() != null ? f.playerOne().value() : null);
      fe.setPlayerTwo(f.playerTwo() != null ? f.playerTwo().value() : null);
      fe.setMatchId(f.matchId() != null ? f.matchId().value() : null);
      fe.setWinner(f.winner() != null ? f.winner().value() : null);
      fe.setStatus(f.status().name());
      fixtures.add(fe);
    }
    entity.setFixtures(fixtures);

    final var wins = new ArrayList<TournamentWinJpaEntity>();
    for (final var entry : snapshot.winsByPlayer().entrySet()) {
      wins.add(new TournamentWinJpaEntity(tournamentId, entry.getKey().value(), entry.getValue()));
    }
    entity.setWins(wins);

    return entity;
  }

  public Tournament toDomain(final TournamentJpaEntity entity) {

    final var participants = entity.getParticipants().stream()
        .map(p -> new PlayerId(p.getPlayerId())).toList();

    final var fixtures = entity.getFixtures().stream().map(
        f -> new TournamentSnapshot.FixtureData(new FixtureId(f.getId()), f.getMatchdayNumber(),
            f.getPlayerOne() != null ? new PlayerId(f.getPlayerOne()) : null,
            f.getPlayerTwo() != null ? new PlayerId(f.getPlayerTwo()) : null,
            f.getMatchId() != null ? new MatchId(f.getMatchId()) : null,
            f.getWinner() != null ? new PlayerId(f.getWinner()) : null,
            FixtureStatus.valueOf(f.getStatus()))).toList();

    final Map<PlayerId, Integer> winsByPlayer = new LinkedHashMap<>();
    for (final var w : entity.getWins()) {
      winsByPlayer.put(new PlayerId(w.getPlayerId()), w.getWins());
    }

    final var snapshot = new TournamentSnapshot.TournamentData(new TournamentId(entity.getId()),
        new ArrayList<>(participants), fixtures, winsByPlayer, entity.getNumberOfPlayers(),
        GamesToPlay.of(entity.getGamesToPlay()), InviteCode.of(entity.getInviteCode()),
        TournamentStatus.valueOf(entity.getStatus()));

    final var tournament = TournamentRehydrator.rehydrate(snapshot);
    tournament.setVersion(entity.getVersion());
    return tournament;
  }

}
