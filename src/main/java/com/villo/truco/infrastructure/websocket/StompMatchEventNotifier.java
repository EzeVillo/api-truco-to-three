package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.infrastructure.http.dto.response.MatchStateResponse;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public final class StompMatchEventNotifier implements MatchEventNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchQueryRepository matchQueryRepository;

    public StompMatchEventNotifier(final SimpMessagingTemplate messagingTemplate,
        final MatchQueryRepository matchQueryRepository) {

        this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
        this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    }

    @Override
    public void notifyPlayers(final MatchId matchId, final PlayerId playerOneId,
        final PlayerId playerTwoId) {

        this.notifyPlayer(matchId, playerOneId);
        this.notifyPlayer(matchId, playerTwoId);
    }

    private void notifyPlayer(final MatchId matchId, final PlayerId playerId) {

        this.matchQueryRepository.findById(matchId).ifPresent(match -> {
            final var dto = MatchStateDTO.of(match, playerId);
            this.messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId.value().toString() + "/" + playerId.value().toString(),
                MatchStateResponse.from(dto));
        });
    }

}
