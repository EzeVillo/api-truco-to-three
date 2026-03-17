package com.villo.truco.application.ports.in;

public interface UseCase<C, R> {

  R handle(C command);

}
