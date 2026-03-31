package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.MatchAbandoned;
import com.villo.truco.application.events.MatchCompleted;
import com.villo.truco.application.events.MatchForfeited;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompetitionDomainEventTranslator")
class CompetitionDomainEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;
  private final CompetitionDomainEventTranslator translator = new CompetitionDomainEventTranslator(
      publisher);

  @Test
  @DisplayName("MatchFinishedEvent → publica MatchCompleted con el winner correcto")
  void matchFinishedPublishesMatchCompleted() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var event = new MatchFinishedEvent(matchId, p1, p2, PlayerSeat.PLAYER_TWO, 1, 3);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var completed = (MatchCompleted) published.get(0);
    assertThat(completed.matchId()).isEqualTo(matchId);
    assertThat(completed.winnerId()).isEqualTo(p2);
  }

  @Test
  @DisplayName("MatchForfeitedEvent → publica MatchForfeited con winner y loser correctos")
  void matchForfeitedPublishesMatchForfeited() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var event = new MatchForfeitedEvent(matchId, p1, p2, PlayerSeat.PLAYER_ONE, 2, 0);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var forfeited = (MatchForfeited) published.get(0);
    assertThat(forfeited.matchId()).isEqualTo(matchId);
    assertThat(forfeited.winnerId()).isEqualTo(p1);
    assertThat(forfeited.loserId()).isEqualTo(p2);
  }

  @Test
  @DisplayName("MatchAbandonedEvent publica MatchAbandoned")
  void matchAbandonedPublishesCompletedAndAbandoned() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var event = new MatchAbandonedEvent(matchId, p1, p2, PlayerSeat.PLAYER_TWO,
        PlayerSeat.PLAYER_ONE, 0, 3);

    translator.handle(event);

    assertThat(published).hasSize(1);
    assertThat(published.getFirst()).isEqualTo(new MatchAbandoned(matchId, p2, p1));
  }

  @Test
  @DisplayName("otro evento → no publica nada")
  void otherEventPublishesNothing() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();

    translator.handle(new PlayerJoinedEvent(matchId, p1, p2));

    assertThat(published).isEmpty();
  }

}
