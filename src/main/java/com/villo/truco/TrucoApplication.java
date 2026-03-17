package com.villo.truco;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrucoApplication {

  public static void main(String[] args) {

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(TrucoApplication.class, args);
  }

}
