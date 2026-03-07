package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.ports.MatchRepository;

public final class CreateMatchCommandHandler implements CreateMatchUseCase {

  private final MatchRepository matchRepository;
  private final PlayerTokenProvider tokenProvider;

  public CreateMatchCommandHandler(final MatchRepository matchRepository,
      final PlayerTokenProvider tokenProvider) {

    this.matchRepository = matchRepository;
    this.tokenProvider = tokenProvider;
  }

  @Override
  public CreateMatchDTO handle(final CreateMatchCommand command) {

    final var playerOneId = PlayerId.generate();
    final var playerTwoId = PlayerId.generate();
    final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay());
    final var match = Match.create(playerOneId, playerTwoId, rules);

    this.matchRepository.save(match);

    final var accessToken = this.tokenProvider.generateAccessToken(match.getId(), playerOneId);

    return new CreateMatchDTO(match.getId().value().toString(), accessToken,
        match.getInviteCode().value());
  }

}
