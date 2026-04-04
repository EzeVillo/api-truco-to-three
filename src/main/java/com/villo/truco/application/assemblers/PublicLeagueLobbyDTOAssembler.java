package com.villo.truco.application.assemblers;

import com.villo.truco.application.dto.PublicLeagueLobbyDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PublicLeagueLobbyDTOAssembler {

  private final PublicActorResolver publicActorResolver;

  public PublicLeagueLobbyDTOAssembler(final PublicActorResolver publicActorResolver) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  public List<PublicLeagueLobbyDTO> assembleAll(final List<League> leagues) {

    final var actorNames = this.publicActorResolver.resolveAll(
        leagues.stream().map(League::getCreator).collect(Collectors.toSet()));
    return leagues.stream().map(league -> assemble(league, actorNames)).toList();
  }

  public PublicLeagueLobbyDTO assemble(final League league) {

    return this.assemble(league,
        Map.of(league.getCreator(), this.publicActorResolver.resolve(league.getCreator())));
  }

  private PublicLeagueLobbyDTO assemble(final League league,
      final Map<PlayerId, String> actorNames) {

    return new PublicLeagueLobbyDTO(league.getId().value().toString(),
        actorNames.get(league.getCreator()), league.getGamesToPlay().value(),
        league.getNumberOfPlayers(), league.getParticipants().size(), league.getStatus().name());
  }

}
