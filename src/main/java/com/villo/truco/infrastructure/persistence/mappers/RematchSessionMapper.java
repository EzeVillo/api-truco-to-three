package com.villo.truco.infrastructure.persistence.mappers;

import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.RematchSessionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RematchSessionMapper {

  public RematchSessionJpaEntity toEntity(final RematchSession session) {

    final var entity = new RematchSessionJpaEntity();
    entity.setId(session.getId().value());
    entity.setOriginMatchId(session.getOriginMatchId().value());
    entity.setPlayerOneId(session.getPlayerOneId().value());
    entity.setPlayerTwoId(session.getPlayerTwoId().value());
    entity.setPlayerOneChoice(session.getPlayerOneChoice().name());
    entity.setPlayerTwoChoice(session.getPlayerTwoChoice().name());
    entity.setPlayerOneIsBot(session.isPlayerOneIsBot());
    entity.setPlayerTwoIsBot(session.isPlayerTwoIsBot());
    entity.setStatus(session.getStatus().name());
    entity.setGamesToWin(session.getGamesToWin());
    entity.setExpiresAt(session.getExpiresAt());
    entity.setResultMatchId(
        session.getResultMatchId() != null ? session.getResultMatchId().value() : null);
    entity.setVersion((int) session.getVersion());
    return entity;
  }

  public RematchSession toDomain(final RematchSessionJpaEntity entity) {

    final var resultMatchId =
        entity.getResultMatchId() != null ? new MatchId(entity.getResultMatchId()) : null;

    final var session = RematchSession.reconstruct(new RematchSessionId(entity.getId()),
        new MatchId(entity.getOriginMatchId()), new PlayerId(entity.getPlayerOneId()),
        new PlayerId(entity.getPlayerTwoId()), entity.isPlayerOneIsBot(), entity.isPlayerTwoIsBot(),
        entity.getGamesToWin(), entity.getExpiresAt(),
        RematchPlayerChoice.valueOf(entity.getPlayerOneChoice()),
        RematchPlayerChoice.valueOf(entity.getPlayerTwoChoice()),
        RematchSessionStatus.valueOf(entity.getStatus()), resultMatchId);
    session.setVersion(entity.getVersion());
    return session;
  }

}
