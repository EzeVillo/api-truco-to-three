package com.villo.truco.social.infrastructure.config;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import com.villo.truco.social.application.ports.in.AcceptFriendshipUseCase;
import com.villo.truco.social.application.ports.in.AcceptResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.CancelFriendshipUseCase;
import com.villo.truco.social.application.ports.in.CancelResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.CreateResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.DeclineFriendshipUseCase;
import com.villo.truco.social.application.ports.in.DeclineResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.ExpirePendingResourceInvitationsUseCase;
import com.villo.truco.social.application.ports.in.GetFriendsUseCase;
import com.villo.truco.social.application.ports.in.GetFriendshipRequestsUseCase;
import com.villo.truco.social.application.ports.in.GetResourceInvitationsUseCase;
import com.villo.truco.social.application.ports.in.GetSentFriendshipRequestsUseCase;
import com.villo.truco.social.application.ports.in.GetSentResourceInvitationsUseCase;
import com.villo.truco.social.application.ports.in.RemoveFriendshipUseCase;
import com.villo.truco.social.application.ports.in.RequestFriendshipUseCase;
import com.villo.truco.social.application.services.FriendshipResolver;
import com.villo.truco.social.application.services.InvitationTargetService;
import com.villo.truco.social.application.services.ResourceInvitationPolicy;
import com.villo.truco.social.application.services.ResourceInvitationResolver;
import com.villo.truco.social.application.services.SocialFriendshipParticipantsSupport;
import com.villo.truco.social.application.services.SocialInvitationExpirationPolicy;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.application.usecases.commands.AcceptFriendshipCommandHandler;
import com.villo.truco.social.application.usecases.commands.AcceptResourceInvitationCommandHandler;
import com.villo.truco.social.application.usecases.commands.CancelFriendshipCommandHandler;
import com.villo.truco.social.application.usecases.commands.CancelResourceInvitationCommandHandler;
import com.villo.truco.social.application.usecases.commands.CreateResourceInvitationCommandHandler;
import com.villo.truco.social.application.usecases.commands.DeclineFriendshipCommandHandler;
import com.villo.truco.social.application.usecases.commands.DeclineResourceInvitationCommandHandler;
import com.villo.truco.social.application.usecases.commands.ExpirePendingResourceInvitationsCommandHandler;
import com.villo.truco.social.application.usecases.commands.RemoveFriendshipCommandHandler;
import com.villo.truco.social.application.usecases.commands.RequestFriendshipCommandHandler;
import com.villo.truco.social.application.usecases.queries.GetFriendsQueryHandler;
import com.villo.truco.social.application.usecases.queries.GetFriendshipRequestsQueryHandler;
import com.villo.truco.social.application.usecases.queries.GetResourceInvitationsQueryHandler;
import com.villo.truco.social.application.usecases.queries.GetSentFriendshipRequestsQueryHandler;
import com.villo.truco.social.application.usecases.queries.GetSentResourceInvitationsQueryHandler;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SocialInvitationExpirationProperties.class)
public class SocialUseCaseConfiguration {

  private final UserQueryRepository userQueryRepository;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final FriendshipRepository friendshipRepository;
  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;
  private final ResourceInvitationRepository resourceInvitationRepository;
  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final TransactionalRunner transactionalRunner;
  private final UseCasePipeline retryTransactionalPipeline;
  private final UseCasePipeline transactionalPipeline;
  private final SocialInvitationExpirationProperties socialInvitationExpirationProperties;
  private final Clock clock;

  public SocialUseCaseConfiguration(final UserQueryRepository userQueryRepository,
      final FriendshipQueryRepository friendshipQueryRepository,
      final FriendshipRepository friendshipRepository,
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final ResourceInvitationRepository resourceInvitationRepository,
      final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final TransactionalRunner transactionalRunner,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline,
      final SocialInvitationExpirationProperties socialInvitationExpirationProperties,
      final Clock clock) {

    this.userQueryRepository = userQueryRepository;
    this.friendshipQueryRepository = friendshipQueryRepository;
    this.friendshipRepository = friendshipRepository;
    this.resourceInvitationQueryRepository = resourceInvitationQueryRepository;
    this.resourceInvitationRepository = resourceInvitationRepository;
    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.transactionalRunner = transactionalRunner;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
    this.transactionalPipeline = transactionalPipeline;
    this.socialInvitationExpirationProperties = socialInvitationExpirationProperties;
    this.clock = clock;
  }

  @Bean
  SocialUserGuard socialUserGuard() {

    return new SocialUserGuard(this.userQueryRepository);
  }

  @Bean
  FriendshipResolver friendshipResolver() {

    return new FriendshipResolver(this.friendshipQueryRepository);
  }

  @Bean
  ResourceInvitationResolver resourceInvitationResolver() {

    return new ResourceInvitationResolver(this.resourceInvitationQueryRepository);
  }

  @Bean
  SocialViewAssembler socialViewAssembler() {

    return new SocialViewAssembler(this.userQueryRepository);
  }

