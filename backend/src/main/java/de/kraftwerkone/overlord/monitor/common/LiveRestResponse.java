package de.kraftwerkone.overlord.monitor.common;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Der Block {@code liveRest} einer Antwort: ob der Verkehr seit dem letzten Rollup-Lauf in den
 * Zahlen steckt ({@code docs/live-rest.md}).
 *
 * <p>Die Oberflaeche sagt bei {@link LiveRestZustand#AUSGESETZT}, dass die Zahlen unvollstaendig
 * sind — mit Zeitangabe, wenn es einen Lauf gab, sonst ohne. Bei den anderen beiden sagt sie
 * nichts.
 *
 * <p><b>Seit Teil B liegt der Block in {@code common}</b> (E-189): Prozessbaum und Dashboard tragen
 * denselben Block, und Fachpakete kennen einander nicht ({@code docs/PROJEKTBESCHREIBUNG.md} §6).
 * Ein zweites Record derselben Gestalt im Dashboard waere dieselbe Zuordnung an zwei Stellen — und
 * die Oberflaeche liest beide ueber denselben Baustein.
 *
 * @param zustand einer der drei Zustaende
 * @param vollstaendigBis <b>G</b> in UTC — nur bei {@code AUSGESETZT} mit vorhandenem Lauf, sonst
 *     {@code null}. Bis hierhin sind die Zahlen vollstaendig, ab hier fehlt Verkehr
 */
public record LiveRestResponse(LiveRestZustand zustand, Instant vollstaendigBis) {

  /** Der Block aus der Entscheidung; G aus der Wanduhrzeit der Quelle nach UTC gerechnet. */
  public static LiveRestResponse aus(LiveRestEntscheidung entscheidung, ZoneId zone) {
    return new LiveRestResponse(
        entscheidung.zustand(), Zeitpunkte.nachUtc(entscheidung.vollstaendigBis(), zone));
  }
}
