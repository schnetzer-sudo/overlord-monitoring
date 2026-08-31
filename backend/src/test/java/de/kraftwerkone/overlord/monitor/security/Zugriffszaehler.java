package de.kraftwerkone.overlord.monitor.security;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;

/**
 * Zaehlt die Statements, die auf dem <b>Lese-Kontext</b> abgesetzt werden, und haelt ihren
 * gerenderten Text fest.
 *
 * <p><b>Wozu es ihn gibt.</b> Ein Isolationstest kann die Ununterscheidbarkeit zweier Antworten
 * ueber ihren Rumpf pruefen — das ist die <i>Wirkung</i>. Die <i>Ursache</i> erreicht er damit
 * nicht: Ein Code, der erst die Existenz nachschlaegt und dann denselben festen Text ausgibt,
 * bestuende jeden Rumpfvergleich und waere trotzdem unterscheidbar, weil „gibt es nicht" einen
 * Zugriff kostet und „gehoert einem anderen" zwei. Ueber genug Anfragen ist das ein messbarer
 * Kanal.
 *
 * <p><b>Gezaehlt und nicht gestoppt</b> — Regel <b>T1</b>. Bis zum 31.08.2026 stand an dieser
 * Stelle ein Vergleich zweier Wanduhrzeiten gegen eine Faktor-10-Schranke. Er hat einen
 * Datenbankzugriff von einer halben Millisekunde ueber HTTP geschuetzt und ist deshalb gelegentlich
 * grundlos gefallen (einmal mit 279 ms gegen 20 ms). <b>Der Test war nicht ungenau, er hat die
 * falsche Groesse gemessen.</b> Die Zahl der abgesetzten Statements ist dieselbe Aussage ohne das
 * Rauschen: Sie ist genau das, was eine nachgelagerte Existenzpruefung veraendern wuerde, sie ist
 * deterministisch, und sie steigt <b>sofort</b> und nicht in einem von vier Laeufen.
 *
 * <p><b>Gezaehlt wird in {@link ExecuteListener#executeStart}</b>, weil {@link
 * ExecuteContext#sql()} dort steht: Das Statement ist gerendert, die Bindewerte sind noch
 * Platzhalter. Damit ist der Text zweier Anfragen, die dasselbe Statement mit anderen Werten
 * absetzen, <b>identisch</b> — und ein zusaetzliches Statement faellt nicht nur als Zahl auf,
 * sondern mit seinem Wortlaut.
 *
 * <p><b>Was die Zaehlung nicht abdeckt</b> — und das ist ein offener Punkt und keine
 * Nebenbemerkung: Zwei gleich viele Zugriffe koennten verschieden lange dauern, etwa weil das eine
 * Statement Zeilen liest und das andere keine. Ob das eine reale Luecke ist, ist <b>nicht</b>
 * beantwortet; die Frage steht als offener Punkt T-1 in {@code docs/testfestigkeit.md} §6.
 *
 * <p>Angebracht wird er ueber {@link Zugriffszaehlung}. Er lebt ausschliesslich in {@code
 * src/test}; am Anwendungscode aendert sich nichts.
 *
 * @see Zugriffszaehlung
 */
public final class Zugriffszaehler implements ExecuteListener {

  private final List<String> abgesetzt = new CopyOnWriteArrayList<>();

  @Override
  public void executeStart(ExecuteContext ctx) {
    abgesetzt.add(String.valueOf(ctx.sql()));
  }

  public void zuruecksetzen() {
    abgesetzt.clear();
  }

  /** Die Statements seit dem letzten {@link #zuruecksetzen()}, in der Reihenfolge des Absetzens. */
  public List<String> abgesetzt() {
    return List.copyOf(abgesetzt);
  }
}
