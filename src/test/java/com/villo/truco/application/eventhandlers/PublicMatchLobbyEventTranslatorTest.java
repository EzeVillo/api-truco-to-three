package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.assemblers.PublicMatchLobbyDTOAssembler;
import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.PublicMatchLobbyNotification;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
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

@DisplayName("PublicMatchLobbyEventTranslator")
class PublicMatchLobbyEventTranslatorTest {

  @Test
  @DisplayName("creacion publica emite un unico UPSERT global")
  void publicCreationPublishesGlobalUpsert() {

    final var creator = PlayerId.generate();
    final var match = Match.create(creator, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PUBLIC);
    final var queryRepository = repositoryReturning(match);
    final var published = new ArrayList<ApplicationEvent>();

    final var translator = new PublicMatchLobbyEventTranslator(queryRepository,
        new PublicMatchLobbyDTOAssembler(TestPublicActorResolver.guestStyle()), published::add);

    translator.handle(new PublicMatchLobbyOpenedEvent(match.getId(), creator));

    assertThat(published).singleElement().isInstanceOf(PublicMatchLobbyNotification.class);
    final var notification = (PublicMatchLobbyNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("PUBLIC_MATCH_LOBBY_UPSERT");
    assertThat(notification.payload()).containsKey("lobby");
  }

  @Test
  @DisplayName("cuando el lobby deja de ser visible emite REMOVED")
  void emitsRemovedWhenLobbyStopsBeingVisible() {

    final var creator = PlayerId.generate();
    final var joiner = PlayerId.generate();
    final var match = Match.create(creator, MatchRules.fromGamesToPlay(GamesToPlay.of(3)),
        Visibility.PUBLIC);
    match.joinPublic(joiner);

    final var queryRepository = repositoryReturning(match);
    final var published = new ArrayList<ApplicationEvent>();

    final var translator = new PublicMatchLobbyEventTranslator(queryRepository,
        new PublicMatchLobbyDTOAssembler(TestPublicActorResolver.guestStyle()), published::add);

    translator.handle(new PlayerJoinedEvent(match.getId(), creator, joiner));

    assertThat(published).singleElement().isInstanceOf(PublicMatchLobbyNotification.class);
    final var notification = (PublicMatchLobbyNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("PUBLIC_MATCH_LOBBY_REMOVED");
    assertThat(notification.payload()).containsEntry("id", match.getId().value().toString());
  }

  private MatchQueryRepository repositoryReturning(final Match match) {

    return new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.of(match);
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public boolean hasUnfinishedMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }

      @Override
      public List<Match> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
  }

}
