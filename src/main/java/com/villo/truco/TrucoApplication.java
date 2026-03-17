package com.villo.truco;

import com.villo.truco.infrastructure.config.MatchTimeoutProperties;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MatchTimeoutProperties.class)
public class TrucoApplication {

  public static void main(String[] args) {

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(TrucoApplication.class, args);
  }

}
