package com.villo.truco.profile.application.events;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;

public record ProfileEventNotification(List<PlayerId> recipients, String eventType, long timestamp,
                                       Map<String, Object> payload) implements ApplicationEvent {

}
