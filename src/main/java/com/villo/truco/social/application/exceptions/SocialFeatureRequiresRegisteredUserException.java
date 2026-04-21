package com.villo.truco.social.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class SocialFeatureRequiresRegisteredUserException extends ApplicationException {

  public SocialFeatureRequiresRegisteredUserException() {

    super(ApplicationStatus.UNAUTHORIZED, "Social features require a registered user account");
  }

}
