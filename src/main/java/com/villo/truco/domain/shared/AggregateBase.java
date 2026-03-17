package com.villo.truco.domain.shared;

public abstract class AggregateBase<T> extends EntityBase<T> {

  private long version = 0;

  protected AggregateBase(T id) {

    super(id);
  }

  public long getVersion() {

    return version;
  }

  public void setVersion(long version) {

    this.version = version;
  }

}
