package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.gameplay.valueobjects.ActorSeat;
import com.villo.truco.domain.model.gameplay.valueobjects.ActorType;
import com.villo.truco.domain.model.gameplay.valueobjects.DecisionContext;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedAction;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedActionType;
import com.villo.truco.domain.model.gameplay.valueobjects.RecordedDecision;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchPlayerDecisionView;
import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.persistence.entities.MatchActionLogJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchActionLogRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("JpaGameplayRecorderAdapter")
class JpaGameplayRecorderAdapterTest {

  private final SpringDataMatchActionLogRepository repository = mock(
      SpringDataMatchActionLogRepository.class);

  private final JpaGameplayRecorderAdapter adapter = new JpaGameplayRecorderAdapter(
      this.repository);

  private MatchSnapshot snapshot() {

    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true), Visibility.PRIVATE);
    return MatchSnapshotExtractor.extract(match);
  }

  private RecordedDecision decision(final MatchSnapshot snapshot, final RecordedAction action) {

    final var context = new DecisionContext(0, 0, 0, 0, 0, 0, 0, 0, false, null, null, false,
        List.of(), false, false, false, MatchPlayerDecisionView.empty(0, 0));
    return new RecordedDecision(snapshot.id(), snapshot.stateVersion(), snapshot.gameNumber(),
        snapshot.roundNumber(), ActorSeat.PLAYER_ONE, ActorType.BOT, action, snapshot, snapshot,
        context, Instant.now(), 2);
  }

  @Test
  @DisplayName("persiste la decisión mapeando estado y acción a JSONB")
  void persistsDecision() {

    final var snapshot = this.snapshot();
    final var action = new RecordedAction(RecordedActionType.PLAY_CARD, Card.of(Suit.ESPADA, 7));
    when(this.repository.existsByMatchIdAndStateVersion(snapshot.id().value(),
        snapshot.stateVersion())).thenReturn(false);

    this.adapter.record(this.decision(snapshot, action));

    final var captor = ArgumentCaptor.forClass(MatchActionLogJpaEntity.class);
    verify(this.repository).save(captor.capture());
    final var entity = captor.getValue();
    assertThat(entity.getMatchId()).isEqualTo(snapshot.id().value());
    assertThat(entity.getStateVersion()).isEqualTo(snapshot.stateVersion());
    assertThat(entity.getActorType()).isEqualTo("BOT");
    assertThat(entity.getActorSeat()).isEqualTo("PLAYER_ONE");
    assertThat(entity.getActionType()).isEqualTo("PLAY_CARD");
    assertThat(entity.getActionDetail()).isNotNull();
    assertThat(entity.getMatchStateBefore()).isNotNull();
    assertThat(entity.getMatchStateAfter()).isNotNull();
    assertThat(entity.getDecisionContext()).isNotNull();
    assertThat(entity.getSchemaVersion()).isEqualTo(2);
    assertThat(entity.getOccurredAt()).isNotNull();
  }

  @Test
  @DisplayName("no persiste detalle para acciones sin parámetro")
  void persistsNullDetailForParameterlessActions() {

    final var snapshot = this.snapshot();
    final var action = new RecordedAction(RecordedActionType.FOLD, null);
    when(this.repository.existsByMatchIdAndStateVersion(snapshot.id().value(),
        snapshot.stateVersion())).thenReturn(false);

    this.adapter.record(this.decision(snapshot, action));

    final var captor = ArgumentCaptor.forClass(MatchActionLogJpaEntity.class);
    verify(this.repository).save(captor.capture());
    assertThat(captor.getValue().getActionDetail()).isNull();
  }

  @Test
  @DisplayName("es idempotente: no duplica ante misma (matchId, stateVersion)")
  void isIdempotent() {

    final var snapshot = this.snapshot();
    final var action = new RecordedAction(RecordedActionType.FOLD, null);
    when(this.repository.existsByMatchIdAndStateVersion(eq(snapshot.id().value()),
        eq(snapshot.stateVersion()))).thenReturn(true);

    this.adapter.record(this.decision(snapshot, action));

    verify(this.repository, never()).save(any());
  }

}
