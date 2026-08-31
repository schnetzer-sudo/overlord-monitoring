package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Die Kachel <i>Ueberfaellig</i> (Block 4) — <b>zwei Zahlen, beide live</b>.
 *
 * <h2>Die erste benannte Ausnahme von Leistungsregel L2</h2>
 *
 * <p>L2 sagt: „Dashboard-Kennzahlen kommen ausschliesslich aus {@code message_rollup}." <b>Das Wort
 * bleibt stehen, und daneben steht diese Ausnahme</b> (E-c vom 24.08.2026, {@code
 * PROJEKTBESCHREIBUNG.md} §8). Sie ist einzeln begruendet und einzeln gemessen: 2,275 ms fuer die
 * erste Zahl, 4,275 ms fuer die zweite (M90).
 *
 * <h2>Und es sind die einzigen beiden Felder, die „nicht ermittelbar" sein duerfen</h2>
 *
 * <p>Sie sind der einzige Teil der Antwort, der zur Laufzeit auf der <b>Produktion</b> liest, wo
 * {@code max_statement_time} nach zehn Sekunden abraeumt. Der Rest kommt aus unserer eigenen
 * Tabelle. <b>Stirbt die Live-Abfrage, darf nicht die ganze Seite sterben</b> — dann sind beide
 * Zahlen {@code null} und {@link #ermittelbar} ist {@code false}.
 *
 * <p><b>Kein allgemeiner Teilerfolg-Mechanismus.</b> Genau diese zwei Felder, und kein anderer
 * Block bekommt einen. Ein Dashboard, das jeden Block einzeln scheitern lassen kann, zeigt
 * irgendwann eine Seite voller Luecken und nennt das eine Antwort.
 *
 * <p><b>Und die beiden Zahlen fallen zusammen.</b> Faellt eine, ist auch die andere {@code null} —
 * sie stehen als <i>Paar</i> nebeneinander („im Zeitraum" gegen „insgesamt"), und eine Kachel, in
 * der eine Zahl steht und die andere fehlt, laedt zu genau der Rechnung ein, die dann nicht
 * aufgeht.
 *
 * @param imFenster ueberfaellig innerhalb des gezeigten Zeitraums (E-h) — dieselbe Zahl, die der
 *     Klick in die Liste liefert. {@code null}, wenn nicht ermittelbar
 * @param insgesamt ueberfaellig ohne Zeitfenster. {@code null}, wenn nicht ermittelbar
 * @param ermittelbar {@code false} heisst: Die Live-Abfrage ist an der Zeitgrenze abgebrochen. Das
 *     ist <b>nicht</b> dasselbe wie „null ueberfaellige Nachrichten", und genau deshalb steht es
 *     als eigenes Feld da und nicht als Null
 */
public record UeberfaelligkachelResponse(Long imFenster, Long insgesamt, boolean ermittelbar) {

  /** Beide Zahlen liegen vor. */
  static UeberfaelligkachelResponse von(long imFenster, long insgesamt) {
    return new UeberfaelligkachelResponse(imFenster, insgesamt, true);
  }

  /** Die Live-Abfrage ist abgebrochen — die Seite steht trotzdem. */
  static UeberfaelligkachelResponse nichtErmittelbar() {
    return new UeberfaelligkachelResponse(null, null, false);
  }
}
