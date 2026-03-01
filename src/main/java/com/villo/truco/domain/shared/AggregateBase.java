package com.villo.truco.domain.shared;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateBase<T> extends EntityBase<T> {

    protected final List<DomainEventBase> domainEvents = new ArrayList<>();

    protected AggregateBase(T id) {

        super(id);
    }

    public void addDomainEvent(DomainEventBase event) {

        domainEvents.add(event);
    }

    public List<DomainEventBase> getDomainEvents() {

        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {

        domainEvents.clear();
    }

}
