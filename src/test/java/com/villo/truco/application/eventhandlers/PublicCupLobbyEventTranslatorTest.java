package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.assemblers.PublicCupLobbyDTOAssembler;
import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.PublicCupLobbyNotification;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.support.TestPublicActorResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PublicCupLobbyEventTranslator")
class PublicCupLobbyEventTranslatorTest {

  @Test
  @DisplayName("join parcial mantiene el lobby visible y emite UPSERT")
  void partialJoinPublishesUpsert() {

    final var creator = PlayerId.generate();
    final var joiner = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);
    cup.join(joiner);

    final var queryRepository = repositoryReturning(cup);
    final var published = new ArrayList<ApplicationEvent>();

    final var translator = new PublicCupLobbyEventTranslator(queryRepository,
        new PublicCupLobbyDTOAssembler(TestPublicActorResolver.guestStyle()), published::add);

    translator.handle(
        new CupPlayerJoinedEvent(cup.getId(), List.copyOf(cup.getParticipants()), joiner));

    assertThat(published).singleElement().isInstanceOf(PublicCupLobbyNotification.class);
    final var notification = (PublicCupLobbyNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("PUBLIC_CUP_LOBBY_UPSERT");
    assertThat(((com.villo.truco.application.dto.PublicCupLobbyDTO) notification.payload()
        .get("lobby")).occupiedSlots()).isEqualTo(2);
  }

  @Test
  @DisplayName("inicio de copa publica emite REMOVED")
  void startedCupPublishesRemoved() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);
    cup.join(p2);
    cup.join(p3);
    cup.join(p4);

    final var queryRepository = repositoryReturning(cup);
    final var published = new ArrayList<ApplicationEvent>();

    final var translator = new PublicCupLobbyEventTranslator(queryRepository,
        new PublicCupLobbyDTOAssembler(TestPublicActorResolver.guestStyle()), published::add);

    translator.handle(new CupStartedEvent(cup.getId(), List.copyOf(cup.getParticipants())));

    assertThat(published).singleElement().isInstanceOf(PublicCupLobbyNotification.class);
    final var notification = (PublicCupLobbyNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("PUBLIC_CUP_LOBBY_REMOVED");
    assertThat(notification.payload()).containsEntry("id", cup.getId().value().toString());
  }

  private CupQueryRepository repositoryReturning(final Cup cup) {

    return new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.of(cup);
      }

      @Override
      public Optional<Cup> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<CupId> findIdleCupIds(final Instant idleSince) {

        return List.of();
      }

      private List<Cup> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
  }

}
