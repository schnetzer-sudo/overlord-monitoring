package de.kraftwerkone.overlord.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Anwendung.
 *
 * <p>Diese Klasse liegt bewusst direkt im Basispaket {@code de.kraftwerkone.overlord.monitor},
 * damit der Component-Scan die gesamte Anwendung erfasst. Sie wird nicht verschoben.
 */
@SpringBootApplication
public class OverlordMonitorApplication {

  public static void main(String[] args) {
    SpringApplication.run(OverlordMonitorApplication.class, args);
  }
}
