package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.ports.MatchRepository;

public final class CreateMatchCommandHandler implements CreateMatchUseCase {

    private final MatchRepository matchRepository;
    private final MatchRules matchRules;

    public CreateMatchCommandHandler(final MatchRepository matchRepository,
        final MatchRules matchRules) {

        this.matchRepository = matchRepository;
        this.matchRules = matchRules;
    }

    @Override
    public CreateMatchDTO handle(final CreateMatchCommand command) {

        final var playerOneId = PlayerId.generate();
        final var playerTwoId = PlayerId.generate();
        final var match = Match.create(playerOneId, playerTwoId, this.matchRules);

        this.matchRepository.save(match);

        return new CreateMatchDTO(match.getId().value().toString(), playerOneId.value().toString(),
            playerTwoId.value().toString());
    }

}
