package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Wie eine Ablage dasteht — <b>das Ergebnis einer Pruefung und nicht der Inhalt einer Spalte</b>.
 *
 * <p>Dieselben drei Werte tragen ein einzelnes Ziel und die zusammenfassende Kachel. Die
 * Zusammenfassung ist in {@link Ablagenkachel} beschrieben: ein nicht erreichbares Ziel schlaegt
 * durch, sonst ein ungeklaertes, sonst ist alles erreichbar.
 *
 * <h2>Warum die Werte nicht aus {@code ServiceStatus} kommen</h2>
 *
 * <p><b>Ablagen senden keinen Heartbeat</b> (Auskunft des Auftraggebers vom 10.09.2026, nicht
 * gemessen), und M52 zeigt fuer alle elf denselben eingefrorenen Stand {@code HEARTBEAT} vom
 * 07.06.2012. Ihr {@code ServiceStatus} ist damit keine Auskunft ueber ihre Erreichbarkeit, sondern
 * eine Karteileiche — deshalb wird gefragt statt abgelesen (E‑119, E‑120).
 */
public enum Ablagenzustand {

  /**
   * Die Ablage hat geantwortet.
   *
   * <p>Gemessen: Auf die Null-UUID antwortet sie mit {@code Error (Skipped)} im Rumpf und ohne
   * Anhang, was der Abrufweg als {@code DATEI_NICHT_VORHANDEN} einordnet (M174, {@code
   * docs/dienste.md} §4). <b>„Datei nicht vorhanden" heisst hier also: der Knoten lebt</b> — genau
   * das ist die Frage der Kachel.
   */
  ERREICHBAR,

  /**
   * Die Ablage antwortet nicht, oder ihre Kennung loest auf keine Zeile in {@code Service} auf.
   *
   * <p>Beides ist derselbe Zustand, und zwar aus demselben Grund wie beim Rohdatenabruf ({@code
   * docs/rohdaten-backend.md} §3): Es gibt keinen Weg zur Datei. <b>Kein Rueckfall auf eine andere
   * Ablage</b> — 20 von 20 Kreuzabrufen scheitern, die Ablagen sind keine Spiegel (M68).
   */
  NICHT_ERREICHBAR,

  /**
   * Die Ablage hat auf die Null-UUID <b>Daten geliefert</b> — oder es fehlt der Beleg, dass
   * ueberhaupt geprueft worden ist.
   *
   * <p><b>Der erste Fall ist der unerwartete</b>, und deshalb hat er einen eigenen Zustand statt
   * eines gruenen Ergebnisses: Die Null-UUID zeigt mit Sicherheit auf keine Datei; kommt trotzdem
   * eine, verhaelt sich der Knoten anders als gemessen, und das gehoert gezeigt statt eingeordnet.
   *
   * <p>Der zweite Fall traegt einen benannten {@link Ablagengrund}.
   */
  UNGEKLAERT
}
