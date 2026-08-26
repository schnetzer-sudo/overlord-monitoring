package de.kraftwerkone.overlord.monitor.rollup;

/**
 * Die beiden Laufarten. Sie unterscheiden sich <b>ausschliesslich im Fenster</b> — beide rufen
 * dieselbe Methode {@code RollupJob.fuehreAus(von, bis, art)} mit demselben Code auf.
 *
 * <p><b>Zwei getrennte Abfragen zu schreiben ist ausdruecklich verboten.</b> Sie liefen
 * auseinander, und der Nachtlauf aenderte dann still die Zahlen, die tagsueber jemand gesehen hat.
 * Die Art steht nur deshalb in der Protokollzeile, damit im Nachhinein ablesbar ist, welcher Lauf
 * ein Fenster zuletzt gerechnet hat.
 *
 * <p>Der Wert wird als Zeichenkette in {@code rollup_lauf.art} abgelegt (VARCHAR(10), kein ENUM in
 * der Datenbank — dieselbe Entscheidung wie bei {@code audit_log.event_type} und {@code
 * process_catalog.richtung}).
 */
public enum LaufArt {

  /**
   * Der stuendliche Lauf: vom Wasserstand minus Nachlauffenster bis jetzt.
   *
   * <p>Gemessen kostet er <b>88,167 ms</b> in der dichtesten Stunde des gesamten Bestands (8.630
   * Zeilen) und <b>3,190 ms</b> in der letzten Stunde (285 Zeilen) — M88.
   */
  DELTA,

  /**
   * Der naechtliche Lauf: vom fruehesten {@code MessageLastUpdate} bis jetzt.
   *
   * <p>Er rechnet denselben Bestand noch einmal von vorn. Sein Zweck ist nicht Geschwindigkeit,
   * sondern <b>Selbstheilung</b>: Ein Delta-Lauf, der einen Eimer verfehlt hat — weil eine Zeile
   * spaeter als das Nachlauffenster nachgeschrieben wurde oder weil ein Lauf abgebrochen ist —,
   * wird hier ohne Zutun berichtigt.
   */
  VOLL
}
