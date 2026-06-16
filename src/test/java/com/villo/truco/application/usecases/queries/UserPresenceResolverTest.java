package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserPresenceResolver")
class UserPresenceResolverTest {

  private MatchQueryRepository matchQueryRepository;
  private LeagueQueryRepository leagueQueryRepository;
  private CupQueryRepository cupQueryRepository;
  private RematchSessionRepository rematchSessionRepository;
  private QuickMatchQueuePort quickMatchQueuePort;
  private SpectatorshipRepository spectatorshipRepository;
  private com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry botVsBotMatchRegistry;
  private UserPresenceResolver resolver;

  private static Match unfinishedMatch(final PlayerId player) {

    return Match.createReady(player, PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
  }

  private static League waitingLeague(final PlayerId player) {

    return League.create(player, 3, GamesToPlay.of(3), Visibility.PRIVATE);
  }

  private static League inProgressLeague(final PlayerId player) {

    final var league = League.create(player, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(PlayerId.generate());
    league.join(PlayerId.generate());
    league.start(player);
    return league;
  }

  private static Cup waitingCup(final PlayerId player) {

    return Cup.create(player, 4, GamesToPlay.of(3), Visibility.PRIVATE);
  }

  @BeforeEach
  void setUp() {

    matchQueryRepository = mock(MatchQueryRepository.class);
    leagueQueryRepository = mock(LeagueQueryRepository.class);
    cupQueryRepository = mock(CupQueryRepository.class);
    rematchSessionRepository = mock(RematchSessionRepository.class);
    quickMatchQueuePort = mock(QuickMatchQueuePort.class);
    spectatorshipRepository = mock(SpectatorshipRepository.class);
    botVsBotMatchRegistry = new com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry();
    resolver = new UserPresenceResolver(matchQueryRepository, leagueQueryRepository,
        cupQueryRepository, rematchSessionRepository, quickMatchQueuePort, spectatorshipRepository,
        botVsBotMatchRegistry);

    when(matchQueryRepository.findById(any())).thenReturn(Optional.empty());
    when(matchQueryRepository.findUnfinishedByPlayer(any())).thenReturn(Optional.empty());
    when(leagueQueryRepository.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(leagueQueryRepository.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    when(cupQueryRepository.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(cupQueryRepository.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    when(rematchSessionRepository.findOpenByPlayer(any())).thenReturn(Optional.empty());
    when(quickMatchQueuePort.findByPlayer(any())).thenReturn(Optional.empty());
  }

  @Test
  @DisplayName("usuario libre devuelve busy false y todas las referencias nulas")
  void freeUserReturnsNotBusy() {

    final var presence = resolver.resolve(PlayerId.generate());

    assertThat(presence.busy()).isFalse();
    assertThat(presence.match()).isNull();
    assertThat(presence.league()).isNull();
    assertThat(presence.cup()).isNull();
    assertThat(presence.rematch()).isNull();
    assertThat(presence.quickMatch()).isNull();
  }

  @Test
  @DisplayName("con partida no finalizada expone match ref con id y estado y busy true")
  void exposesMatchReference() {

    final var player = PlayerId.generate();
    final var match = unfinishedMatch(player);
    when(matchQueryRepository.findUnfinishedByPlayer(player)).thenReturn(Optional.of(match));

    final var presence = resolver.resolve(player);

    assertThat(presence.busy()).isTrue();
    assertThat(presence.match()).isNotNull();
    assertThat(presence.match().id()).isEqualTo(match.getId().value().toString());
    assertThat(presence.match().status()).isEqualTo(match.getStatus().name());
  }

  @Test
  @DisplayName("liga en espera expone league ref con currentMatchId nulo")
  void waitingLeagueHasNoCurrentMatch() {

    final var player = PlayerId.generate();
    final var league = waitingLeague(player);
    when(leagueQueryRepository.findWaitingByPlayer(player)).thenReturn(Optional.of(league));

    final var presence = resolver.resolve(player);

    assertThat(presence.league()).isNotNull();
    assertThat(presence.league().id()).isEqualTo(league.getId().value().toString());
    assertThat(presence.league().status()).isEqualTo(league.getStatus().name());
    assertThat(presence.league().currentMatchId()).isNull();
  }

  @Test
  @DisplayName("liga en progreso con partida del torneo expone currentMatchId igual a match.id")
  void inProgressLeagueExposesCurrentMatch() {

    final var player = PlayerId.generate();
    final var match = unfinishedMatch(player);
    final var league = inProgressLeague(player);
    when(matchQueryRepository.findUnfinishedByPlayer(player)).thenReturn(Optional.of(match));
    when(leagueQueryRepository.findInProgressByPlayer(player)).thenReturn(Optional.of(league));

    final var presence = resolver.resolve(player);

    assertThat(presence.league().currentMatchId()).isEqualTo(match.getId().value().toString());
    assertThat(presence.match().id()).isEqualTo(presence.league().currentMatchId());
  }

  @Test
  @DisplayName("copa en espera expone cup ref con currentMatchId nulo")
  void waitingCupHasNoCurrentMatch() {

    final var player = PlayerId.generate();
    final var cup = waitingCup(player);
    when(cupQueryRepository.findWaitingByPlayer(player)).thenReturn(Optional.of(cup));

    final var presence = resolver.resolve(player);

    assertThat(presence.cup()).isNotNull();
    assertThat(presence.cup().id()).isEqualTo(cup.getId().value().toString());
    assertThat(presence.cup().status()).isEqualTo(cup.getStatus().name());
    assertThat(presence.cup().currentMatchId()).isNull();
  }

  @Test
  @DisplayName("con sesion de revancha abierta expone rematch ref con id y partida de origen")
  void exposesRematchReference() {

    final var player = PlayerId.generate();
    final var originMatchId = MatchId.generate();
    final var session = RematchSession.open(originMatchId, player, PlayerId.generate(), 3, false,
        false, Instant.now(), Duration.ofMinutes(2));
    when(rematchSessionRepository.findOpenByPlayer(player)).thenReturn(Optional.of(session));

    final var presence = resolver.resolve(player);

    assertThat(presence.rematch()).isNotNull();
    assertThat(presence.rematch().id()).isEqualTo(session.getId().value().toString());
    assertThat(presence.rematch().originMatchId()).isEqualTo(originMatchId.value().toString());
  }

  @Test
  @DisplayName("con busqueda Quick Match expone quickMatch ref y busy true")
  void exposesQuickMatchReference() {

    final var player = PlayerId.generate();
    final var enqueuedAt = Instant.parse("2026-05-20T10:00:00Z");
    final var ticket = new QuickMatchTicket(player, GamesToPlay.of(3), enqueuedAt, "ws-1");
    when(quickMatchQueuePort.findByPlayer(player)).thenReturn(Optional.of(ticket));

    final var presence = resolver.resolve(player);

    assertThat(presence.busy()).isTrue();
    assertThat(presence.quickMatch()).isNotNull();
    assertThat(presence.quickMatch().status()).isEqualTo("SEARCHING");
    assertThat(presence.quickMatch().enqueuedAt()).isEqualTo(enqueuedAt);
  }

  @Test
  @DisplayName("con bot-match propia activa expone ownedBotMatch con id y estado y busy true")
  void exposesOwnedBotMatchReference() {

    final var owner = PlayerId.generate();
    final var match = unfinishedMatch(owner);
    botVsBotMatchRegistry.register(match.getId(), owner);
    when(matchQueryRepository.findById(match.getId())).thenReturn(Optional.of(match));

    final var presence = resolver.resolve(owner);

    assertThat(presence.busy()).isTrue();
    assertThat(presence.ownedBotMatch()).isNotNull();
    assertThat(presence.ownedBotMatch().matchId()).isEqualTo(match.getId().value().toString());
    assertThat(presence.ownedBotMatch().status()).isEqualTo(match.getStatus().name());
    assertThat(presence.match()).isNull();
    assertThat(presence.spectating()).isNull();
  }

  @Test
  @DisplayName("sin bot-match propia activa ownedBotMatch es null")
  void noOwnedBotMatchWhenAbsent() {

    final var presence = resolver.resolve(PlayerId.generate());

    assertThat(presence.ownedBotMatch()).isNull();
  }

}
