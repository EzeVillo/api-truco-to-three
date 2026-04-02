package com.villo.truco.auth.domain.model.user;

import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record UserSnapshot(PlayerId id, Username username, HashedPassword hashedPassword) {

}
