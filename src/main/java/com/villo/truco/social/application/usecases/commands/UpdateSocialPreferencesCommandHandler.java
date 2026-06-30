package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.social.application.commands.UpdateSocialPreferencesCommand;
import com.villo.truco.social.application.dto.SocialPreferencesDTO;
import com.villo.truco.social.application.ports.in.UpdateSocialPreferencesUseCase;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import java.util.Objects;

public final class UpdateSocialPreferencesCommandHandler implements UpdateSocialPreferencesUseCase {

  private final SocialUserGuard socialUserGuard;
  private final SocialPreferencesRepository socialPreferencesRepository;

  public UpdateSocialPreferencesCommandHandler(final SocialUserGuard socialUserGuard,
      final SocialPreferencesRepository socialPreferencesRepository) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.socialPreferencesRepository = Objects.requireNonNull(socialPreferencesRepository);
  }

  @Override
  public SocialPreferencesDTO handle(final UpdateSocialPreferencesCommand command) {

    this.socialUserGuard.ensureRegisteredUser(command.playerId());
    final var preferences = this.socialPreferencesRepository.findByPlayerId(command.playerId())
        .orElseGet(() -> SocialPreferences.create(command.playerId()));

    preferences.changeAcceptsFriendRequests(command.acceptsFriendRequests());
    this.socialPreferencesRepository.save(preferences);

    return new SocialPreferencesDTO(preferences.acceptsFriendRequests());
  }

}
