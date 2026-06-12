package com.villo.truco.campaign.infrastructure.persistence.mappers;

import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.model.CampaignProgressRehydrator;
import com.villo.truco.campaign.domain.model.CampaignProgressSnapshot;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignChallenge;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignPoints;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignRivalRecord;
import com.villo.truco.campaign.infrastructure.persistence.entities.CampaignProgressJpaEntity;
import com.villo.truco.campaign.infrastructure.persistence.entities.CampaignRivalRecordJpaEmbeddable;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CampaignProgressMapper {

  private static CampaignRivalRecordJpaEmbeddable toEmbeddable(final PlayerId rivalId,
      final CampaignRivalRecord record) {

    final var embeddable = new CampaignRivalRecordJpaEmbeddable();
    embeddable.setRivalId(rivalId.value());
    embeddable.setWins(record.wins());
    embeddable.setLosses(record.losses());
    return embeddable;
  }

  public CampaignProgressJpaEntity toEntity(final CampaignProgress progress) {

    final var snapshot = progress.snapshot();
    final var entity = new CampaignProgressJpaEntity();
    entity.setPlayerId(snapshot.playerId().value());
    entity.setPoints(snapshot.points().value());
    if (snapshot.activeChallenge() != null) {
      entity.setActiveChallengeMatchId(snapshot.activeChallenge().matchId().value());
      entity.setActiveChallengeRivalId(snapshot.activeChallenge().rivalId().value());
    }
    entity.setTopOneReached(snapshot.topOneReached());
    entity.setAllRivalsDefeated(snapshot.allRivalsDefeated());
    entity.setRivalRecords(snapshot.rivalRecords().entrySet().stream()
        .map(entry -> toEmbeddable(entry.getKey(), entry.getValue())).toList());
    entity.setVersion((int) progress.getVersion());
    return entity;
  }

  public CampaignProgress toDomain(final CampaignProgressJpaEntity entity) {

    final Map<PlayerId, CampaignRivalRecord> rivalRecords = new LinkedHashMap<>();
    for (final var record : entity.getRivalRecords()) {
      rivalRecords.put(new PlayerId(record.getRivalId()),
          new CampaignRivalRecord(record.getWins(), record.getLosses()));
    }

    final var activeChallenge = entity.getActiveChallengeMatchId() == null ? null
        : new CampaignChallenge(new MatchId(entity.getActiveChallengeMatchId()),
            new PlayerId(entity.getActiveChallengeRivalId()));

    final var snapshot = new CampaignProgressSnapshot(new PlayerId(entity.getPlayerId()),
        new CampaignPoints(entity.getPoints()), activeChallenge, rivalRecords,
        entity.isTopOneReached(), entity.isAllRivalsDefeated());
    final var progress = CampaignProgressRehydrator.rehydrate(snapshot);
    progress.setVersion(entity.getVersion());
    return progress;
  }

}
