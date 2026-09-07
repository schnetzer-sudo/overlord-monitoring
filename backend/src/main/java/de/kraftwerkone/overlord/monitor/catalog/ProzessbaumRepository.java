package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_MONAT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.MESSAGE_ROLLUP_TAG;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;

import de.kraftwerkone.overlord.monitor.common.Baumfenster.Segment;
import de.kraftwerkone.overlord.monitor.common.Rollupebene;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Der Datenzugriff der Prozessansicht — <b>zwei Statements, und mehr werden es nicht</b>.
 *
 * <p>Gelesen wird ueber {@code glassfishDsl}, den Lese-Pool: Beide Abfragen fassen {@code
 * GlassfishDB} und {@code overlord_monitor} in <b>einer</b> Verbindung an, und schemauebergreifende
 * Abfragen laufen zwingend ueber eine einzige Verbindung ({@code config/JooqConfig}). Der {@code
 * ReadOnlyExecuteListener} haengt daran und weist jeden Schreibversuch ab.
 *
 * <h2>Warum es zwei sind und nicht eines</h2>
 *
 * <p>Die beiden Abfragen beantworten <b>zwei verschiedene Fragen ueber zwei verschiedene
 * Mengen</b>:
 *
 * <ul>
 *   <li>{@link #geruest} zaehlt <b>alle</b> Prozesse des Mandanten — auch die, die im Fenster
 *       nichts getragen haben, und auch die, die noch nie etwas getragen haben. Es ist
 *       <b>fensterunabhaengig</b>.
 *   <li>{@link #kennzahlen} zaehlt nur, was im Fenster liegt, und ist damit die einzige der beiden,
 *       die ueberhaupt ein Fenster kennt.
 * </ul>
 *
 * <p>In <i>einem</i> Statement waere die zweite Menge ein {@code LEFT JOIN} auf eine Aggregation
 * und damit ein zweiter Zugriffspfad im selben Plan; getrennt hat jede ihren eigenen, und jeder ist
 * gemessen (Regel L7, {@code docs/process-view.md} §7).
 *
 * <p><b>Das gilt auch fuer das freie Zeitfenster</b> <i>(seit 07.09.2026, Schritt 10c-4b)</i>: Die
 * Kennzahlen eines freien Fensters bleiben <b>ein</b> Statement, auch wenn sie bis zu drei
 * Rollup-Ebenen lesen — Bauform Z-U aus {@code docs/process-view.md} §33, gemessen gegen die
 * Bauform mit drei Statements (M149 gegen M150: Faktor 1,8 bis 2,2 schneller). E-42 faellt nicht.
 *
 * <h2>Die Mandantenkette steht zweimal verschieden da — und das ist kein Versehen</h2>
 *
 * <p>{@link #geruest} nimmt einen <b>Join</b>, {@link #kennzahlen} ein <b>{@code EXISTS}</b>. Der
 * Unterschied folgt aus der Richtung des Zugriffs, genau wie in {@code docs/prozessauswahl.md} §4:
 *
 * <ul>
 *   <li><b>Im Geruest</b> ist {@code ProjectMandant} der selektivste Teil der Bedingung und soll
 *       den Zugriff treiben. Vervielfachen kann er nichts: Der Primaerschluessel ist {@code
 *       (ProjectID, MandantID)}, und mit {@code MandantID = ?} bleibt je Projekt hoechstens eine
 *       Zeile uebrig. Das gilt <b>aus dem Schema heraus</b> und nicht erst aus den Daten.
 *       Aggregiert wird ausserdem nichts — eine Zeile zu viel waere sichtbar und keine stille
 *       Verfaelschung.
 *   <li><b>In den Kennzahlen</b> wird <b>summiert</b>. {@code ProjectMandant} ist n:m; ein Join
 *       vervielfachte jede Rollupzeile, sobald ein Projekt an mehreren Mandanten haengt — und damit
 *       <b>die Summe</b>. Der Fehler waere still: Die Zahlen saehen plausibel aus und waeren zu
 *       hoch. Dieselbe Begruendung wie in {@code dashboard/DashboardRepository}.
 * </ul>
 *
 * <p><b>Gemessen ist der Unterschied trotzdem</b> (M113): Ueber alle vier gemessenen Mandanten und
 * alle drei Fensterbreiten liegen Join und {@code EXISTS} innerhalb des Rauschens — 6,3 bis 77,2 ms
 * die eine, 6,4 bis 77,2 ms die andere. <b>Die Wahl faellt damit an der Richtigkeit und nicht am
 * Tempo</b>, und das ist der Grund, warum sie hier steht und nicht im Kasten „schneller".
 */
@Repository
public class ProzessbaumRepository {

  /**
   * Der Alias fuer die Mandantenkette der Kennzahlenabfrage. Er ist noetig, sobald dieselbe Abfrage
   * {@code Process} auch ausserhalb des {@code EXISTS} anfasst — und er steht auch dort, wo sie es
   * heute nicht tut, damit der naechste Join nicht stillschweigend auf die falsche Tabelle zeigt.
   */
  private static final Process KETTE_PROCESS = PROCESS.as("baum_process");

  /** Der Alias der Ableitung, in der die Bereichslesungen des freien Fensters zusammenlaufen. */
  private static final String ABLEITUNG = "t";

  private final DSLContext glassfishDsl;

  ProzessbaumRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * <b>Statement 1</b>: alle Prozesse des Mandanten mit ihren kuratierten Feldern und ihrer letzten
   * Bewegung.
   *
   * <h2>„Letzte Bewegung" als {@code ORDER BY … LIMIT 1} und nicht als {@code MAX()}</h2>
   *
   * <p>Beide fragen denselben Wert. <b>Sie kosten nicht dasselbe</b>, und der Unterschied ist
   * gemessen (M112 gegen M114):
   *
   * <table border="1">
   *   <caption>beste von fuenf, gegen die Testkopie</caption>
   *   <tr><th>Fassung</th><th>NEXANS (733)</th><th>VOTG (390)</th><th>IBIS (192)</th>
   *       <th>SUTTONS (17)</th></tr>
   *   <tr><td>{@code MAX()} als korrelierte Unterabfrage</td><td>16,089 ms</td><td>2,364 ms</td>
   *       <td>5,002 ms</td><td><b>32,577 ms</b></td></tr>
   *   <tr><td><b>{@code ORDER BY … LIMIT 1}</b> — gebaut</td><td><b>3,417 ms</b></td>
   *       <td><b>2,513 ms</b></td><td><b>2,524 ms</b></td><td><b>1,460 ms</b></td></tr>
   * </table>
   *
   * <p><b>Der Plan sieht in beiden Faellen gleich aus</b> — {@code DEPENDENT SUBQUERY … ref
   * message_rollup_prozess_idx … Using index} —, und genau das ist die Falle: Mit {@code MAX()}
   * liest MariaDB je Prozess den <b>ganzen</b> Indexbereich und nimmt davon das Maximum; die
   * MIN/MAX-Optimierung greift in einer abhaengigen Unterabfrage nicht. Mit {@code ORDER BY … DESC
   * LIMIT 1} steht der gesuchte Wert am Anfang des Bereichs, und der Zugriff endet nach einer
   * Zeile.
   *
   * <p><b>Der Ausreisser bei {@code SUTTONS} ist der Beleg dafuer, dass es nicht an der Prozesszahl
   * haengt:</b> 17 Prozesse und die <i>hoechste</i> Laufzeit aller vier Mandanten. Der Aufwand der
   * {@code MAX()}-Fassung haengt an der Zahl der <b>Eimer je Prozess</b>, und die ist bei einem
   * kleinen Mandanten mit viel Verkehr hoch.
   *
   * <h2>Ohne den Index aus {@code V11} waere das hier nicht bezahlbar</h2>
   *
   * <p>{@code message_rollup_prozess_idx (process_id, stunde)} existiert seit dem 30.08.2026 und
   * ist fuer die Fensterverengung der Nachrichtenliste gebaut worden ({@code docs/rollup.md} §9b).
   * Dieselbe Spaltenreihenfolge traegt diese Abfrage. Gemessen mit {@code IGNORE INDEX} (M115,
   * derselbe Bau, nur ohne den Index):
   *
   * <table border="1">
   *   <caption>beste von fuenf, mit und ohne Index</caption>
   *   <tr><th></th><th>NEXANS</th><th>VOTG</th><th>IBIS</th><th>SUTTONS</th></tr>
   *   <tr><td>mit Index</td><td>3,520 ms</td><td>2,598 ms</td><td>2,461 ms</td>
   *       <td>1,575 ms</td></tr>
   *   <tr><td>ohne Index</td><td>2.858 ms</td><td><b>6.083 ms</b></td><td>2.811 ms</td>
   *       <td>11,701 ms</td></tr>
   * </table>
   *
   * <p><b>Es entsteht deshalb keine neue Migration</b> — der Index ist da, und er ist an {@code
   * information_schema.STATISTICS} nachgesehen und nicht aus der Dokumentation uebernommen (M109).
   *
   * <h2>{@code process_catalog} als {@code LEFT JOIN}</h2>
   *
   * <p>{@code SUTTONS} und {@code WOC} haben <b>keine einzige</b> Katalogzeile (M110). Ein innerer
   * Join verloere ihre 17 bzw. 4 Prozesse stillschweigend — und damit ausgerechnet die, die
   * vollstaendig unter „nicht zugeordnet" erscheinen muessten.
   *
   * @param mandant Regel M2 — erster Pflichtparameter, und er steht im Statement und nicht dahinter
   *     (Regel M3)
   */
  public List<Prozessgeruestzeile> geruest(MandantContext mandant) {
    Field<LocalDateTime> letzteBewegung =
        DSL.field(
            DSL.select(MESSAGE_ROLLUP.STUNDE)
                .from(MESSAGE_ROLLUP)
                .where(MESSAGE_ROLLUP.PROCESS_ID.eq(PROCESS.PROCESSID))
                .orderBy(MESSAGE_ROLLUP.STUNDE.desc())
                .limit(1));

    return glassfishDsl
        .select(
            PROCESS.PROCESSID,
            PROCESS.PROCESSNAME,
            PROCESS_CATALOG.PARTNER,
            PROCESS_CATALOG.RICHTUNG,
            PROCESS_CATALOG.PFLEGESTATUS,
            letzteBewegung)
        .from(PROCESS)
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROCESS_CATALOG)
        .on(PROCESS_CATALOG.PROCESS_ID.eq(PROCESS.PROCESSID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .orderBy(PROCESS.PROCESSNAME.asc())
        .fetch(
            satz ->
                new Prozessgeruestzeile(
                    satz.value1(),
                    satz.value2(),
                    satz.value3(),
                    satz.value4(),
                    satz.value5(),
                    satz.value6()));
  }

  /**
   * <b>Statement 2</b>: je Prozess und Rohstatus die Summe im gewaehlten Fenster.
   *
   * <p><b>Gruppiert wird nach dem Rohstatus</b> und nicht schon nach der Einordnung (E-g). Damit
   * entstehen <i>Nachrichten</i> und <i>Fehler</i> aus <b>einem</b> Lesevorgang, und die Einordnung
   * bleibt eine Regel, die sich aendern kann, ohne dass eine Tabelle neu gerechnet werden muss.
   *
   * <p><b>Kein {@code ORDER BY}.</b> Die Reihenfolge des Baums entsteht in {@link
   * ProzessbaumService} — nach Partner, Richtung und Name, nicht nach Prozesskennung. Ein {@code
   * ORDER BY} hier kostete eine Sortierung, die niemand liest.
   *
   * <h2>Ein Segment oder mehrere — zwei Bauformen, ein Statement</h2>
   *
   * <p><b>Ein Segment</b> — das ist jedes der drei Paare — rendert <b>Zeichen fuer Zeichen den Text
   * von vor dem 07.09.2026</b>: Bereich und Mandantenkette in einem {@code WHERE}, gemessen in
   * M116, festgehalten in {@code ProzessbaumStatementsTest}. Dass dieser Text unveraendert bleibt,
   * ist die tragende Zusage von Schritt 10c-4b.
   *
   * <p><b>Mehrere Segmente</b> — nur das freie Fenster — fuehren ihre Bereichslesungen in einer
   * Ableitung mit {@code UNION ALL} zusammen: je Ebene <b>ein</b> Zweig, dessen Bereiche als {@code
   * OR} nebeneinanderstehen (Kopf und Fuss eines Fensters liegen auf derselben Ebene), darueber
   * <b>eine</b> Gruppierung und <b>eine</b> Mandantenkette. <b>Leere Ebenen erzeugen keinen
   * Zweig</b>: Traegt ein Fenster keine Monatssegmente, steht keine Monatsebene im Text.
   *
   * <p><b>Warum die Kette aussen steht und nicht in jedem Zweig</b> (M149 gegen M114): Die
   * Ableitung wird vom Optimierer <b>einmal materialisiert</b> und ueber einen automatischen
   * Schluessel je Prozess geprobt; die Mandantenkette laeuft damit je Prozess (733 bei {@code
   * NEXANS}) statt je Zeile (14.148 im Boesfall). Stuende sie innen <i>und</i> aussen wie in M114,
   * wuerde sie zweimal ausgewertet. <b>Das ist eine Beobachtung ueber den Optimierer und keine
   * Zusicherung</b> — {@code ProzessbaumPlanDbIT} schreibt je Zweig den Bereichszugriff fest und
   * nicht die Materialisierung.
   *
   * <p><b>Die vier Eigenschaften aus {@code docs/process-view.md} §6 gelten je Zweig</b>: keine
   * Funktion um die Schluesselspalte, {@code EXISTS} statt Join, und — in der Fassung vom
   * 07.09.2026 — <i>jedes Segment liest seine Ebene und keine andere, und es steht keine Ebene im
   * Text, die kein Segment traegt</i>.
   *
   * @param segmente ein bis fuenf Segmente aus {@code Baumfenster.segmente(jetzt)}, {@code von}
   *     einschliessend, {@code bis} ausschliessend, beide auf einer Eimergrenze ihrer Ebene
   */
  public List<Prozesskennzahlzeile> kennzahlen(MandantContext mandant, List<Segment> segmente) {
    if (segmente.isEmpty()) {
      throw new IllegalArgumentException("Kennzahlen ohne Segment gibt es nicht");
    }
    if (segmente.size() == 1) {
      return ungeteilt(mandant, segmente.getFirst());
    }
    return vereinigt(mandant, segmente);
  }

  /** Die Bauform der drei Paare — und der eine Text, der sich nicht aendern darf. */
  private List<Prozesskennzahlzeile> ungeteilt(MandantContext mandant, Segment segment) {
    Ebene ebene = ebene(segment.ebene());
    return glassfishDsl
        .select(ebene.prozess(), ebene.status(), DSL.sum(ebene.anzahl()))
        .from(ebene.tabelle())
        .where(bereich(segment))
        .and(mandantenkette(mandant, ebene.prozess()))
        .groupBy(ebene.prozess(), ebene.status())
        .fetch(
            satz ->
                new Prozesskennzahlzeile(satz.value1(), satz.value2(), satz.value3().longValue()));
  }

  /** Die Bauform Z-U des freien Fensters: eine Ableitung mit {@code UNION ALL}, darueber alles. */
  private List<Prozesskennzahlzeile> vereinigt(MandantContext mandant, List<Segment> segmente) {
    Select<Record3<String, String, Integer>> ableitung = null;
    // Feste Reihenfolge der Zweige — Stunde, Tag, Monat —, damit derselbe Fensterschnitt immer
    // denselben Text ergibt, gleich in welcher Reihenfolge die Segmente ankommen.
    for (Rollupebene rollupebene : Rollupebene.values()) {
      List<Condition> bereiche = new ArrayList<>();
      Ebene ebene = ebene(rollupebene);
      for (Segment segment : segmente) {
        if (segment.ebene() == rollupebene) {
          bereiche.add(bereich(segment));
        }
      }
      if (bereiche.isEmpty()) {
        continue;
      }
      Select<Record3<String, String, Integer>> zweig =
          glassfishDsl
              .select(ebene.prozess(), ebene.status(), ebene.anzahl())
              .from(ebene.tabelle())
              .where(DSL.or(bereiche));
      ableitung = ableitung == null ? zweig : ableitung.unionAll(zweig);
    }

    Table<Record3<String, String, Integer>> t = ableitung.asTable(ABLEITUNG);
    Field<String> prozess = t.field(MESSAGE_ROLLUP.PROCESS_ID);
    Field<String> status = t.field(MESSAGE_ROLLUP.MESSAGE_STATUS);
    Field<Integer> anzahl = t.field(MESSAGE_ROLLUP.ANZAHL);
    return glassfishDsl
        .select(prozess, status, DSL.sum(anzahl))
        .from(t)
        .where(mandantenkette(mandant, prozess))
        .groupBy(prozess, status)
        .fetch(
            satz ->
                new Prozesskennzahlzeile(satz.value1(), satz.value2(), satz.value3().longValue()));
  }

  /**
   * Die Spalten einer Rollup-Ebene.
   *
   * <p><b>Diese Zuordnung steht auch in {@code dashboard/DashboardRepository}, und das ist hier
   * hinnehmbar.</b> Sie liesse sich nicht nach {@code common} heben, ohne die generierten Tabellen
   * dorthin mitzunehmen — und ein Fundament, das an der Codegenerierung haengt, ist keines. Was die
   * Doppelung ungefaehrlich macht, ist das <b>vollstaendige {@code switch} ohne {@code default}</b>
   * an beiden Stellen: Eine vierte Rollup-Ebene loest an beiden einen Compilerfehler aus und keine
   * stille Voreinstellung. Offener Punkt 113, fortgeschrieben am 07.09.2026: Seit {@code
   * common/Rollupebene} den Namen der Ebene traegt, ist hier die Zuordnung Name → Tabelle, dort die
   * Zuordnung Paar → Tabelle.
   */
  private record Ebene(
      Table<?> tabelle, Field<String> prozess, Field<String> status, Field<Integer> anzahl) {}

  private static Ebene ebene(Rollupebene ebene) {
    return switch (ebene) {
      case STUNDE ->
          new Ebene(
              MESSAGE_ROLLUP,
              MESSAGE_ROLLUP.PROCESS_ID,
              MESSAGE_ROLLUP.MESSAGE_STATUS,
              MESSAGE_ROLLUP.ANZAHL);
      case TAG ->
          new Ebene(
              MESSAGE_ROLLUP_TAG,
              MESSAGE_ROLLUP_TAG.PROCESS_ID,
              MESSAGE_ROLLUP_TAG.MESSAGE_STATUS,
              MESSAGE_ROLLUP_TAG.ANZAHL);
      case MONAT ->
          new Ebene(
              MESSAGE_ROLLUP_MONAT,
              MESSAGE_ROLLUP_MONAT.PROCESS_ID,
              MESSAGE_ROLLUP_MONAT.MESSAGE_STATUS,
              MESSAGE_ROLLUP_MONAT.ANZAHL);
    };
  }

  /**
   * Das Bereichspraedikat eines Segments auf seiner Ebene — <b>ohne Funktion um die
   * Schluesselspalte</b>, sonst faellt der Bereichszugriff weg (Eigenschaft 3 aus §6).
   *
   * <p>{@code stunde} ist {@code DATETIME}, {@code tag} und {@code monat} sind {@code DATE} ({@code
   * docs/rollup.md} §2). Der Vergleichswert wird deshalb <b>in Java</b> auf das Datum geschnitten
   * und nicht in SQL — die Zerlegung stellt sicher, dass Tages- und Monatssegmente auf Mitternacht
   * liegen.
   */
  private static Condition bereich(Segment segment) {
    return switch (segment.ebene()) {
      case STUNDE ->
          MESSAGE_ROLLUP.STUNDE.ge(segment.von()).and(MESSAGE_ROLLUP.STUNDE.lt(segment.bis()));
      case TAG ->
          MESSAGE_ROLLUP_TAG
              .TAG
              .ge(segment.von().toLocalDate())
              .and(MESSAGE_ROLLUP_TAG.TAG.lt(segment.bis().toLocalDate()));
      case MONAT ->
          MESSAGE_ROLLUP_MONAT
              .MONAT
              .ge(segment.von().toLocalDate())
              .and(MESSAGE_ROLLUP_MONAT.MONAT.lt(segment.bis().toLocalDate()));
    };
  }

  /**
   * <b>Regel M3, als Bestandteil des Statements und nicht als nachgelagerte Pruefung.</b>
   *
   * @param prozessSpalte die {@code process_id} der jeweiligen Rollup-Ebene — oder die der
   *     Ableitung, wenn mehrere Ebenen zusammenlaufen
   */
  private static Condition mandantenkette(MandantContext mandant, Field<String> prozessSpalte) {
    return DSL.exists(
        DSL.selectOne()
            .from(KETTE_PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(KETTE_PROCESS.PROJECTID))
            .where(KETTE_PROCESS.PROCESSID.eq(prozessSpalte))
            .and(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())));
  }
}
