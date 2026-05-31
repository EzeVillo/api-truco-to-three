package com.villo.truco.auth.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.auth.application.model.AuthenticatedSessionIdentity;
import com.villo.truco.auth.application.queries.GetCurrentSessionIdentityQuery;

public interface GetCurrentSessionIdentityUseCase extends
    UseCase<GetCurrentSessionIdentityQuery, AuthenticatedSessionIdentity> {

}
