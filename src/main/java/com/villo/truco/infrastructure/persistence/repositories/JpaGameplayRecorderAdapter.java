package com.villo.truco.infrastructure.persistence.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedDecision;
import com.villo.truco.domain.ports.GameplayRecorderPort;
import com.villo.truco.infrastructure.persistence.entities.MatchActionLogJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchActionLogRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaGameplayRecorderAdapter implements GameplayRecorderPort {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  private final SpringDataMatchActionLogRepository repository;

  public JpaGameplayRecorderAdapter(final SpringDataMatchActionLogRepository repository) {

    this.repository = Objects.requireNonNull(repository);
  }

  @Override
  @Transactional
  public void record(final RecordedDecision decision) {

    final UUID matchId = decision.matchId().value();

    if (this.repository.existsByMatchIdAndStateVersion(matchId, decision.stateVersion())) {
      return;
    }

    final var entity = new MatchActionLogJpaEntity();
    entity.setMatchId(matchId);
    entity.setStateVersion(decision.stateVersion());
    entity.setGameNumber(decision.gameNumber());
    entity.setRoundNumber(decision.roundNumber());
    entity.setActorSeat(decision.actorSeat().name());
    entity.setActorType(decision.actorType().name());
    entity.setActionType(decision.action().type().name());
    entity.setActionDetail(this.toJson(decision.action().detail()));
    entity.setMatchState(OBJECT_MAPPER.valueToTree(decision.snapshot()));
    entity.setSchemaVersion(decision.schemaVersion());
    entity.setOccurredAt(decision.occurredAt());

    this.repository.save(entity);
  }

  private JsonNode toJson(final Object detail) {

    return detail == null ? null : OBJECT_MAPPER.valueToTree(detail);
  }

}
