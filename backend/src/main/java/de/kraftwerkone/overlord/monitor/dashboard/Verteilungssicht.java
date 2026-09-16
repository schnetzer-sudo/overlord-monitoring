package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Wonach der Verteilungsblock gruppiert — <b>ein Block, zwei Sichten</b>.
 *
 * <p>Beide laufen ueber <b>dasselbe Statement</b>, nur mit einer anderen Spalte im Ausdruck. Das
 * ist kein Zufall, sondern die Bedingung dafuer, dass die zweite Sicht nichts kostet, was die erste
 * nicht schon kostet: Der teure Teil ist der Bereichszugriff auf die Rolluptabelle, und der ist
 * derselbe.
 *
 * <p><b>Beide bekommen den {@code pflegestatus}-Riegel</b> (Entscheidung E-i). Bei der Richtung ist
 * er heute folgenlos — nach der Kuratierung tragen alle Zeilen mit Richtung {@code GEPFLEGT} —,
 * aber die Regel ist E-i und nicht der Zufall dieses Katalogstands.
 *
 * <h2>⚠️ Kein Anfrageparameter mehr (16.09.2026)</h2>
 *
 * <p>Bis zum 16.09.2026 kam die Sicht als {@code ?verteilung=} in die Anfrage, mit der Vorgabe
 * {@code PARTNER} und einem {@code 400} fuer einen unbekannten Wert. <b>Seither traegt jede Antwort
 * beide Sichten</b>, und der Umschalter wechselt allein im Browser ({@code docs/dashboard.md} §4).
 * Der Parameter ist damit wirkungslos wie {@code ?mandant=}; die Vorgabe {@code PARTNER} ist eine
 * Frage der Anzeige und steht im Frontend. Diese Aufzaehlung waehlt nur noch die Katalogspalte des
 * Statements.
 */
public enum Verteilungssicht {

  /** Nach kuratiertem Partner ({@code process_catalog.partner}). */
  PARTNER,

  /** Nach kuratierter Richtung ({@code process_catalog.richtung}). */
  RICHTUNG
}
