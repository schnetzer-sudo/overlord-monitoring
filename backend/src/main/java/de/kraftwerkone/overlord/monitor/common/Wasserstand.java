package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <b>Der Wasserstand des Rollups</b>: bis wohin ist gerechnet? — als Schnittstelle, damit Tests ihn
 * setzen koennen, ohne in {@code rollup_lauf} zu schreiben.
 *
 * <p>Der Wert ist {@code MAX(fenster_bis)} ueber die abgeschlossenen, fehlerfreien Laeufe ({@code
 * docs/rollup.md} §2) — <b>Datenzeit der Anwendungsuhr</b>, ein voller Stundenanfang und
 * <b>ausschliessend</b>: Der Eimer {@code wasserstand - 1 h} ist der, in dem der Lauf lief, und er
 * ist nur bis zum Laufzeitpunkt gerechnet. {@code gestartet_am} und {@code beendet_am} derselben
 * Zeile stammen aus der Systemuhr und gehen in keine Rechnung ein ({@code docs/rollup.md} §4).
 *
 * <p><i>(seit 17.09.2026, Live-Rest Teil A.)</i> Bis dahin las die Fensterverengung der
 * Nachrichtenliste den Wasserstand an eigener Stelle ({@code message/VerengungRepository}). Mit dem
 * Live-Rest bekommt er einen zweiten Verbraucher ausserhalb von {@code message}, und Fachpakete
 * kennen einander nicht — er liegt deshalb hier ({@code docs/live-rest.md}). Der Rollup-Job selbst
 * liest ihn weiterhin an eigener Stelle ({@code rollup/RollupSchreibRepository.wasserstand()},
 * ueber den Schreib-Kontext); das ist gemeldet und nicht nebenbei zusammengelegt.
 *
 * @see WasserstandRepository die eine lesende Stelle
 */
@FunctionalInterface
public interface Wasserstand {

  /** Leer, wenn noch nie ein Lauf abgeschlossen und fehlerfrei war. */
  Optional<LocalDateTime> wasserstand();
}
