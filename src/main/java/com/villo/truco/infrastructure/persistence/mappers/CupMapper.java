package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.CupRehydrator;
import com.villo.truco.domain.model.cup.CupSnapshot;
import com.villo.truco.domain.model.cup.CupSnapshotExtractor;
import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.CupBoutJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.CupForfeitedPlayerJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.CupJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.CupParticipantJpaEntity;
import java.util.ArrayList;
import java.util.HashSet;
import org.springframework.stereotype.Component;

@Component
public class CupMapper {

  public CupJpaEntity toEntity(final Cup cup) {

    final var snapshot = CupSnapshotExtractor.extract(cup);
    final var entity = new CupJpaEntity();
    final var cupId = snapshot.id().value();

    entity.setId(cupId);
    entity.setNumberOfPlayers(snapshot.numberOfPlayers());
    entity.setGamesToPlay(snapshot.gamesToPlay().value());
    entity.setInviteCode(snapshot.inviteCode().value());
    entity.setStatus(snapshot.status().name());
    entity.setChampion(snapshot.champion() != null ? snapshot.champion().value() : null);
    entity.setVersion((int) cup.getVersion());

    final var participants = new ArrayList<CupParticipantJpaEntity>();
    for (int i = 0; i < snapshot.participants().size(); i++) {
      participants.add(
          new CupParticipantJpaEntity(cupId, snapshot.participants().get(i).value(), i));
    }
    entity.setParticipants(participants);

    final var bouts = new ArrayList<CupBoutJpaEntity>();
    for (final var b : snapshot.bouts()) {
      final var be = new CupBoutJpaEntity();
      be.setId(b.id().value());
      be.setCupId(cupId);
      be.setRoundNumber(b.roundNumber());
      be.setBracketPosition(b.bracketPosition());
      be.setPlayerOne(b.playerOne() != null ? b.playerOne().value() : null);
      be.setPlayerTwo(b.playerTwo() != null ? b.playerTwo().value() : null);
      be.setMatchId(b.matchId() != null ? b.matchId().value() : null);
      be.setWinner(b.winner() != null ? b.winner().value() : null);
      be.setStatus(b.status().name());
      bouts.add(be);
    }
    entity.setBouts(bouts);

    final var forfeitedPlayers = new ArrayList<CupForfeitedPlayerJpaEntity>();
    for (final var fp : snapshot.forfeitedPlayers()) {
      forfeitedPlayers.add(new CupForfeitedPlayerJpaEntity(cupId, fp.value()));
    }
    entity.setForfeitedPlayers(forfeitedPlayers);

    return entity;
  }

  public Cup toDomain(final CupJpaEntity entity) {

    final var participants = entity.getParticipants().stream()
        .map(p -> new PlayerId(p.getPlayerId())).toList();

    final var bouts = entity.getBouts().stream().map(
        b -> new CupSnapshot.BoutData(new BoutId(b.getId()), b.getRoundNumber(),
            b.getBracketPosition(),
            b.getPlayerOne() != null ? new PlayerId(b.getPlayerOne()) : null,
            b.getPlayerTwo() != null ? new PlayerId(b.getPlayerTwo()) : null,
            b.getMatchId() != null ? new MatchId(b.getMatchId()) : null,
            b.getWinner() != null ? new PlayerId(b.getWinner()) : null,
            BoutStatus.valueOf(b.getStatus()))).toList();

    final var forfeitedPlayers = new HashSet<PlayerId>();
    for (final var fp : entity.getForfeitedPlayers()) {
      forfeitedPlayers.add(new PlayerId(fp.getPlayerId()));
    }

    final var champion = entity.getChampion() != null ? new PlayerId(entity.getChampion()) : null;

    final var snapshot = new CupSnapshot.CupData(new CupId(entity.getId()),
        new ArrayList<>(participants), bouts, forfeitedPlayers, entity.getNumberOfPlayers(),
        GamesToPlay.of(entity.getGamesToPlay()), InviteCode.of(entity.getInviteCode()),
        CupStatus.valueOf(entity.getStatus()), champion);

    final var cup = CupRehydrator.rehydrate(snapshot);
    cup.setVersion(entity.getVersion());
    return cup;
  }

}
