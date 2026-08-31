package de.kraftwerkone.overlord.monitor.rollup;

/**
 * Wie viele Rollup-Eimer <b>unterhalb des Bestandsanfangs</b> stehen — je Ebene gezaehlt.
 *
 * <p><b>Das ist ein Befund und kein Fehler</b> (offener Punkt 54, entschieden am 27.08.2026).
 * Entfernt das Altsystem alte Nachrichten, wandert {@code MIN(Message.MessageLastUpdate)} nach
 * vorn. Die untere Grenze <b>beider</b> Laufarten kommt aus dem Quellbestand, und geloescht wird
 * ausschliesslich innerhalb des Fensters — die Eimer davor sind fuer den Kalender damit
 * unerreichbar und bleiben fuer immer stehen.
 *
 * <p><b>Sie werden nicht geloescht.</b> Hat das Altsystem die Nachrichten entfernt, ist der Rollup
 * die einzige Stelle im Haus, an der noch steht, wie viel in jenem Zeitraum gelaufen ist; ein
 * Monitoring, das seine eigene Geschichte wegraeumt, sobald die Quelle sie vergisst, raeumt genau
 * das weg, wofuer es gebaut wurde. Loeschen waere ausserdem unumkehrbar und vollzoege eine
 * Aufbewahrungsfrist, die der Job gar nicht kennt.
 *
 * <p><b>Der Schaden aus Punkt 54 ist nicht, dass die Zeilen dastehen, sondern dass es niemand
 * merkt.</b> Genau das behebt dieser Wert: {@code SUM(anzahl)} laeuft danach gegen die Zeilenzahl
 * von {@code Message} auseinander, und die Summenprobe gilt nur noch ueber den
 * <b>ueberlappenden</b> Bereich ({@link RollupSchreibRepository#ueberlappenderBereich}). Erkannt
 * und benannt ist derselbe Zustand eine Auskunft statt eines Fehlers.
 *
 * <p><b>Alle drei Ebenen, nicht nur die Stundenebene</b> (offener Punkt 68). Die abgeleiteten
 * Ebenen frieren mit ein, weil ihre Rechenbereiche ebenfalls aus dem Fenster kommen. Ein
 * Erkennungsweg, der nur eine Ebene prueft, meldete eine Abweichung nur fuer eine von dreien.
 *
 * @param stundeneimer Zeilen in {@code message_rollup} mit {@code stunde} vor dem Bestandsanfang
 * @param tageseimer Zeilen in {@code message_rollup_tag} vor dem <b>Kalendertag</b> des
 *     Bestandsanfangs. Der Tag des Bestandsanfangs selbst zaehlt nicht mit — ihn rechnet jeder
 *     Volllauf neu, weil {@code RollupFenster.betroffeneTage} ihn einschliesst
 * @param monatseimer Zeilen in {@code message_rollup_monat} vor dem <b>Kalendermonat</b> des
 *     Bestandsanfangs, aus demselben Grund
 */
public record EingefroreneEimer(long stundeneimer, long tageseimer, long monatseimer) {

  /** Keine einzige Zeile unterhalb des Bestandsanfangs — der Normalfall. */
  public static final EingefroreneEimer KEINE = new EingefroreneEimer(0, 0, 0);

  /** Ist der Fall aus Punkt 54 eingetreten? */
  public boolean vorhanden() {
    return stundeneimer > 0 || tageseimer > 0 || monatseimer > 0;
  }
}
