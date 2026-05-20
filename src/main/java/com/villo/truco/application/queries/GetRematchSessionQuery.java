package com.villo.truco.application.queries;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetRematchSessionQuery(MatchId originMatchId, PlayerId requester) {

  public GetRematchSessionQuery {

    Objects.requireNonNull(originMatchId);
    Objects.requireNonNull(requester);
  }

  public GetRematchSessionQuery(final String originMatchId, final String requester) {

    this(MatchId.of(originMatchId), PlayerId.of(requester));
  }

}
