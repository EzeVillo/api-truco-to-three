package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinLeagueCommand;
import com.villo.truco.application.dto.JoinLeagueDTO;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class JoinLeagueCommandHandler implements JoinLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;

  public JoinLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
  }

  @Override
  public JoinLeagueDTO handle(final JoinLeagueCommand command) {

    final var league = this.leagueResolver.resolve(command.inviteCode());

    league.join(command.playerId(), command.inviteCode());

    this.leagueRepository.save(league);

    return new JoinLeagueDTO(league.getId().value().toString());
  }

}
