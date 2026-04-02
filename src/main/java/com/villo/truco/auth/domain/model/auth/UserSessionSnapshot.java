package com.villo.truco.auth.domain.model.auth;

import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record UserSessionSnapshot(UserSessionId id, PlayerId userId,
                                  List<RefreshTokenSnapshot> refreshTokens) {

}
