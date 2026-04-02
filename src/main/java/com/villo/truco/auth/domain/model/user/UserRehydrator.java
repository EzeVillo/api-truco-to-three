package com.villo.truco.auth.domain.model.user;

public final class UserRehydrator {

  private UserRehydrator() {

  }

  public static User rehydrate(final UserSnapshot snapshot) {

    return User.reconstruct(snapshot.id(), snapshot.username(), snapshot.hashedPassword());
  }

}
