package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.assemblers.PublicLeagueLobbyDTOAssembler;
import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.PublicLeagueLobbyNotification;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.LeagueQueryRepository;
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

@DisplayName("PublicLeagueLobbyEventTranslator")
class PublicLeagueLobbyEventTranslatorTest {

  @Test
  @DisplayName("join parcial mantiene el lobby visible y emite UPSERT")
  void partialJoinPublishesUpsert() {

    final var creator = PlayerId.generate();
    final var joiner = PlayerId.generate();
    final var league = League.create(creator, 4, GamesToPlay.of(3), Visibility.PUBLIC);
    league.join(joiner);

    final var queryRepository = repositoryReturning(league);
    final var published = new ArrayList<ApplicationEvent>();

    final var translator = new PublicLeagueLobbyEventTranslator(queryRepository,
        new PublicLeagueLobbyDTOAssembler(TestPublicActorResolver.guestStyle()), published::add);

    translator.handle(
        new LeaguePlayerJoinedEvent(league.getId(), List.copyOf(league.getParticipants()), joiner));

    assertThat(published).singleElement().isInstanceOf(PublicLeagueLobbyNotification.class);
    final var notification = (PublicLeagueLobbyNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("PUBLIC_LEAGUE_LOBBY_UPSERT");
    assertThat(((com.villo.truco.application.dto.PublicLeagueLobbyDTO) notification.payload()
        .get("lobby")).occupiedSlots()).isEqualTo(2);
  }

  @Test
  @DisplayName("inicio de liga publica emite REMOVED")
  void startedLeaguePublishesRemoved() {

    final var creator = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(creator, 3, GamesToPlay.of(3), Visibility.PUBLIC);
    league.join(p2);
    league.join(p3);

    final var queryRepository = repositoryReturning(league);
    final var published = new ArrayList<ApplicationEvent>();

    final var translator = new PublicLeagueLobbyEventTranslator(queryRepository,
        new PublicLeagueLobbyDTOAssembler(TestPublicActorResolver.guestStyle()), published::add);

    translator.handle(
        new LeagueStartedEvent(league.getId(), List.copyOf(league.getParticipants())));

    assertThat(published).singleElement().isInstanceOf(PublicLeagueLobbyNotification.class);
    final var notification = (PublicLeagueLobbyNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("PUBLIC_LEAGUE_LOBBY_REMOVED");
    assertThat(notification.payload()).containsEntry("id", league.getId().value().toString());
  }

  private LeagueQueryRepository repositoryReturning(final League league) {

    return new LeagueQueryRepository() {
      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.of(league);
      }

      @Override
      public Optional<League> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

        return List.of();
      }

      @Override
      public List<League> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<League> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
  }

}
