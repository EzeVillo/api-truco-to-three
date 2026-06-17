package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInWaitingCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInWaitingLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
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
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import com.villo.truco.testutil.NoOpSpectatorshipRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("CreateResourceInvitationCommandHandler")
class CreateResourceInvitationCommandHandlerTest {

  @Test
  @DisplayName("resuelve recipientUsername y crea la invitacion")
  void createsInvitationByRecipientUsername() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenAnswer(inv -> {
      final Set<PlayerId> ids = inv.getArgument(0);
      final var result = new HashMap<PlayerId, String>();
      if (ids.contains(sender)) {
        result.put(sender, "juancho");
      }
      if (ids.contains(recipient)) {
        result.put(recipient, "martina");
      }
      return result;
    });
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var invQueryRepo = mock(ResourceInvitationQueryRepository.class);
    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.findById(match.getId())).thenReturn(Optional.of(match));

    final var invRepo = mock(ResourceInvitationRepository.class);
    final var handler = newHandler(userRepo, friendshipRepo, invQueryRepo, matchRepo, invRepo);

    final var dto = handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()));

    final var captor = ArgumentCaptor.forClass(ResourceInvitation.class);
    verify(invRepo).save(captor.capture());
    assertThat(captor.getValue().getSenderId()).isEqualTo(sender);
    assertThat(captor.getValue().getRecipientId()).isEqualTo(recipient);
    assertThat(dto.senderUsername()).isEqualTo("juancho");
    assertThat(dto.recipientUsername()).isEqualTo("martina");
    assertThat(dto.status()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("rechaza usernames inexistentes")
  void rejectsUnknownRecipientUsername() {

    final var sender = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(sender, "juancho"));

    final var handler = newHandler(userRepo, mock(FriendshipQueryRepository.class),
        mock(ResourceInvitationQueryRepository.class), mock(MatchQueryRepository.class),
        mock(ResourceInvitationRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(SocialUserNotFoundException.class);
  }

  @Test
  @DisplayName("mantiene la validacion de amistad aceptada")
  void rejectsWhenFriendshipIsNotAccepted() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var handler = newHandler(userRepo, mock(FriendshipQueryRepository.class),
        mock(ResourceInvitationQueryRepository.class), mock(MatchQueryRepository.class),
        mock(ResourceInvitationRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(FriendshipRequiredException.class);
  }

  @Test
  @DisplayName("ignora historial no activo y exige amistad aceptada vigente")
  void rejectsWhenOnlyHistoricalNonActiveFriendshipsExist() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    // existsAcceptedByPlayers defaults to false — historical non-active friendships are not accepted

    final var handler = newHandler(userRepo, friendshipRepo,
        mock(ResourceInvitationQueryRepository.class), mock(MatchQueryRepository.class),
        mock(ResourceInvitationRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(FriendshipRequiredException.class);
  }

  @Test
  @DisplayName("mantiene la validacion de invitacion pendiente duplicada")
  void rejectsDuplicatedPendingInvitation() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var invQueryRepo = mock(ResourceInvitationQueryRepository.class);
    when(invQueryRepo.existsPendingBySenderAndRecipientAndTarget(any(), any(),
        any(ResourceInvitationTargetType.class), any())).thenReturn(true);

    final var handler = newHandler(userRepo, friendshipRepo, invQueryRepo,
        mock(MatchQueryRepository.class), mock(ResourceInvitationRepository.class));

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
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.hasUnfinishedMatch(any())).thenReturn(true);

    final var handler = newHandler(userRepo, friendshipRepo,
        mock(ResourceInvitationQueryRepository.class), matchRepo,
        mock(ResourceInvitationRepository.class));

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
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var league = League.create(recipient, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(sender);
    league.join(thirdPlayer);
    league.start(recipient);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina", thirdPlayer, "pedro"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var matchRepo = mock(MatchQueryRepository.class);
    final var leagueRepo = mock(LeagueQueryRepository.class);
    when(leagueRepo.findInProgressByPlayer(any())).thenReturn(Optional.of(league));

    final var handler = newHandler(userRepo, friendshipRepo,
        mock(ResourceInvitationQueryRepository.class), matchRepo, leagueRepo,
        mock(CupQueryRepository.class), mock(ResourceInvitationRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(PlayerBusyInLeagueException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario esta en una liga esperando jugadores")
  void rejectsWhenRecipientIsInWaitingLeague() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var waitingLeague = League.create(recipient, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var leagueRepo = mock(LeagueQueryRepository.class);
    when(leagueRepo.findWaitingByPlayer(any())).thenReturn(Optional.of(waitingLeague));

    final var handler = newHandler(userRepo, friendshipRepo,
        mock(ResourceInvitationQueryRepository.class), mock(MatchQueryRepository.class), leagueRepo,
        mock(CupQueryRepository.class), mock(ResourceInvitationRepository.class));

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
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var cup = Cup.create(recipient, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    cup.join(sender);
    cup.join(thirdPlayer);
    cup.join(fourthPlayer);
    cup.start(recipient);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina", thirdPlayer, "pedro", fourthPlayer,
            "lucia"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var cupRepo = mock(CupQueryRepository.class);
    when(cupRepo.findInProgressByPlayer(any())).thenReturn(Optional.of(cup));

    final var handler = newHandler(userRepo, friendshipRepo,
        mock(ResourceInvitationQueryRepository.class), mock(MatchQueryRepository.class),
        mock(LeagueQueryRepository.class), cupRepo, mock(ResourceInvitationRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new CreateResourceInvitationCommand(sender.value().toString(), "martina", "MATCH",
            match.getId().value().toString()))).isInstanceOf(PlayerBusyInCupException.class);
  }

  @Test
  @DisplayName("rechaza cuando el destinatario esta en una copa esperando jugadores")
  void rejectsWhenRecipientIsInWaitingCup() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var match = Match.create(sender, new MatchRules(2, true), Visibility.PRIVATE);
    final var waitingCup = Cup.create(recipient, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(sender, "juancho", recipient, "martina"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(recipient));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.existsAcceptedByPlayers(sender, recipient)).thenReturn(true);

    final var cupRepo = mock(CupQueryRepository.class);
    when(cupRepo.findWaitingByPlayer(any())).thenReturn(Optional.of(waitingCup));

    final var handler = newHandler(userRepo, friendshipRepo,
        mock(ResourceInvitationQueryRepository.class), mock(MatchQueryRepository.class),
        mock(LeagueQueryRepository.class), cupRepo, mock(ResourceInvitationRepository.class));

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

    return new CreateResourceInvitationCommandHandler(new SocialUserGuard(userQueryRepository),
        new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
            cupQueryRepository, mock(BotRegistry.class), mock(RematchSessionRepository.class),
            mock(QuickMatchQueuePort.class), NoOpSpectatorshipRepository.INSTANCE,
            new InMemoryBotVsBotMatchRegistry()), friendshipQueryRepository,
        new ResourceInvitationPolicy(resourceInvitationQueryRepository),
        resourceInvitationRepository, events -> {
    }, new InvitationTargetService(matchQueryRepository, leagueQueryRepository, cupQueryRepository),
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
        resourceInvitationQueryRepository, matchQueryRepository, mock(LeagueQueryRepository.class),
        mock(CupQueryRepository.class), resourceInvitationRepository);
  }

}
