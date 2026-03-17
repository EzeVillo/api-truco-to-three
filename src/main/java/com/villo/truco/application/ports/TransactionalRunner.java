package com.villo.truco.application.ports;

public interface TransactionalRunner {

  void run(Runnable action);

}
