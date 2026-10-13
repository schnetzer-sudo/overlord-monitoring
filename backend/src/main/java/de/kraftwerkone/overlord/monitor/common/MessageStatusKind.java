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
 *
 * <p><b>{@code QUITTUNG_AUSSTEHEND} ist am 13.10.2026 dazugekommen</b> (E‑234, {@code
 * EERP_PENDING}): uebertragen, die Empfangsbestaetigung des Partners steht aus — das Gegenstueck zu
 * {@code QUITTIERT}. <b>Ausdruecklich nicht {@code WARTEND}:</b> „wartet bei uns" und „wartet auf
 * den Partner" sind fuer den Nutzer verschiedene Antworten. Der einzige Name mit Unterstrich in
 * dieser Aufzaehlung; die naechsten Einwortformen waeren schlechter ({@code UNQUITTIERT} kollidiert
 * mit der Problemkategorie <i>Unquittiert</i>, {@code AUSSTEHEND} verliert, was aussteht).
 */
public enum MessageStatusKind {
  FEHLER,
  WARTEND,
  LAEUFT,
  AUFGETEILT,
  ZUSAMMENGEFUEHRT,
  ABGESCHLOSSEN,
  QUITTIERT,
  QUITTUNG_AUSSTEHEND,
  UNGEKLAERT
}
