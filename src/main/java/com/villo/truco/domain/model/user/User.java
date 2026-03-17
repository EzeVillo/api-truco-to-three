package com.villo.truco.domain.model.user;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record User(PlayerId id, String username, String hashedPassword) {

}
