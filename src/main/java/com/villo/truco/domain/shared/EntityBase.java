package com.villo.truco.domain.shared;

import java.util.Objects;

public abstract class EntityBase<T> {

    protected final T id;

    protected EntityBase(T id) {

        this.id = Objects.requireNonNull(id, "Entity id cannot be null");
    }

    public T getId() {

        return id;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var that = (EntityBase) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

}
