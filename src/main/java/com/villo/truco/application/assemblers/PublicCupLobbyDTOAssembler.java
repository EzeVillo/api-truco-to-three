package com.villo.truco.application.assemblers;

import com.villo.truco.application.dto.PublicCupLobbyDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PublicCupLobbyDTOAssembler {

  private final PublicActorResolver publicActorResolver;

  public PublicCupLobbyDTOAssembler(final PublicActorResolver publicActorResolver) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  public List<PublicCupLobbyDTO> assembleAll(final List<Cup> cups) {

    final var actorNames = this.publicActorResolver.resolveAll(
        cups.stream().map(Cup::getCreator).collect(Collectors.toSet()));
    return cups.stream().map(cup -> assemble(cup, actorNames)).toList();
  }

  public PublicCupLobbyDTO assemble(final Cup cup) {

    return this.assemble(cup,
        Map.of(cup.getCreator(), this.publicActorResolver.resolve(cup.getCreator())));
  }

  private PublicCupLobbyDTO assemble(final Cup cup, final Map<PlayerId, String> actorNames) {

    return new PublicCupLobbyDTO(cup.getId().value().toString(), actorNames.get(cup.getCreator()),
        cup.getGamesToPlay().value(), cup.getNumberOfPlayers(), cup.getParticipants().size(),
        cup.getStatus().name(), cup.getJoinCode().value());
  }

}