  @Bean
  InvitationTargetService invitationTargetService() {

    return new InvitationTargetService(this.matchQueryRepository, this.leagueQueryRepository,
        this.cupQueryRepository);
  }

  @Bean
  ResourceInvitationPolicy resourceInvitationPolicy() {

    return new ResourceInvitationPolicy(this.resourceInvitationQueryRepository);
  }

  @Bean
  SocialInvitationExpirationPolicy socialInvitationExpirationPolicy() {

    return new SocialInvitationExpirationPolicy(this.socialInvitationExpirationProperties.match(),
        this.socialInvitationExpirationProperties.league(),
        this.socialInvitationExpirationProperties.cup());
  }

  @Bean
  SocialFriendshipParticipantsSupport socialFriendshipParticipantsSupport() {

    return new SocialFriendshipParticipantsSupport(this.friendshipQueryRepository);
  }

  @Bean
  RequestFriendshipUseCase requestFriendshipUseCase(final SocialEventNotifier socialEventNotifier) {

    final var handler = new RequestFriendshipCommandHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.friendshipRepository, socialEventNotifier);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  AcceptFriendshipUseCase acceptFriendshipUseCase(final SocialEventNotifier socialEventNotifier) {

    final var handler = new AcceptFriendshipCommandHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.friendshipRepository, socialEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  DeclineFriendshipUseCase declineFriendshipUseCase(final SocialEventNotifier socialEventNotifier) {

    final var handler = new DeclineFriendshipCommandHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.friendshipRepository, socialEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CancelFriendshipUseCase cancelFriendshipUseCase(final SocialEventNotifier socialEventNotifier) {

    final var handler = new CancelFriendshipCommandHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.friendshipRepository, socialEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  RemoveFriendshipUseCase removeFriendshipUseCase(final SocialEventNotifier socialEventNotifier) {

    final var handler = new RemoveFriendshipCommandHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.friendshipRepository, socialEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetFriendsUseCase getFriendsUseCase() {

    return new GetFriendsQueryHandler(this.socialUserGuard(), this.friendshipQueryRepository,
        this.socialViewAssembler());
  }

  @Bean
  GetFriendshipRequestsUseCase getFriendshipRequestsUseCase() {

    return new GetFriendshipRequestsQueryHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.socialViewAssembler());
  }

  @Bean
  CreateResourceInvitationUseCase createResourceInvitationUseCase(
      final SocialEventNotifier socialEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    final var handler = new CreateResourceInvitationCommandHandler(this.socialUserGuard(),
        playerAvailabilityChecker, this.friendshipQueryRepository, this.resourceInvitationPolicy(),
        this.resourceInvitationRepository, socialEventNotifier, this.invitationTargetService(),
        this.socialInvitationExpirationPolicy(), this.socialViewAssembler(), this.clock);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  AcceptResourceInvitationUseCase acceptResourceInvitationUseCase(
      final SocialEventNotifier socialEventNotifier) {

    final var handler = new AcceptResourceInvitationCommandHandler(this.socialUserGuard(),
        this.resourceInvitationResolver(), this.resourceInvitationRepository, socialEventNotifier,
        this.socialViewAssembler());
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  DeclineResourceInvitationUseCase declineResourceInvitationUseCase(
      final SocialEventNotifier socialEventNotifier) {

    final var handler = new DeclineResourceInvitationCommandHandler(this.socialUserGuard(),
        this.resourceInvitationResolver(), this.resourceInvitationRepository, socialEventNotifier,
        this.socialViewAssembler());
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetResourceInvitationsUseCase getResourceInvitationsUseCase() {

    return new GetResourceInvitationsQueryHandler(this.socialUserGuard(),
        this.resourceInvitationQueryRepository, this.socialViewAssembler());
  }

  @Bean
  GetSentFriendshipRequestsUseCase getSentFriendshipRequestsUseCase() {

    return new GetSentFriendshipRequestsQueryHandler(this.socialUserGuard(),
        this.friendshipQueryRepository, this.socialViewAssembler());
  }

  @Bean
  GetSentResourceInvitationsUseCase getSentResourceInvitationsUseCase() {

    return new GetSentResourceInvitationsQueryHandler(this.socialUserGuard(),
        this.resourceInvitationQueryRepository, this.socialViewAssembler());
  }

  @Bean
  CancelResourceInvitationUseCase cancelResourceInvitationUseCase(
      final SocialEventNotifier socialEventNotifier) {

    final var handler = new CancelResourceInvitationCommandHandler(this.socialUserGuard(),
        this.resourceInvitationResolver(), this.resourceInvitationRepository, socialEventNotifier,
        this.socialViewAssembler());
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  ExpirePendingResourceInvitationsUseCase expirePendingResourceInvitationsUseCase(
      final SocialEventNotifier socialEventNotifier) {

    return new ExpirePendingResourceInvitationsCommandHandler(
        this.resourceInvitationQueryRepository, this.resourceInvitationRepository,
        socialEventNotifier, this.transactionalRunner, this.clock);
  }

}
