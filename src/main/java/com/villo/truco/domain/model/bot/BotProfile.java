package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class BotProfile extends EntityBase<PlayerId> {

  private final String displayName;
  private final BotPersonality personality;

  public BotProfile(final PlayerId playerId, final String displayName,
      final BotPersonality personality) {

    super(playerId);
    this.displayName = Objects.requireNonNull(displayName);
    this.personality = Objects.requireNonNull(personality);
  }

  public PlayerId playerId() {

    return getId();
  }

  public String displayName() {

    return displayName;
  }

  public BotPersonality personality() {

    return personality;
  }

}
