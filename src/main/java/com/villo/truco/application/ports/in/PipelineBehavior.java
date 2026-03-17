package com.villo.truco.application.ports.in;

import java.util.function.Supplier;

public interface PipelineBehavior {

  <C, R> R handle(C command, Supplier<R> next);

}
