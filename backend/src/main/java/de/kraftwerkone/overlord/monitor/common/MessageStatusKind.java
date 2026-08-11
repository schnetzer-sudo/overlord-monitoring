package de.kraftwerkone.overlord.monitor.common;

/**
 * Fachliche Einordnung eines {@code MessageStatus}. Die Zuordnung von Rohwert zu Einordnung
 * entsteht ausschliesslich im {@link MessageStatusClassifier} und wird nirgends nachgebaut.
 *
 * <p>{@code UNGEKLAERT} ist der Auffang: unbekannte oder fachlich nicht verifizierte Statuswerte
 * werden niemals in einen geratenen Wert gedraengt.
 *
 * <p><b>{@code ZWISCHENSCHRITT} ist am 11.08.2026 in zwei Werte zerfallen</b> (Schritt 6, Teil 2a):
 * {@code AUFGETEILT} ({@code SPLITTED}) und {@code ZUSAMMENGEFUEHRT} ({@code MERGED}). Technisch
 * waren beide dasselbe — fuer den Nutzer bedeuten sie Gegenteiliges: <i>aus eins wurde viel</i>
 * gegen <i>aus viel wurde eins</i>. Ein gemeinsamer Eimer verschluckt genau den Unterschied, den
 * die Verkettung sichtbar macht ({@code docs/verkettung.md} §1).
 *
 * <p>An der Ueberfaelligkeitsrechnung aendert das <b>nichts</b>: Beide bleiben Endstatus ({@link
 * MessageStatusClassifier#istEndstatus}).
 */
public enum MessageStatusKind {
  FEHLER,
  WARTEND,
  LAEUFT,
  AUFGETEILT,
  ZUSAMMENGEFUEHRT,
  ABGESCHLOSSEN,
  QUITTIERT,
  UNGEKLAERT
}
