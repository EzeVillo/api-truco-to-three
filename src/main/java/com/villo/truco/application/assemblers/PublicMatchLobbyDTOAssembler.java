package com.villo.truco.application.assemblers;

import com.villo.truco.application.dto.PublicMatchLobbyDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PublicMatchLobbyDTOAssembler {

  private final PublicActorResolver publicActorResolver;

  public PublicMatchLobbyDTOAssembler(final PublicActorResolver publicActorResolver) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  public List<PublicMatchLobbyDTO> assembleAll(final List<Match> matches) {

    final var actorNames = this.publicActorResolver.resolveAll(
        matches.stream().map(Match::getPlayerOne).collect(Collectors.toSet()));
    return matches.stream().map(match -> assemble(match, actorNames)).toList();
  }

  public PublicMatchLobbyDTO assemble(final Match match) {

    return this.assemble(match,
        Map.of(match.getPlayerOne(), this.publicActorResolver.resolve(match.getPlayerOne())));
  }

  private PublicMatchLobbyDTO assemble(final Match match, final Map<PlayerId, String> actorNames) {

    return new PublicMatchLobbyDTO(match.getId().value().toString(),
        actorNames.get(match.getPlayerOne()), match.getGamesToPlay(), 2,
        match.getPlayerTwo() == null ? 1 : 2, match.getStatus().name(),
        match.getJoinCode().value());
  }

}
