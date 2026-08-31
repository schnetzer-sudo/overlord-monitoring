package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Warum eine Nachricht in „Zuletzt aufgefallen" steht.
 *
 * <p><b>Die beiden schliessen einander aus, und das ist kein Zufall:</b> <i>Ueberfaellig</i> setzt
 * voraus, dass die Nachricht <b>nicht</b> in einem Endstatus ist, und <i>Fehler</i> ist einer
 * ({@code MessageStatusClassifier.istEndstatus}). Eine Zeile kann deshalb nie beides sein — die
 * Kategorie ist eindeutig und keine Auswahl unter mehreren zutreffenden.
 *
 * <p>Sie werden trotzdem <b>getrennt benannt und nie zu „Problem" zusammengefasst</b> (Regel Q3):
 * Ein Fehler ist passiert, eine ueberfaellige Nachricht haengt. Das sind zwei verschiedene
 * Handlungen.
 */
public enum Auffaelligkeit {

  /** {@code MessageStatus} beginnt mit {@code ERROR_} oder ist {@code COMMIT_REJECTED}. */
  FEHLER,

  /** Nicht in einem Endstatus, und {@code MessageLastUpdate + MessageTimeout} ist verstrichen. */
  UEBERFAELLIG
}
