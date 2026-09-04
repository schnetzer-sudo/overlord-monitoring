package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Eine der beiden Kacheln fuer die <b>offenen</b> Zustaende — <i>Laeuft</i> (Block 4) und
 * <i>Wartend</i> (Block 4a). Beide haben dieselbe Gestalt und deshalb denselben Typ.
 *
 * <h2>Warum „offen" und nicht ein Name je Kachel</h2>
 *
 * <p>Die beiden Kacheln zeigen genau die beiden Einordnungen, fuer die {@code
 * MessageStatusClassifier.istEndstatus} {@code false} liefert: {@link
 * de.kraftwerkone.overlord.monitor.common.MessageStatusKind#LAEUFT} und {@link
 * de.kraftwerkone.overlord.monitor.common.MessageStatusKind#WARTEND}. Sie sind damit nicht zwei
 * beliebige Kacheln, sondern die vollstaendige Aufteilung eines Begriffs — und der heisst in diesem
 * Projekt <i>offen</i>.
 *
 * <h2>Die zwei benannten Ausnahmen von Leistungsregel L2</h2>
 *
 * <p>L2 sagt: „Dashboard-Kennzahlen kommen ausschliesslich aus {@code message_rollup}." <b>Das Wort
 * bleibt stehen, und daneben stehen diese beiden Ausnahmen</b> (E‑72 und E‑73 vom 03.09.2026,
 * {@code PROJEKTBESCHREIBUNG.md} §8). Sie treten an die Stelle der einen bisherigen Ausnahme
 * <i>Ueberfaellig</i>, die mit E‑71 entfallen ist.
 *
 * <p><b>Der Grund ist derselbe wie damals und trotzdem ein anderer.</b> Nicht mehr, dass die
 * Kennzahl an einer Frist haengt — sondern dass sie an einem <b>fluechtigen Status</b> haengt.
 * {@code message_rollup} traegt <b>keine Statushistorie</b>: Der naechtliche Volllauf rechnet jeden
 * Eimer aus dem <i>heutigen</i> Zustand jeder Nachricht neu. Eine Nachricht, die im Maerz {@code
 * SUSPENDED} war und im April fertig wurde, hinterlaesst im Maerz nichts. „Wie viele warten gerade"
 * ist dort nicht beantwortbar. Ausgeschrieben in {@code docs/rollup.md} §7a.
 *
 * <h2>Und sie sind die einzigen Felder, die „nicht ermittelbar" sein duerfen</h2>
 *
 * <p>Sie sind der einzige Teil der Antwort, der zur Laufzeit auf der <b>Produktion</b> live ueber
 * {@code Message} liest, wo {@code max_statement_time} nach zehn Sekunden abraeumt. Der Rest kommt
 * aus unserer eigenen Tabelle. <b>Stirbt die Live-Abfrage, darf nicht die ganze Seite sterben</b> —
 * dann sind beide Zahlen {@code null} und {@link #ermittelbar} ist {@code false}.
 *
 * <p><b>Kein allgemeiner Teilerfolg-Mechanismus.</b> Genau diese Kacheln, und kein anderer Block
 * bekommt einen — auch die Erscheinungsbedingung aus {@code
 * DashboardRepository.hatWartendeAblaeufe} nicht: Sie liest <b>Stammdaten</b> ({@code SOSAction},
 * 2,0 MiB) und ist damit dieselbe Art Zugriff wie die Mandantenkette, die in jedem Statement steht.
 * Ein Dashboard, das jeden Block einzeln scheitern lassen kann, zeigt irgendwann eine Seite voller
 * Luecken und nennt das eine Antwort.
 *
 * <p><b>Die beiden Zahlen einer Kachel fallen zusammen</b>, aber die beiden <i>Kacheln</i> nicht:
 * {@code laeuft} und {@code wartend} sind zwei getrennte Statements und zwei getrennte Auskuenfte.
 * Faellt eines, steht das andere weiterhin da.
 *
 * @param anzahl wie viele Nachrichten des Mandanten gerade in diesem Zustand stehen. {@code null},
 *     wenn nicht ermittelbar. <b>Ohne Zeitfenster</b> (Regel L9): Gefragt ist, was <i>jetzt</i>
 *     offen ist, und ein Fenster schnitte gerade die aeltesten Zeilen weg — also die, um die es
 *     geht
 * @param aeltesteSekunden wie lange die aelteste dieser Nachrichten schon steht, in ganzen
 *     Sekunden, gerechnet gegen die <b>Anwendungsuhr</b> (Regel Z1). <b>{@code null} bei {@code
 *     anzahl = 0}</b> — ohne Zeile gibt es kein Alter. Ebenfalls {@code null}, wenn der Abstand
 *     negativ waere: „wartet seit minus drei Sekunden" ist schlechter als gar keine Angabe
 *     (dieselbe Regel wie in {@code docs/nachrichtendetail.md} §3a). {@code null} auch, wenn nicht
 *     ermittelbar
 * @param ermittelbar {@code false} heisst: Die Live-Abfrage ist an der Zeitgrenze abgebrochen. Das
 *     ist <b>nicht</b> dasselbe wie „null Nachrichten in diesem Zustand", und genau deshalb steht
 *     es als eigenes Feld da und nicht als Null. In einem Ueberwachungswerkzeug ist eine erfundene
 *     Null die schlimmste falsche Antwort
 */
public record OffeneKachelResponse(Long anzahl, Long aeltesteSekunden, boolean ermittelbar) {

  /** Die Zahlen liegen vor. {@code aeltesteSekunden} darf dabei {@code null} sein. */
  static OffeneKachelResponse von(long anzahl, Long aeltesteSekunden) {
    return new OffeneKachelResponse(anzahl, aeltesteSekunden, true);
  }

  /** Die Live-Abfrage ist abgebrochen — die Seite steht trotzdem. */
  static OffeneKachelResponse nichtErmittelbar() {
    return new OffeneKachelResponse(null, null, false);
  }
}
