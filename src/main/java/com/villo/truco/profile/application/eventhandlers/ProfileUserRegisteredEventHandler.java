package com.villo.truco.profile.application.eventhandlers;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.auth.domain.model.user.events.UserRegisteredEvent;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.model.PlayerStats;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import java.util.Objects;

public final class ProfileUserRegisteredEventHandler implements
    DomainEventHandler<UserRegisteredEvent> {

  private final PlayerProfileRepository playerProfileRepository;
  private final PlayerStatsRepository playerStatsRepository;

  public ProfileUserRegisteredEventHandler(final PlayerProfileRepository playerProfileRepository,
      final PlayerStatsRepository playerStatsRepository) {

    this.playerProfileRepository = Objects.requireNonNull(playerProfileRepository);
    this.playerStatsRepository = Objects.requireNonNull(playerStatsRepository);
  }

  @Override
  public Class<UserRegisteredEvent> eventType() {

    return UserRegisteredEvent.class;
  }

  @Override
  public void handle(final UserRegisteredEvent event) {

    this.playerProfileRepository.save(PlayerProfile.create(event.getUserId()));
    this.playerStatsRepository.save(PlayerStats.create(event.getUserId()));
  }

}
