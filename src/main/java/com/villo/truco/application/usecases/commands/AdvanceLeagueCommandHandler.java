package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.AdvanceLeagueCommand;
import com.villo.truco.application.ports.in.AdvanceLeagueUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchActivatedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.List;
import java.util.Objects;

public final class AdvanceLeagueCommandHandler implements AdvanceLeagueUseCase {

    private final LeagueResolver leagueResolver;
    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;
    private final MatchEventNotifier matchEventNotifier;

    public AdvanceLeagueCommandHandler(final LeagueResolver leagueResolver,
        final LeagueRepository leagueRepository, final MatchRepository matchRepository,
        final MatchEventNotifier matchEventNotifier) {

        this.leagueResolver = Objects.requireNonNull(leagueResolver);
        this.leagueRepository = Objects.requireNonNull(leagueRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    }

    @Override
    public Void handle(final AdvanceLeagueCommand command) {

        final var league = this.leagueResolver.resolve(command.leagueId());

        league.recordMatchWinner(command.matchId(), command.winner());

        final var matchRules = MatchRules.fromGamesToPlay(league.getGamesToPlay());

        for (final var activation : league.activateNextFixtures()) {
            final var match =
                Match.createReady(activation.playerOne(), activation.playerTwo(), matchRules);
            this.matchRepository.save(match);
            league.linkFixtureMatch(activation.fixtureId(), match.getId());
            this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
                match.getPlayerTwo(), List.of(new MatchActivatedEvent(match.getId())));
        }

        this.leagueRepository.save(league);

        return null;
    }

}
