package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.util.List;
import java.util.Set;

public record CupSnapshot(CupId id, List<PlayerId> participants, List<BoutSnapshot> bouts,
                          Set<PlayerId> forfeitedPlayers, int numberOfPlayers,
                          GamesToPlay gamesToPlay, Visibility visibility, JoinCode joinCode,
                          CupStatus status, PlayerId champion) {

}
