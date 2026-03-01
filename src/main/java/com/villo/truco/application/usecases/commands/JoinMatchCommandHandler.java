package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class JoinMatchCommandHandler implements JoinMatchUseCase {

    private final MatchResolver matchResolver;
    private final MatchRepository matchRepository;
    private final MatchEventNotifier matchEventNotifier;

    public JoinMatchCommandHandler(final MatchResolver matchResolver,
        final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

        this.matchResolver = Objects.requireNonNull(matchResolver);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    }

    @Override
    public void handle(final JoinMatchCommand command) {

        final var match = this.matchResolver.resolve(command.matchId());

        match.join(command.playerTwoId());

        this.matchRepository.save(match);
        this.matchEventNotifier.notifyPlayers(match.getId(), match.getPlayerOne(),
            match.getPlayerTwo());
    }

}
