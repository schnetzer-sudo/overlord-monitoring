package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOS;

import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectField;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die fuenf Statements der Verkettung — <b>vier Richtungen und zwei Zaehlungen</b>, jedes einzeln
 * gemessen (M30‑1).
 *
 * <p><b>Jedes einzelne traegt den Mandantenfilter</b> (Regeln M3 und M5), und zwar als {@code
 * EXISTS} ueber {@code Process → ProjectMandant} in genau der Form, die {@code
 * NachrichtenRepository} und {@code NachrichtendetailRepository} verwenden — nicht als
 * nachgelagerte Pruefung. <b>Auch die Zaehlung</b>: Naehme sie den Filter nicht, nennte die Antwort
 * 3.048 Teile und lieferte 3.000, und der Unterschied saehe wie ein Fehler des Werkzeugs aus. M27
 * hat gemessen, dass die Kette die Mandantengrenze nie ueberschreitet, und M30‑5 hat es fuer die
 * beiden Bezugszeilen nachgeprueft — der Filter ist trotzdem gesetzt: Er ist die
 * <b>Zusicherung</b>, nicht die Beobachtung.
 *
 * <p><b>Kein Zeitfenster</b>, und das ist keine Nachlaessigkeit gegenueber Regel L1: Die gilt fuer
 * <i>Listen</i> ueber {@code Message}. Hier ist die Menge durch einen Primaerschluessel benannt —
 * dieselbe Begruendung wie beim Detail-Endpunkt. Ein Fenster koennte hier sogar schaden: Die Kinder
 * einer drei Monate alten Wurzel laegen ausserhalb jedes vernuenftigen Fensters.
 *
 * <p><b>Die Kosten, gemessen gegen die Testkopie</b> ({@code docs/messungen-schritt6.md} M30‑1;
 * beste von fuenf nach einem Aufwaermlauf):
 *
 * <table>
 *   <caption>Zugriffspfad und Laufzeit je Statement</caption>
 *   <tr><th>Statement</th><th>Zugriff</th><th>Normalfall</th><th>Breitfall</th></tr>
 *   <tr><td>{@link #findeGlied}</td><td>{@code const} ueber {@code PRIMARY}</td>
 *       <td>0,48–0,51 ms</td><td>—</td></tr>
 *   <tr><td>{@link #findeKinder}</td><td>{@code ref} ueber {@code SourceMessageIDIDX}</td>
 *       <td>0,65 ms</td><td>26,0 ms bei 3.048</td></tr>
 *   <tr><td>{@link #findeMergeEingaenge}</td><td>{@code ref} ueber {@code TargetMessageIDIDX}</td>
 *       <td>0,60 ms</td><td>5,7 ms bei 749</td></tr>
 *   <tr><td>{@link #zaehleKinder}</td><td>{@code ref} ueber {@code SourceMessageIDIDX}</td>
 *       <td>0,54 ms</td><td>19,8 ms bei 3.350</td></tr>
 *   <tr><td>{@link #zaehleMergeEingaenge}</td><td>{@code ref} ueber {@code TargetMessageIDIDX}</td>
 *       <td>0,51 ms</td><td>4,9 ms bei 897</td></tr>
 * </table>
 */
@Repository
public class KettenRepository {

  /**
   * Der Alias fuer die Mandantenkette. Er muss ein anderer sein als der der aeusseren Abfrage — ein
   * gleichnamiger wuerde die Unterabfrage an sich selbst binden.
   *
   * <p>Der Typ {@code Process} ist die generierte Tabelle des Quellschemas; der Import verdeckt in
   * dieser Datei {@code java.lang.Process}.
   */
  private static final Process MANDANTEN_PROCESS = PROCESS.as("mandanten_process");

  /**
   * Die Spalten eines Kettenglieds — an einer Stelle, weil alle drei lesenden Statements dieselben
   * brauchen.
   *
   * <p>Die vier Verkettungsspalten kommen mit, obwohl sie in der Antwortzeile nicht erscheinen: Aus
   * ihnen entstehen die Rollen, und aus zweien von ihnen der naechste Schritt des Aufstiegs.
   */
  private static final List<SelectField<?>> SPALTEN =
      List.of(
          MESSAGE.MESSAGEID,
          MESSAGE.MESSAGESTATUS,
          MESSAGE.MESSAGELASTUPDATE,
          SOS.SOSNAME,
          MESSAGE.SOURCE,
          MESSAGE.TARGET,
          MESSAGE.SOURCEMESSAGEID,
          MESSAGE.TARGETMESSAGEID);

  private final DSLContext glassfishDsl;

  KettenRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Ein einzelnes Glied — oder {@code null}, wenn es die Nachricht nicht gibt <b>oder</b> sie einem
   * fremden Mandanten gehoert.
   *
   * <p>Beides ist von hier aus ununterscheidbar, und genau das ist der Zweck: Der Aufrufer bekommt
   * in beiden Faellen dieselbe {@code 404}-Antwort, weil beide Faelle <i>dasselbe Statement mit
   * null Zeilen</i> sind (Regel M3, {@code mandantentrennung.md} §5). Eine Existenzpruefung davor
   * braeche die Zusage, auch wenn kein Test rot wuerde — „gibt es nicht" kostete einen Zugriff und
   * „gehoert einem anderen" zwei.
   *
   * <p><b>Dieses eine Statement bedient die Richtungen 1 und 3</b> der Messung: Der Schluessel
   * kommt einmal aus {@code SourceMessageID} (mein Elternteil), einmal aus {@code TargetMessageID}
   * (mein Merge-Ergebnis). Zwei Methoden waeren zwei Statements mit identischem Text.
   *
   * <p>Gemessen als {@code const} auf jeder beteiligten Tabelle — nicht {@code eq_ref}: Die Kennung
   * steht als Literal im Statement, MariaDB loest den Zugriff schon beim Planen auf. 0,48 bis 0,51
   * Millisekunden (M30‑1).
   */
  public Kettengliedzeile findeGlied(MandantContext mandant, String messageId) {
    return glassfishDsl
        .select(SPALTEN)
        .from(MESSAGE)
        .leftJoin(SOS)
        .on(SOS.SOSID.eq(MESSAGE.SOSID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(mandantenkette(mandant))
        .fetchOne(KettenRepository::zeile);
  }

  /**
   * Die <b>Kinder</b> einer Wurzel — die Gegenrichtung der Aufteilung, ueber {@code
   * SourceMessageIDIDX}.
   *
   * <p>Gelesen werden {@code limit + 1} Zeilen; die Zusatzzeile beantwortet „gibt es weitere" ohne
   * einen zweiten Zugriff. Sortiert wird nach {@code (MessageLastUpdate, MessageID)} — demselben
   * Schluessel wie in der Liste, damit derselbe Cursor-Typ passt.
   *
   * <p><b>Das {@code LIMIT} begrenzt die Ausgabe, nicht die Arbeit</b> (M30‑1). {@code
   * SourceMessageIDIDX} steht auf {@code SourceMessageID} und liefert die Sortierreihenfolge nicht
   * mit; das {@code Using filesort} im Plan bedeutet, dass jede Trefferzeile gesehen werden muss,
   * <b>bevor</b> das {@code LIMIT} greifen kann. An der breitesten Wurzel des Bestands sind das
   * 3.350 Zeilen und 26,4 Millisekunden — die Groessenordnung eines Listenaufrufs und weit unter
   * der Zeitgrenze des Lese-Pools. Die Grenze bleibt trotzdem: 3.350 Zeilen in einem JSON-Rumpf
   * sind unabhaengig von der Datenbank ein Problem.
   */
  public List<Kettengliedzeile> findeKinder(
      MandantContext mandant, String wurzelId, Seitenposition ab, int limit) {
    return findeAbwaerts(mandant, MESSAGE.SOURCEMESSAGEID, wurzelId, ab, limit);
  }

  /**
   * Die <b>Eingaenge</b> eines Merge-Ergebnisses — die Gegenrichtung der Zusammenfuehrung, ueber
   * {@code TargetMessageIDIDX}.
   *
   * <p>Dieselbe Gestalt wie {@link #findeKinder}, dieselben Kosten je Zeile (7,6 gegen 8,5
   * Mikrosekunden, M30‑1) — <b>aber eine andere Verteilung</b>: {@code TargetMessageIDIDX} traegt
   * eine Kardinalitaet von 63.580 gegen 1.780.243 (M23‑1), und der Zusammenfuehrungsgrad liegt bei
   * 23 : 1 (M25‑1). Praktisch heisst das: <b>11,38 Prozent der Merge-Ergebnisse haben mehr als 50
   * Eingaenge, gegen 1,07 Prozent der Wurzeln</b> (M30‑2). Die Breitengrenze greift hier also
   * regelmaessig und nicht ausnahmsweise.
   */
  public List<Kettengliedzeile> findeMergeEingaenge(
      MandantContext mandant, String ergebnisId, Seitenposition ab, int limit) {
    return findeAbwaerts(mandant, MESSAGE.TARGETMESSAGEID, ergebnisId, ab, limit);
  }

  /**
   * Wie viele Kinder eine Wurzel hat — <b>die genaue Zahl</b>, nicht „mehr als 50".
   *
   * <p><b>Regel L2 ist hier nicht beruehrt.</b> Sie verbietet Live-Aggregation ueber {@code
   * Message} fuer Dashboard-Kennzahlen; die kommen aus {@code message_rollup}. Dieser {@code COUNT}
   * ist ein {@code ref}-Zugriff auf hoechstens 3.350 Zeilen, bezogen auf <b>eine</b> benannte
   * Wurzel — dieselbe Bauform wie die Anzahl der Eigenschaften im Nachrichtendetail. Das steht
   * hier, damit die Zahl nicht spaeter mit Verweis auf L2 gestrichen wird.
   *
   * <p><b>Die genaue Zahl ist eine gemessene Entscheidung</b> (M30‑1): An der breitesten Wurzel des
   * <i>gesamten</i> Bestands kostet die Zaehlung 19,760 Millisekunden — nicht die Haelfte der
   * Grenze von 50, ab der auf „mehr als 50" umgestellt worden waere.
   *
   * <p><b>Kein {@code Using index}, und das ist der Preis von Regel M5.</b> Der Mandantenfilter
   * braucht {@code Message.ProcessID}, und die steht in keinem der beiden Verkettungsindizes — der
   * {@code COUNT} muss also in die Tabelle. Ohne Filter waere er index-only; mit Filter beschreibt
   * er denselben Bestand wie die Zeilen darunter, und das ist mehr wert.
   */
  public int zaehleKinder(MandantContext mandant, String wurzelId) {
    return zaehle(mandant, MESSAGE.SOURCEMESSAGEID, wurzelId);
  }

  /**
   * Wie viele Eingaenge ein Merge-Ergebnis hat. Gestalt und Begruendung wie {@link #zaehleKinder}.
   */
  public int zaehleMergeEingaenge(MandantContext mandant, String ergebnisId) {
    return zaehle(mandant, MESSAGE.TARGETMESSAGEID, ergebnisId);
  }

  /**
   * Die gemeinsame Gestalt der beiden Abwaertsrichtungen. Sie unterscheiden sich in genau einer
   * Spalte — als zwei ausgeschriebene Statements waeren sie zwei Stellen, an denen der
   * Mandantenfilter vergessen werden kann.
   */
  private List<Kettengliedzeile> findeAbwaerts(
      MandantContext mandant,
      TableField<?, String> verweis,
      String wurzelId,
      Seitenposition ab,
      int limit) {
    Condition bedingungen = verweis.eq(wurzelId).and(mandantenkette(mandant));
    if (ab != null) {
      bedingungen = bedingungen.and(hinterDerPosition(ab));
    }
    return glassfishDsl
        .select(SPALTEN)
        .from(MESSAGE)
        .leftJoin(SOS)
        .on(SOS.SOSID.eq(MESSAGE.SOSID))
        .where(bedingungen)
        .orderBy(MESSAGE.MESSAGELASTUPDATE.asc(), MESSAGE.MESSAGEID.asc())
        .limit(limit + 1)
        .fetch(KettenRepository::zeile);
  }

  private int zaehle(MandantContext mandant, TableField<?, String> verweis, String wurzelId) {
    Integer anzahl =
        glassfishDsl
            .selectCount()
            .from(MESSAGE)
            .where(verweis.eq(wurzelId))
            .and(mandantenkette(mandant))
            .fetchOne(0, Integer.class);
    return anzahl == null ? 0 : anzahl;
  }

  /**
   * Die Cursor-Bedingung in der <b>ODER-Form</b>, Wort fuer Wort wie in {@code
   * NachrichtenRepository} — dort ist sie gemessen (Messung L8): Sie ergibt einen Bereich ueber
   * beide Spalten, der Tupelvergleich nur einen ueber den Zeitstempel.
   *
   * <p>Hier traegt sie den Zugriffspfad nicht — der ist {@code SourceMessageIDIDX} beziehungsweise
   * {@code TargetMessageIDIDX} —, sondern schneidet nur die schon gelesene Menge ab. Die gleiche
   * Form zu nehmen ist trotzdem richtig: Zwei Fassungen derselben Bedingung sind zwei Stellen, an
   * denen sie auseinanderlaufen kann.
   */
  private static Condition hinterDerPosition(Seitenposition position) {
    return MESSAGE
        .MESSAGELASTUPDATE
        .gt(position.zeitpunkt())
        .or(
            MESSAGE
                .MESSAGELASTUPDATE
                .eq(position.zeitpunkt())
                .and(MESSAGE.MESSAGEID.gt(position.id())));
  }

  /**
   * Die Mandantenkette {@code Message → Process → ProjectMandant} als {@code EXISTS} — Wort fuer
   * Wort dieselbe wie in {@code NachrichtenRepository} und {@code NachrichtendetailRepository}.
   *
   * <p>Als {@code EXISTS} und nicht als Join: {@code ProjectMandant} ist im Schema n:m, ein Join
   * koennte Zeilen vervielfachen, sobald ein Projekt mehreren Mandanten gehoert. Heute tut er das
   * nicht (M3: alle 134 Projekte gehoeren genau einem Mandanten) — aber eine Antwort, deren
   * Zeilenzahl an einer Stammdatenpflege haengt, ist die falsche Grundlage fuer eine
   * Sicherheitsgrenze. Bei der Kette waere die Folge doppelt sichtbar: Die Zaehlung und die Zeilen
   * liefen gemeinsam aus dem Ruder, und das saehe nach einem Fehler in der Kette aus statt nach
   * einem im Filter.
   */
  private static Condition mandantenkette(MandantContext mandant) {
    return DSL.exists(
        DSL.selectOne()
            .from(MANDANTEN_PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(MANDANTEN_PROCESS.PROJECTID))
            .where(MANDANTEN_PROCESS.PROCESSID.eq(MESSAGE.PROCESSID))
            .and(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())));
  }

  private static Kettengliedzeile zeile(org.jooq.Record satz) {
    return new Kettengliedzeile(
        satz.get(MESSAGE.MESSAGEID),
        satz.get(MESSAGE.MESSAGESTATUS),
        satz.get(MESSAGE.MESSAGELASTUPDATE),
        satz.get(SOS.SOSNAME),
        satz.get(MESSAGE.SOURCE),
        satz.get(MESSAGE.TARGET),
        satz.get(MESSAGE.SOURCEMESSAGEID),
        satz.get(MESSAGE.TARGETMESSAGEID));
  }
}
