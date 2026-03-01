package com.villo.truco.domain.shared;

public abstract class DomainEventBase {

    private final long timestamp = System.currentTimeMillis();
    private final String eventType;

    protected DomainEventBase(final String eventType) {

        this.eventType = eventType;
    }

    public long getTimestamp() {

        return timestamp;
    }

    public String getEventType() {

        return eventType;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + " {" + "eventType='" + getEventType() + '\''
            + ", timestamp=" + timestamp + '}';
    }

}
