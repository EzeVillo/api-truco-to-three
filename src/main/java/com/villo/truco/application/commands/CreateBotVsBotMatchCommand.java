package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CreateBotVsBotMatchCommand(PlayerId ownerId, GamesToPlay gamesToPlay,
                                         PlayerId botOneId, PlayerId botTwoId) {

  public CreateBotVsBotMatchCommand(final String ownerId, final int gamesToPlay,
      final String botOneId, final String botTwoId) {

    this(PlayerId.of(ownerId), GamesToPlay.of(gamesToPlay), PlayerId.of(botOneId),
        PlayerId.of(botTwoId));
  }

}
