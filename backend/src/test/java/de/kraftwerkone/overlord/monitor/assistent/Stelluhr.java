package de.kraftwerkone.overlord.monitor.assistent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Eine Uhr, die nur geht, wenn der Test sie stellt (Regel T1: keine Wanduhrzeit im Test). */
final class Stelluhr extends Clock {

  private Instant jetzt = Instant.parse("2026-10-16T08:00:00Z");

  void vor(Duration dauer) {
    jetzt = jetzt.plus(dauer);
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return this;
  }

  @Override
  public Instant instant() {
    return jetzt;
  }
}
