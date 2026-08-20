package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Woher der <b>Partner</b>vorschlag einer Zeile stammt.
 *
 * <p><b>Sie beschreibt die Herkunft des Partners, nicht die der Richtung.</b> Das ist keine
 * Nachlaessigkeit, sondern die Zaehlweise von {@code docs/prozess-katalog.md} §3.5: Dort stehen
 * Regel A mit 887, Regel B mit 281 und 322 „ohne Vorschlag" — und die 224 NEXANS-Prozesse unter den
 * 322 haengen sehr wohl an einem Projekt mit Richtungsanker ({@code 300_KundenEingehend}). Eine
 * Zeile darf also {@code KEINE} tragen und trotzdem eine Richtung haben.
 *
 * <p>{@code KEINE} heisst <b>„geprueft, nichts abgeleitet"</b> und nicht „noch nicht gelaufen".
 * Nach Regel Q4 ist das ein Ergebnis und kein fehlender Wert.
 */
public enum VorschlagHerkunft {
  /** Nummernpraefix und Position: Token 2 der {@code ProcessID}. */
  REGEL_A,
  /** CamelCase mit Richtungsanker: was hinter {@code Eingehend}/{@code Ausgehend} steht. */
  REGEL_B,
  /** Keine Regel hat getroffen. Das Feld bleibt leer (Regel Q4). */
  KEINE
}
