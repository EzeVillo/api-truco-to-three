package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class SocialUserNotFoundException extends ApplicationException {

  public SocialUserNotFoundException(final String username) {

    super(ApplicationStatus.NOT_FOUND, "User not found for username: " + username);
  }

}
