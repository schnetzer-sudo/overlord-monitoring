package de.kraftwerkone.overlord.monitor.common.auftrag;

/**
 * Die stabilen Codes der Pruefung eines Auftrags ({@code docs/tasks.md} §5.3, §6.2). Der Klartext
 * daneben darf sich aendern, der Code nicht: Der Assistent (A2) baut seine Rueckfragen auf ihm auf
 * und beurteilt Werte nicht selbst.
 */
public enum Fehlercode {
  /** Die Aktion ist keine der Aktionen der Definition. */
  AKTION_UNBEKANNT,
  /** Der Auftrag hat keinen Eintrag. */
  ZU_WENIGE_EINTRAEGE,
  /** Der Auftrag hat mehr Eintraege als {@code maxEintraege}. */
  ZU_VIELE_EINTRAEGE,
  /** Ein Eintrag nennt ein Feld, das die Definition nicht kennt. */
  FELD_UNBEKANNT,
  /** Ein Feld traegt einen Wert bei einer Aktion ausserhalb seines {@code nurBei}. */
  FELD_NICHT_ERLAUBT,
  /** Ein Pflichtfeld hat bei einer Aktion, bei der es vorkommt, keinen Wert. */
  PFLICHT_FEHLT,
  /** Ein Wert passt nicht auf sein Muster — oder die {@code anfrageId} ist keine UUID. */
  FORMAT,
  /** Ein Wert enthaelt einen Umbruch, ein Steuerzeichen oder ein Trennzeichen (E‑1022). */
  UNZULAESSIGES_ZEICHEN,
  /** Regel {@code mindestensEins}: keines der Felder hat einen Wert. */
  MINDESTENS_EINS,
  /** Regel {@code eindeutig}: zwei Eintraege tragen dieselben Werte. */
  DOPPELT,
  /** Der Parametertext waere laenger als die Spalte {@code task.parameter} (65.535 Bytes). */
  ZU_LANG
}
