package de.kraftwerkone.overlord.monitor.payload;

/**
 * Der Zustand eines Abrufs — die vier benannten Faelle aus {@code docs/rohdaten.md} §8, plus der
 * Regelfall.
 *
 * <p><b>Keiner davon ist ein leeres Feld.</b> Jeder bekommt in der Oberflaeche einen eigenen Text;
 * das Backend liefert den Schluessel und deutet ihn nicht. Die Trennung ist der Punkt: „Datei weg"
 * und „Ablage aus" sehen fuer den Nutzer gleich aus und sind fuer den Betrieb voellig verschiedene
 * Lagen.
 */
public enum Artefaktzustand {

  /** Regelfall: die Datei ist da, ist Text und wird angezeigt. */
  ANZEIGBAR,

  /**
   * Nullbytes bzw. Anteil druckbarer Zeichen unter der Schwelle. Betrifft <b>18,2 % der
   * Nutzdateien</b> und 0 % der Protokolle (M61). Das Altsystem zeigt hier Zeichenmuell, der wie
   * ein Fehler aussieht; wir benennen den Fall und zeigen ihn nicht an. Der Download bleibt
   * moeglich.
   */
  BINAERDATEI,

  /**
   * Der Beschnitt greift, aber es gibt kein vollstaendiges Markenpaar. Betrifft die haeufigste
   * Familie: {@code HTTPSender} traegt in 30 von 30 Faellen keine Marken, {@code FTPSender} in 28
   * von 30 (M63) — und {@code FTPSender} haengt an rund 69 % der Nachrichten. Der Fall ist damit
   * <b>Normalzustand und kein Ausfall</b> und darf nicht wie einer aussehen.
   */
  KEIN_ANZEIGBARER_PROTOKOLLTEIL,

  /**
   * Die Ablage antwortet, liefert aber nichts — {@code Error (Skipped)} im Rumpf, nicht im
   * HTTP-Status (M68, M66 (2)). Produktiv ein Fehlerzustand; <b>in der Entwicklung der
   * Normalfall</b>, weil 63,2 % des Bestands vor der Rotationsgrenze liegen und die zugehoerigen
   * Ablagen aus sind.
   */
  DATEI_NICHT_VORHANDEN,

  /**
   * Die Kennung loest nicht auf, oder der Knoten antwortet nicht. <b>Etwas anderes als „Datei
   * weg"</b> — und fuer den Betrieb die wichtigere Unterscheidung. Ein Rueckfall auf eine andere
   * Ablage gibt es nicht: 20 von 20 Kreuzabrufen scheitern, die Ablagen sind keine Spiegel (M68).
   */
  ABLAGE_NICHT_ERREICHBAR
}
