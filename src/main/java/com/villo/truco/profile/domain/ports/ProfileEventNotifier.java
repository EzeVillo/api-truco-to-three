package com.villo.truco.profile.domain.ports;

import com.villo.truco.profile.domain.model.events.AchievementUnlocked;
import java.util.List;

public interface ProfileEventNotifier {

  void publishDomainEvents(List<? extends AchievementUnlocked> events);
}
