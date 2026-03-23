package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.valueobjects.BoutPairing;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record MatchAdvancementResult(List<BoutPairing> pendingPairings, boolean cupFinished,
                                     PlayerId champion) {

  public static MatchAdvancementResult empty() {

    return new MatchAdvancementResult(List.of(), false, null);
  }

}
