package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Set;

public interface HiddenBotIdsProvider {

  Set<PlayerId> hiddenBotIds();

}
