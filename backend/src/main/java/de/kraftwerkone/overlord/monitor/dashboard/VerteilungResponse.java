package de.kraftwerkone.overlord.monitor.dashboard;

import java.util.List;

/**
 * Der Verteilungsblock (Block 5) — <b>Top 10 und zwei Restzeilen</b>.
 *
 * <h2>Die Reihenfolge ist Teil der Aussage</h2>
 *
 * <p>Zuerst die benannten Werte, absteigend nach Anzahl. <b>Danach, immer unten, die beiden
 * Restzeilen</b> — unabhaengig von ihrer Groesse. Bei {@code IBIS} waere „Übrige (40)" mit 27,92 %
 * sonst der groesste Balken des Blocks und stuende auf Rang 1, als gaebe es einen Partner dieses
 * Namens (M98, Befund 21).
 *
 * <p><b>„Nicht zugeordnet" ist keine Rangposition</b> und faellt nie in „Übrige". Deshalb sortiert
 * schon die Abfrage {@code ORDER BY (schluessel IS NULL), summe DESC}: Die Raenge 1…k gehoeren
 * lueckenlos den benannten Werten.
 *
 * @param sicht wonach gruppiert wurde — sie steht in der Antwort, damit die Oberflaeche den
 *     Umschalter richtig stellt, auch wenn der Parameter fehlte
 * @param zeilen Top 10, dann {@code UEBRIGE} (falls es einen Rang 11 gibt), dann immer {@code
 *     NICHT_ZUGEORDNET}
 */
public record VerteilungResponse(Verteilungssicht sicht, List<VerteilungszeileResponse> zeilen) {

  public VerteilungResponse {
    zeilen = List.copyOf(zeilen);
  }
}
