package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Duration;

/**
 * Was ein Lauf getan hat. Es ist dasselbe, was in {@code rollup_lauf} steht — der Rueckgabewert
 * erspart dem Aufrufer nur, die Zeile noch einmal zu lesen.
 *
 * @param laufId die Kennung der Protokollzeile in {@code rollup_lauf}
 * @param art {@link LaufArt#DELTA} oder {@link LaufArt#VOLL}
 * @param fenster der verarbeitete Zeitschnitt, in <b>Datenzeit</b>
 * @param scheiben in wie viele Monatsscheiben das Fenster zerlegt wurde — bei einem Delta-Lauf
 *     eine, beim Volllauf ueber den Bestand der Testkopie 22
 * @param zeilenGeschrieben wie viele Zeilen in {@code message_rollup} stehen. <b>Nicht</b> die Zahl
 *     der gelesenen Nachrichten: Auf der Testkopie verdichtet der Rollup um Faktor 9,96 (M87)
 * @param nachrichten wie viele Nachrichten dahinterstehen, also {@code SUM(anzahl)}. Sie ist die
 *     Groesse, gegen die sich die Summenprobe fuehren laesst — ueber den Gesamtbestand 3.341.519
 * @param dauer Laufzeit von {@code gestartet_am} bis {@code beendet_am}, beide aus {@code
 *     systemClock}
 */
public record RollupErgebnis(
    long laufId,
    LaufArt art,
    RollupFenster fenster,
    int scheiben,
    int zeilenGeschrieben,
    long nachrichten,
    Duration dauer) {}
