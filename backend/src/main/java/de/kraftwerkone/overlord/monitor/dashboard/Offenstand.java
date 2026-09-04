package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;

/**
 * Was <b>ein</b> Statement der Kacheln <i>Laeuft</i> und <i>Wartend</i> zurueckbringt — zwei Werte
 * aus einem Lesevorgang.
 *
 * <p><b>Zwei Werte und ein Statement, nicht zwei.</b> {@code COUNT(*)} und {@code MIN(...)} lesen
 * denselben Indexbereich; ein zweites Statement fuer das Alter kostete denselben Bereich ein
 * zweites Mal und brauchte dazu einen zweiten Verbindungsgriff.
 *
 * <p>Der Zeitpunkt ist <b>roh</b>, so wie er in {@code GlassfishDB} steht — als Wanduhrzeit des
 * Servers ({@code docs/datenzugriff.md} §7). Die Umrechnung in eine Dauer gegen die Anwendungsuhr
 * macht {@code DashboardService}, nicht das Repository: <b>Ein Repository liest keine Uhr.</b>
 *
 * @param anzahl wie viele Zeilen der Mandant in diesem Rohstatus hat
 * @param aelteste {@code MIN(MessageLastUpdate)} dieser Zeilen — <b>{@code null} bei {@code anzahl
 *     = 0}</b>, weil {@code MIN} ueber eine leere Menge {@code NULL} ist
 */
record Offenstand(long anzahl, LocalDateTime aelteste) {}
