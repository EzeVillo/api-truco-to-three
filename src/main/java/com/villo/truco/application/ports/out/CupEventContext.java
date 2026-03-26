package com.villo.truco.application.ports.out;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record CupEventContext(CupId cupId, List<PlayerId> participants) {

}
