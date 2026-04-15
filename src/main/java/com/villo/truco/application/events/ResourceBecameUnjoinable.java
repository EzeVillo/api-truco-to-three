package com.villo.truco.application.events;

public record ResourceBecameUnjoinable(String targetType, String targetId) implements
    ApplicationEvent {

}
