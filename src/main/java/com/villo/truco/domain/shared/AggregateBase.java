package com.villo.truco.domain.shared;

public abstract class AggregateBase<T> extends EntityBase<T> {

  protected AggregateBase(T id) {

    super(id);
  }

}
