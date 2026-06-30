package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.SocialPreferencesDTO;
import com.villo.truco.social.application.ports.in.GetSocialPreferencesUseCase;
import com.villo.truco.social.application.queries.GetSocialPreferencesQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import java.util.Objects;

public final class GetSocialPreferencesQueryHandler implements GetSocialPreferencesUseCase {

  private final SocialUserGuard socialUserGuard;
  private final SocialPreferencesRepository socialPreferencesRepository;

  public GetSocialPreferencesQueryHandler(final SocialUserGuard socialUserGuard,
      final SocialPreferencesRepository socialPreferencesRepository) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.socialPreferencesRepository = Objects.requireNonNull(socialPreferencesRepository);
  }

  @Override
  public SocialPreferencesDTO handle(final GetSocialPreferencesQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    final var acceptsFriendRequests = this.socialPreferencesRepository.findByPlayerId(
        query.playerId()).map(SocialPreferences::acceptsFriendRequests).orElse(true);

    return new SocialPreferencesDTO(acceptsFriendRequests);
  }

}
