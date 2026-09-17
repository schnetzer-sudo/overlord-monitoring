package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;

/**
 * Eine Zeile im Live-Bereich — dieselbe Gestalt wie eine Zeile von {@code message_rollup}: je
 * Stundeneimer, Prozess und <b>Rohstatus</b> eine Anzahl.
 *
 * <p>Rohstatus und nicht Einordnung (Entscheidung E-g in {@code docs/rollup.md} §2): Was Fehler
 * ist, entscheidet {@code MessageStatusClassifier} beim Lesen — im Verbraucher, nicht hier.
 *
 * <p>In einer {@link LiveRestKorrektur} ist {@code anzahl} <b>vorzeichenbehaftet</b>: negativ fuer
 * das, was aus dem Rollup abgezogen wird, positiv fuer das, was die Zaehlung aus der Quelle
 * dazugibt.
 *
 * @param stunde der Stundeneimer, Wanduhrzeit des Quellservers
 * @param processId die {@code ProcessID}
 * @param messageStatus der Rohwert, etwa {@code FINISHED} oder {@code ERROR_TIMEOUT}
 * @param anzahl die Anzahl — in einer Korrektur mit Vorzeichen
 */
public record LiveRestZeile(
    LocalDateTime stunde, String processId, String messageStatus, long anzahl) {

  public LiveRestZeile {
    if (stunde == null || processId == null) {
      throw new IllegalArgumentException("Eine Live-Rest-Zeile braucht Stunde und Prozess");
    }
  }
}
