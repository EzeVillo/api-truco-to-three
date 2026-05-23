package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "truco.timeout")
public class TimeoutSchedulerProperties {

  private Scheduler scheduler = new Scheduler();

  public Scheduler getScheduler() {

    return scheduler;
  }

  public void setScheduler(Scheduler scheduler) {

    this.scheduler = scheduler;
  }

  public static class Scheduler {

    private int poolSize = 4;

    public int getPoolSize() {

      return poolSize;
    }

    public void setPoolSize(int poolSize) {

      this.poolSize = poolSize;
    }

  }

}
