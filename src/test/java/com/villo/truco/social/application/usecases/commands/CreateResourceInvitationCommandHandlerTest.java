package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInWaitingCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInWaitingLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.social.application.commands.CreateResourceInvitationCommand;
import com.villo.truco.social.application.exceptions.FriendshipRequiredException;
import com.villo.truco.social.application.exceptions.ResourceInvitationAlreadyExistsException;
import com.villo.truco.social.application.exceptions.SocialUserNotFoundException;
import com.villo.truco.social.application.services.InvitationTargetService;
import com.villo.truco.social.application.services.ResourceInvitationPolicy;
import com.villo.truco.social.application.services.SocialInvitationExpirationPolicy;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateResourceInvitationCommandHandler")
class CreateResourceInvitationCommandHandlerTest {

  @Test
  @DisplayName("resuelve recipientUsername y crea la invitacion")
  void createsInvitationByRecipientUsername() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var saved = new AtomicReference<ResourceInvitation>();
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        saved::set);

    final var dto = handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()));

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getSenderId()).isEqualTo(sender);
    assertThat(saved.get().getRecipientId()).isEqualTo(recipient);
    assertThat(dto.senderUsername()).isEqualTo("juancho");
    assertThat(dto.recipientUsername()).isEqualTo("martina");
    assertThat(dto.status()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("rechaza usernames inexistentes")
  void rejectsUnknownRecipientUsername() {

    final var sender = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(Map.of(sender, "juancho"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of()),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(SocialUserNotFoundException.class);
  }

  @Test
  @DisplayName("mantiene la validacion de amistad aceptada")
  void rejectsWhenFriendshipIsNotAccepted() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of()),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(FriendshipRequiredException.class);
  }

  @Test
  @DisplayName("ignora historial no activo y exige amistad aceptada vigente")
  void rejectsWhenOnlyHistoricalNonActiveFriendshipsExist() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository, new FixedFriendshipQueryRepository(
            List.of(declinedFriendship(sender, recipient), cancelledFriendship(sender, recipient),
                removedFriendship(sender, recipient))),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(FriendshipRequiredException.class);
  }

  @Test
  @DisplayName("mantiene la validacion de invitacion pendiente duplicada")
  void rejectsDuplicatedPendingInvitation() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(true), new FixedMatchQueryRepository(match),
        invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(
        ResourceInvitationAlreadyExistsException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario ya tiene un match sin finalizar")
  void rejectsWhenRecipientHasUnfinishedMatch() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(false),
        new FixedMatchQueryRepository(match, true), new EmptyLeagueQueryRepository(),
        new EmptyCupQueryRepository(), invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario esta ocupado en una liga en progreso")
  void rejectsWhenRecipientIsBusyInLeague() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var thirdPlayer = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var league = League.create(recipient, 3,
        com.villo.truco.domain.shared.valueobjects.GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(sender);
    league.join(thirdPlayer);
    league.start(recipient);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina", thirdPlayer, "pedro"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        new FixedLeagueQueryRepository(Optional.of(league), Optional.empty()),
        new EmptyCupQueryRepository(), invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(PlayerBusyInLeagueException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario esta en una liga esperando jugadores")
  void rejectsWhenRecipientIsInWaitingLeague() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var waitingLeague = League.create(recipient, 3,
        com.villo.truco.domain.shared.valueobjects.GamesToPlay.of(3), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        new FixedLeagueQueryRepository(Optional.empty(), Optional.of(waitingLeague)),
        new EmptyCupQueryRepository(), invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(
        PlayerAlreadyInWaitingLeagueException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario sigue compitiendo en una copa")
  void rejectsWhenRecipientIsBusyInCup() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var thirdPlayer = PlayerId.generate();
    final var fourthPlayer = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var cup = Cup.create(recipient, 4,
        com.villo.truco.domain.shared.valueobjects.GamesToPlay.of(3), Visibility.PRIVATE);
    cup.join(sender);
    cup.join(thirdPlayer);
    cup.join(fourthPlayer);
    cup.start(recipient);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina", thirdPlayer, "pedro", fourthPlayer,
            "lucia"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        new EmptyLeagueQueryRepository(),
        new FixedCupQueryRepository(Optional.of(cup), Optional.empty()), invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(PlayerBusyInCupException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario esta en una copa esperando jugadores")
  void rejectsWhenRecipientIsInWaitingCup() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var friendship = acceptedFriendship(sender, recipient);
    final var match = Match.create(sender, new MatchRules(2), Visibility.PRIVATE);
    final var waitingCup = Cup.create(recipient, 4,
        com.villo.truco.domain.shared.valueobjects.GamesToPlay.of(3), Visibility.PRIVATE);
    final var userQueryRepository = new FixedUserQueryRepository(
        Map.of(sender, "juancho", recipient, "martina"));
    final var handler = newHandler(userQueryRepository,
        new FixedFriendshipQueryRepository(List.of(friendship)),
        new FixedResourceInvitationQueryRepository(false), new FixedMatchQueryRepository(match),
        new EmptyLeagueQueryRepository(),
        new FixedCupQueryRepository(Optional.empty(), Optional.of(waitingCup)), invitation -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(
        PlayerAlreadyInWaitingCupException.class);
  }

  private CreateResourceInvitationCommandHandler newHandler(
      final UserQueryRepository userQueryRepository,
      final FriendshipQueryRepository friendshipQueryRepository,
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository,
      final ResourceInvitationRepository resourceInvitationRepository) {

    final SocialEventNotifier socialEventNotifier = events -> {
    };
    return new CreateResourceInvitationCommandHandler(new SocialUserGuard(userQueryRepository),
        new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
            cupQueryRepository, new NoBotRegistry()), friendshipQueryRepository,
        new ResourceInvitationPolicy(resourceInvitationQueryRepository),
        resourceInvitationRepository, socialEventNotifier,
        new InvitationTargetService(matchQueryRepository, leagueQueryRepository,
            cupQueryRepository),
        new SocialInvitationExpirationPolicy(Duration.ofMinutes(10), Duration.ofMinutes(30),
            Duration.ofMinutes(30)), new SocialViewAssembler(userQueryRepository),
        Clock.fixed(Instant.parse("2026-04-04T12:00:00Z"), ZoneOffset.UTC));
  }

  private CreateResourceInvitationCommandHandler newHandler(
      final UserQueryRepository userQueryRepository,
      final FriendshipQueryRepository friendshipQueryRepository,
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final MatchQueryRepository matchQueryRepository,
      final ResourceInvitationRepository resourceInvitationRepository) {

    return this.newHandler(userQueryRepository, friendshipQueryRepository,
        resourceInvitationQueryRepository, matchQueryRepository, new EmptyLeagueQueryRepository(),
        new EmptyCupQueryRepository(), resourceInvitationRepository);
  }

  private Friendship acceptedFriendship(final PlayerId sender, final PlayerId recipient) {

    final var friendship = Friendship.request(sender, recipient);
    friendship.accept(recipient);
    friendship.clearDomainEvents();
    return friendship;
  }

  private Friendship declinedFriendship(final PlayerId sender, final PlayerId recipient) {

    final var friendship = Friendship.request(sender, recipient);
    friendship.decline(recipient);
    friendship.clearDomainEvents();
    return friendship;
  }

  private Friendship cancelledFriendship(final PlayerId sender, final PlayerId recipient) {

    final var friendship = Friendship.request(sender, recipient);
    friendship.cancel(sender);
    friendship.clearDomainEvents();
    return friendship;
  }

  private Friendship removedFriendship(final PlayerId sender, final PlayerId recipient) {

    final var friendship = acceptedFriendship(sender, recipient);
    friendship.remove(sender);
    friendship.clearDomainEvents();
    return friendship;
  }

  private record FixedUserQueryRepository(Map<PlayerId, String> usernamesById) implements
      UserQueryRepository {

    private FixedUserQueryRepository(final Map<PlayerId, String> usernamesById) {

      this.usernamesById = new LinkedHashMap<>(usernamesById);
    }

    @Override
    public Map<PlayerId, String> findUsernamesByIds(final Set<PlayerId> playerIds) {

      return this.usernamesById.entrySet().stream()
          .filter(entry -> playerIds.contains(entry.getKey())).collect(
              Collectors.toMap(Entry::getKey, Entry::getValue, (left, right) -> left,
                  LinkedHashMap::new));
    }

    @Override
    public Optional<PlayerId> findUserIdByUsername(final String username) {

      return this.usernamesById.entrySet().stream()
          .filter(entry -> entry.getValue().equals(username)).map(Entry::getKey).findFirst();
    }

  }

  private record FixedFriendshipQueryRepository(List<Friendship> friendships) implements
      FriendshipQueryRepository {

    @Override
    public Optional<Friendship> findById(final FriendshipId friendshipId) {

      return Optional.empty();
    }

    @Override
    public boolean existsAcceptedByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return this.friendships.stream().anyMatch(
          item -> item.isAccepted() && item.involves(firstPlayerId) && item.involves(
              secondPlayerId));
    }

    @Override
    public Optional<Friendship> findPendingByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return Optional.empty();
    }

    @Override
    public Optional<Friendship> findPendingByRequesterAndAddressee(final PlayerId requesterId,
        final PlayerId addresseeId) {

      return Optional.empty();
    }

    @Override
    public Optional<Friendship> findAcceptedByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return Optional.empty();
    }

    @Override
    public List<Friendship> findAcceptedByPlayer(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<Friendship> findPendingReceivedBy(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<Friendship> findPendingSentBy(final PlayerId playerId) {

      return List.of();
    }

  }

  private record FixedResourceInvitationQueryRepository(boolean duplicated) implements
      ResourceInvitationQueryRepository {

    @Override
    public Optional<ResourceInvitation> findById(final ResourceInvitationId invitationId) {

      return Optional.empty();
    }

    @Override
    public List<ResourceInvitation> findPendingReceivedBy(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<ResourceInvitation> findPendingSentBy(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<ResourceInvitation> findPendingInvitations() {

      return List.of();
    }

    @Override
    public boolean existsPendingBySenderAndRecipientAndTarget(final PlayerId senderId,
        final PlayerId recipientId, final ResourceInvitationTargetType targetType,
        final String targetId) {

      return this.duplicated;
    }

    @Override
    public List<ResourceInvitation> findPendingByTarget(
        final ResourceInvitationTargetType targetType, final String targetId) {

      return List.of();
    }

  }

  private record FixedMatchQueryRepository(Match match, boolean unfinishedMatch) implements
      MatchQueryRepository {

    private FixedMatchQueryRepository(final Match match) {

      this(match, false);
    }

    @Override
    public Optional<Match> findById(final MatchId matchId) {

      return this.match.getId().equals(matchId) ? Optional.of(this.match) : Optional.empty();
    }

    @Override
    public Optional<Match> findByJoinCode(final JoinCode joinCode) {

      return Optional.empty();
    }

    @Override
    public boolean hasActiveMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public boolean hasUnfinishedMatch(final PlayerId playerId) {

      return this.unfinishedMatch;
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

      throw new UnsupportedOperationException();
    }

  }

  private static final class EmptyLeagueQueryRepository implements LeagueQueryRepository {

    @Override
    public Optional<League> findById(final LeagueId leagueId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findByJoinCode(final JoinCode joinCode) {

      return Optional.empty();
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

      throw new UnsupportedOperationException();
    }

  }

  private record FixedLeagueQueryRepository(Optional<League> inProgressLeague,
                                            Optional<League> waitingLeague) implements
      LeagueQueryRepository {

    @Override
    public Optional<League> findById(final LeagueId leagueId) {

      return this.inProgressLeague.filter(league -> league.getId().equals(leagueId))
          .or(() -> this.waitingLeague.filter(league -> league.getId().equals(leagueId)));
    }

    @Override
    public Optional<League> findByJoinCode(final JoinCode joinCode) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

      return this.inProgressLeague.filter(league -> league.hasPlayer(playerId));
    }

    @Override
    public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

      return this.waitingLeague.filter(league -> league.hasPlayer(playerId));
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

      throw new UnsupportedOperationException();
    }

  }

  private static final class EmptyCupQueryRepository implements CupQueryRepository {

    @Override
    public Optional<Cup> findById(final CupId cupId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findByJoinCode(final JoinCode joinCode) {

      return Optional.empty();
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

    @Override
    public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

      throw new UnsupportedOperationException();
    }

  }

  private record FixedCupQueryRepository(Optional<Cup> inProgressCup,
                                         Optional<Cup> waitingCup) implements CupQueryRepository {

    @Override
    public Optional<Cup> findById(final CupId cupId) {

      return this.inProgressCup.filter(cup -> cup.getId().equals(cupId))
          .or(() -> this.waitingCup.filter(cup -> cup.getId().equals(cupId)));
    }

    @Override
    public Optional<Cup> findByJoinCode(final JoinCode joinCode) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

      return this.inProgressCup.filter(cup -> cup.hasPlayer(playerId));
    }

    @Override
    public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

      return this.waitingCup.filter(cup -> cup.hasPlayer(playerId));
    }

    @Override
    public List<CupId> findIdleCupIds(final Instant idleSince) {

      return List.of();
    }

    @Override
    public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

      throw new UnsupportedOperationException();
    }

  }

  private static final class NoBotRegistry implements BotRegistry {

    @Override
    public boolean isBot(final PlayerId playerId) {

      return false;
    }

    @Override
    public Optional<BotProfile> getProfile(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<BotProfile> getAll() {

      return List.of();
    }

    @Override
    public void register(final BotProfile profile) {

    }

  }

}
