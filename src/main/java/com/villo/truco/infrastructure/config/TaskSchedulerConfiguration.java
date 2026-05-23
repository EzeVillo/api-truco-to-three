package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(TimeoutSchedulerProperties.class)
public class TaskSchedulerConfiguration {

  private final TimeoutSchedulerProperties properties;

  public TaskSchedulerConfiguration(final TimeoutSchedulerProperties properties) {

    this.properties = properties;
  }

  @Bean
  TaskScheduler timeoutTaskScheduler() {

    final var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(this.properties.getScheduler().getPoolSize());
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setThreadNamePrefix("truco-timeout-");
    return scheduler;
  }

}
