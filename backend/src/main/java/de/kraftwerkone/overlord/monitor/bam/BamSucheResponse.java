package de.kraftwerkone.overlord.monitor.bam;

import java.time.Instant;
import java.util.List;

/**
 * Die Antwort der BAM-Suche.
 *
 * <p><b>Keine Cursor-Hülle</b> ({@code items}/{@code nextCursor}/{@code hasMore}) — es gibt hier
 * nichts zu blättern. Die Suche liefert höchstens {@link BamSucheRepository#HOECHSTENS_TREFFER}
 * Zeilen und sagt, wenn es mehr gäbe; wer mehr sieht, verengt den Zeitraum oder nennt eine zweite
 * Nummer. Ein Cursor wäre hier zudem nicht dieselbe Zusage wie in der Liste: Die Sortierung ist der
 * Zeitpunkt, die Auswahl aber ein Indexzugriff über den Wert.
 *
 * @param nachrichten die gefundenen Nachrichten, absteigend nach Zeitpunkt. <b>Immer vorhanden,
 *     leer statt fehlend</b>
 * @param begriffe die Begriffe mit ihren gesuchten Fassungen — <b>keine stille Korrektur</b>
 * @param felder die Feldbegriffe der Property-Suche, wie sie verstanden wurden — je mit der Angabe,
 *     ob als Spalte gesucht wurde. <b>Immer vorhanden, leer statt fehlend</b>; eine reine BAM-Suche
 *     trägt hier eine leere Liste, und das ist die eine sichtbare Änderung an ihrer Antwort seit
 *     dem 08.09.2026
 * @param von das tatsächlich verwendete Zeitfenster, Untergrenze. <b>Es steht in der Antwort und
 *     nicht nur in der Anfrage</b>, weil es nicht die Laufzeit verändert, sondern die
 *     <i>Antwort</i>: Beim schlimmsten gemessenen Wert findet ein Tagesfenster 279 von 234.159
 *     Nachrichten (M35). Wer die Vorgabe nicht gesetzt hat, muss erfahren, welche galt
 * @param bis Obergrenze desselben Fensters
 * @param abgeschnitten ob es mehr Treffer gäbe als die gelieferten.
 *     <p><b>Gezählt wird nach dem Mandantenfilter</b>, weil der im Statement steht (Regel M3). Eine
 *     Meldung auf Basis der Rohtreffer sagte einem Nutzer etwas über die Datenmenge fremder
 *     Mandanten — genau die Sorte Leck, gegen die die 404-Regel beim Mandantenwechsel gebaut ist
 * @param modus der <b>tatsächlich verwendete</b> Modus, aus demselben Grund wie {@code von} und
 *     {@code bis}: Er verändert nicht den Preis, sondern die Antwort. M49‑3 misst, dass schon ein
 *     vollständig eingetippter Wert als Präfix <b>23 Nachrichten statt einer</b> findet — wer nicht
 *     weiß, welcher Vergleich gelaufen ist, kann die Trefferliste nicht deuten
 */
public record BamSucheResponse(
    List<BamTrefferResponse> nachrichten,
    List<BamBegriffResponse> begriffe,
    List<FeldBegriffResponse> felder,
    Instant von,
    Instant bis,
    boolean abgeschnitten,
    Suchmodus modus) {

  public BamSucheResponse {
    nachrichten = List.copyOf(nachrichten);
    begriffe = List.copyOf(begriffe);
    felder = List.copyOf(felder);
  }
}
