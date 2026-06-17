package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.AbandonBotVsBotMatchCommand;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.exceptions.AbandonBotMatchNotOwnerException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchLockingRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.MatchTimeoutEntry;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbandonBotVsBotMatchCommandHandler")
class AbandonBotVsBotMatchCommandHandlerTest {

  private PlayerId owner;
  private PlayerId botOne;
  private PlayerId botTwo;
  private Match match;
  private InMemoryBotVsBotMatchRegistry registry;
  private AtomicReference<Match> savedMatch;
  private List<MatchDomainEvent> publishedEvents;

  @BeforeEach
  void setUp() {

    this.owner = PlayerId.generate();
    this.botOne = PlayerId.generate();
    this.botTwo = PlayerId.generate();
    this.match = Match.createReady(this.botOne, this.botTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), false));
    this.match.startMatch(this.botOne);
    this.match.startMatch(this.botTwo);
    this.match.clearDomainEvents();
    this.registry = new InMemoryBotVsBotMatchRegistry();
    this.registry.register(this.match.getId(), this.owner);
    this.savedMatch = new AtomicReference<>();
    this.publishedEvents = new ArrayList<>();
  }

  private AbandonBotVsBotMatchCommandHandler handler() {

    final MatchLockingRepository lockingRepo = matchId -> Optional.of(this.match);
    final MatchRepository matchRepository = new MatchRepository() {
      @Override
      public void save(final Match saved) {

        savedMatch.set(saved);
      }

      @Override
      public Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
    final MatchEventNotifier notifier = this.publishedEvents::addAll;
    return new AbandonBotVsBotMatchCommandHandler(this.registry, lockingRepo, matchRepository,
        notifier);
  }

  private AbandonBotVsBotMatchCommand commandFrom(final PlayerId requester) {

    return new AbandonBotVsBotMatchCommand(this.match.getId().value().toString(),
        requester.value().toString());
  }

  @Test
  @DisplayName("el dueño abandona → serie FINISHED y el otro bot gana administrativamente")
  void ownerAbandonsFinishesMatch() {

    this.handler().handle(commandFrom(this.owner));

    assertThat(this.savedMatch.get()).isNotNull();
    assertThat(this.savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(this.savedMatch.get().getMatchWinner()).isEqualTo(this.botTwo);
  }

  @Test
  @DisplayName("publica MatchAbandonedEvent y no MatchForfeitedEvent")
  void publishesAbandonedNotForfeited() {

    this.handler().handle(commandFrom(this.owner));

    assertThat(this.publishedEvents).anyMatch(e -> e instanceof MatchAbandonedEvent);
    assertThat(this.publishedEvents).noneMatch(e -> e instanceof MatchForfeitedEvent);
  }

  @Test
  @DisplayName("limpia los eventos del aggregate tras publicar")
  void clearsDomainEvents() {

    this.handler().handle(commandFrom(this.owner));

    assertThat(this.match.getDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("un usuario que no es el creador es rechazado y no toca el match")
  void nonOwnerRejected() {

    final var stranger = PlayerId.generate();

    assertThatThrownBy(() -> this.handler().handle(commandFrom(stranger))).isInstanceOf(
        AbandonBotMatchNotOwnerException.class);

    assertThat(this.savedMatch.get()).isNull();
    assertThat(this.publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("si el match no está registrado como bot-vs-bot, se rechaza por no ser el dueño")
  void unknownBotVsBotMatchRejected() {

    final var unregistered = new InMemoryBotVsBotMatchRegistry();
    final MatchLockingRepository lockingRepo = matchId -> Optional.of(this.match);
    final MatchRepository matchRepository = new MatchRepository() {
      @Override
      public void save(final Match saved) {

        savedMatch.set(saved);
      }

      @Override
      public Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
    final var handler = new AbandonBotVsBotMatchCommandHandler(unregistered, lockingRepo,
        matchRepository, this.publishedEvents::addAll);

    assertThatThrownBy(() -> handler.handle(commandFrom(this.owner))).isInstanceOf(
        AbandonBotMatchNotOwnerException.class);
  }

  @Test
  @DisplayName("si el dueño es correcto pero el match no existe, lanza MatchNotFoundException")
  void ownerOkButMatchMissing() {

    final MatchLockingRepository lockingRepo = matchId -> Optional.empty();
    final MatchRepository matchRepository = new MatchRepository() {
      @Override
      public void save(final Match saved) {

        savedMatch.set(saved);
      }

      @Override
      public Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
    final var handler = new AbandonBotVsBotMatchCommandHandler(this.registry, lockingRepo,
        matchRepository, this.publishedEvents::addAll);

    assertThatThrownBy(() -> handler.handle(commandFrom(this.owner))).isInstanceOf(
        MatchNotFoundException.class);
  }

  @Test
  @DisplayName("idempotente: si los bots ya terminaron la serie, no publica MatchAbandonedEvent")
  void idempotentWhenAlreadyFinished() {

    this.match.abandon(this.botOne);
    this.match.clearDomainEvents();
    final var matchId = new AtomicReference<>(this.match.getId());

    this.handler().handle(new AbandonBotVsBotMatchCommand(matchId.get().value().toString(),
        this.owner.value().toString()));

    assertThat(this.publishedEvents).noneMatch(e -> e instanceof MatchAbandonedEvent);
  }

  @Test
  @DisplayName("el comando rechaza matchId nulo")
  void rejectsNullMatchId() {

    assertThatThrownBy(() -> new AbandonBotVsBotMatchCommand(null, this.owner)).isInstanceOf(
        NullPointerException.class);
  }

}
