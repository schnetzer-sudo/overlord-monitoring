package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.common.LiveRestZustand;
import java.time.Instant;

/**
 * Der Block {@code liveRest} der Antwort: ob der Verkehr seit dem letzten Rollup-Lauf in den Zahlen
 * steckt ({@code docs/live-rest.md}).
 *
 * <p>Die Oberflaeche sagt bei {@link LiveRestZustand#AUSGESETZT}, dass die Zahlen unvollstaendig
 * sind — mit Zeitangabe, wenn es einen Lauf gab, sonst ohne. Bei den anderen beiden sagt sie
 * nichts.
 *
 * @param zustand einer der drei Zustaende
 * @param vollstaendigBis <b>G</b> in UTC — nur bei {@code AUSGESETZT} mit vorhandenem Lauf, sonst
 *     {@code null}. Bis hierhin sind die Zahlen vollstaendig, ab hier fehlt Verkehr
 */
public record LiveRestResponse(LiveRestZustand zustand, Instant vollstaendigBis) {}
