package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record MatchAdvancementResult(List<BoutPairing> pendingPairings, boolean cupFinished,
                                     PlayerId champion) {

  public static MatchAdvancementResult empty() {

    return new MatchAdvancementResult(List.of(), false, null);
  }

  public record BoutPairing(BoutId boutId, PlayerId playerOne, PlayerId playerTwo) {

  }

}
