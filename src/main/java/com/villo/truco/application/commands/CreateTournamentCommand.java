package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public record CreateTournamentCommand(List<PlayerId> playerIds) {

    public CreateTournamentCommand {

        Objects.requireNonNull(playerIds);
    }

    public static CreateTournamentCommand fromPlayerIds(final List<String> playerIds) {

        Objects.requireNonNull(playerIds);
        return new CreateTournamentCommand(playerIds.stream().map(PlayerId::of).toList());
    }

}
