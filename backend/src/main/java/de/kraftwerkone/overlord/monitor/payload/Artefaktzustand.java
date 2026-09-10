package de.kraftwerkone.overlord.monitor.payload;

import de.kraftwerkone.overlord.monitor.common.Abrufzustand;

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
  ABLAGE_NICHT_ERREICHBAR;

  /**
   * Der Zustand, den der <b>Transport</b> gemeldet hat, in dieser Menge.
   *
   * <p><b>Die Abbildung steht hier und nicht in {@code common}</b>: {@link Abrufzustand} kennt drei
   * Faelle, diese Aufzaehlung fuenf — die beiden zusaetzlichen ({@link #BINAERDATEI}, {@link
   * #KEIN_ANZEIGBARER_PROTOKOLLTEIL}) entstehen erst nach dem Abruf, bei der Binaerpruefung und
   * beim Beschnitt. Wer die engere Menge in die weitere uebersetzt, ist der, der die weitere kennt;
   * {@code common} kennt {@code payload} nicht.
   *
   * <p><b>{@link Abrufzustand#GELIEFERT} wird zu {@link #ANZEIGBAR}</b>, und das ist an dieser
   * Stelle noch keine Zusage: Ob die gelieferten Bytes tatsaechlich anzeigbar sind, entscheidet
   * erst {@code ArtefaktService}. Der Aufrufer benutzt diesen Zweig deshalb ausschliesslich fuer
   * den Fehlschlag — bei Erfolg geht es mit den Bytes weiter und nicht mit dem Zustand.
   */
  public static Artefaktzustand aus(Abrufzustand zustand) {
    return switch (zustand) {
      case GELIEFERT -> ANZEIGBAR;
      case DATEI_NICHT_VORHANDEN -> DATEI_NICHT_VORHANDEN;
      case ABLAGE_NICHT_ERREICHBAR -> ABLAGE_NICHT_ERREICHBAR;
    };
  }
}
