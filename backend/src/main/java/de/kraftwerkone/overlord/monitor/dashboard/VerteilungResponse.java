package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Der Verteilungsblock (Block 5) — <b>beide Sichten in einer Antwort</b>.
 *
 * <h2>Warum beide und nicht die gewaehlte</h2>
 *
 * <p>Bis zum 16.09.2026 trug die Antwort genau eine Sicht, gewaehlt ueber {@code ?verteilung=}. Ein
 * Wechsel war damit ein neuer Aufruf der <b>ganzen</b> Landingpage — und weil die Oberflaeche fuer
 * den neuen Abfrageschluessel keine Daten hatte, baute sie jeden Block neu auf, nicht nur diesen.
 * <b>Jetzt steht beides hier</b>, und der Umschalter zeigt die andere Haelfte einer Antwort, die
 * schon da ist ({@code docs/dashboard.md} §4). „Ein Aufruf, eine Antwort" bleibt bestehen; die
 * Antwort ist nur vollstaendiger geworden.
 *
 * <p><b>Die Sicht steht nicht mehr als Feld darin</b>, sondern ist der Schluessel: {@code partner}
 * und {@code richtung}. Das Feld {@code sicht} sagte, welche Sicht geliefert worden war — seit
 * beide geliefert werden, gibt es darauf keine Antwort mehr.
 *
 * <p>Je Sicht gelten die Regeln unveraendert: Top 10, dann {@code UEBRIGE} (falls es einen Rang 11
 * gibt), dann immer {@code NICHT_ZUGEORDNET} — siehe {@link VerteilungszeilenResponse}.
 *
 * @param partner die Zeilen nach kuratiertem Partner
 * @param richtung die Zeilen nach kuratierter Richtung
 */
public record VerteilungResponse(
    VerteilungszeilenResponse partner, VerteilungszeilenResponse richtung) {}
