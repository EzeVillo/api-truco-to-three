package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchRepository;

public final class CreateMatchCommandHandler implements CreateMatchUseCase {

  private final MatchRepository matchRepository;

  public CreateMatchCommandHandler(final MatchRepository matchRepository) {

    this.matchRepository = matchRepository;
  }

  @Override
  public CreateMatchDTO handle(final CreateMatchCommand command) {

    final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay());
    final var match = Match.create(command.playerId(), rules);

    this.matchRepository.save(match);

    return new CreateMatchDTO(match.getId().value().toString(), match.getInviteCode().value());
  }

}
