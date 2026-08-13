package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAM;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMTYPE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;

import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die drei Statements des BAM-Blocks — die Existenzfrage und die <b>Zweiteilung</b> aus Zaehlung
 * und gedeckelten Werten.
 *
 * <p><b>Jedes einzelne traegt den Mandantenfilter</b> (Regel M3), als {@code EXISTS} ueber {@code
 * Process → ProjectMandant} in genau der Form, die {@code NachrichtenRepository}, {@code
 * NachrichtendetailRepository} und {@code KettenRepository} verwenden — nicht als nachgelagerte
 * Pruefung. <b>Auch die Zaehlung</b>: Naehme sie den Filter nicht, nennte die Antwort eine Zahl, zu
 * der sie keine Zeilen liefert.
 *
 * <p><b>Jedes Statement steigt ueber {@code Message} ein.</b> Es gibt keinen Weg in {@code
 * MessageBAM} hinein, der nicht durch die Nachricht und damit durch den Filter fuehrt. {@code
 * MessageBAM} wird ausschliesslich ueber die {@code MessageID} angefasst — <b>nie ueber den
 * Wert</b>; das ist Teil 2.
 *
 * <p><b>Kein Zeitfenster</b>, und das ist keine Nachlaessigkeit gegenueber Regel L1: Die gilt fuer
 * <i>Listen</i> ueber {@code Message}. Hier ist die Menge durch einen Primaerschluessel benannt —
 * dieselbe Begruendung wie beim Detail- und beim Ketten-Endpunkt. {@code MessageBAM} traegt zudem
 * gar keinen Zeitstempel ({@code datenmodell.md} §3), ein Fenster koennte hier also nur noch
 * ausschliessen, was der Aufrufer bereits gesagt hat.
 *
 * <p><b>Warum zwei Statements und nicht eines.</b> Die Zaehlung liefert die <i>wahre</i> Zahl je
 * Typ, die Wertabfrage hoechstens {@link #WERTE_JE_GRUPPE} Werte je Typ. Zusammengelegt gaebe es
 * nur eine der beiden Zahlen: Entweder die Antwort ist ungedeckelt, oder die Restangabe ist
 * geraten. Der Preis ist ein zweiter Zugriff ueber <b>denselben</b> Indexpfad; die Kosten stehen in
 * {@code docs/bam-werte.md} §4.
 */
@Repository
public class BamRepository {

  /**
   * Wie viele Werte eine Typgruppe hoechstens ausliefert — <b>die Obergrenze der Antwort</b>.
   *
   * <p><b>Zwanzig, und die Zahl ist gemessen.</b> Ueber Fenster B tragen 82,3 Prozent der Wurzeln
   * zwischen 6 und 20 Werten <i>insgesamt</i>, der Median einer Nachricht mit Werten liegt bei 8
   * Typen und das Maximum bei 18 (M41). Zwanzig Werte je Gruppe decken damit den Normalfall
   * vollstaendig ab und begrenzen die Antwort trotzdem hart: hoechstens 18 × 20 = <b>360
   * Zeilen</b>.
   *
   * <p><b>Der Deckel sitzt in der Gruppe und nicht ueber der Nachricht.</b> Eine Gesamtdeckelung
   * waere bei 9.296 Werten auf einer Nachricht unbrauchbar — sie schnitte willkuerlich mitten in
   * eine Gruppe, und der Nutzer saehe von einem Typ alles und vom naechsten nichts.
   *
   * <p><b>Der Deckel haengt nicht an der Rolle</b>, obwohl die Rollen extrem verschieden sind (0
   * Prozent bei Merge-Eingaengen, 100 Prozent bei Merge-Ergebnissen, M41). Eine rollenabhaengige
   * Grenze waere fuer den Nutzer unerklaerlich, und die Rolle steht ohnehin schon im Kettenblock.
   */
  public static final int WERTE_JE_GRUPPE = 20;

  /** Der Name der abgeleiteten Tabelle in {@link #findeWerte} — an einer Stelle. */
  private static final String GEDECKELT = "gedeckelt";

  /** Der Name der laufenden Nummer je Typgruppe. */
  private static final String RANG = "rang";

  /** Der Name der abgeleiteten Tabelle in {@link #zaehleJeTyp}. */
  private static final String GEZAEHLT = "gezaehlt";

  private static final String TYP = "typ";

  private static final String GESAMT = "gesamt";

  /**
   * Der Alias fuer die Mandantenkette. Er muss ein anderer sein als der der aeusseren Abfrage — ein
   * gleichnamiger wuerde die Unterabfrage an sich selbst binden.
   *
   * <p>Der Typ {@code Process} ist die generierte Tabelle des Quellschemas; der Import verdeckt in
   * dieser Datei {@code java.lang.Process}.
   */
  private static final Process MANDANTEN_PROCESS = PROCESS.as("mandanten_process");

  private final DSLContext glassfishDsl;

  BamRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Gibt es die Nachricht fuer diesen Mandanten?
   *
   * <p><b>Ohne sie waere die Antwort auf eine fremde Nachricht eine leere Gruppenliste statt {@code
   * 404}</b> — und „leer" und „gibt es nicht" waeren zwei verschiedene Auskuenfte, aus denen sich
   * der Bestand abfragen liesse. Der Unterschied ist hier besonders greifbar: <b>80,6 Prozent aller
   * Nachrichten tragen keinen einzigen BAM-Wert</b> (M41), eine leere Antwort ist also der
   * Normalfall und darf gerade deshalb nicht „nicht vorhanden" heissen.
   *
   * <p>Dieselbe Reihenfolge und dasselbe Statement wie beim Eigenschaften-Endpunkt. Dass es hier
   * ein zweites Mal steht, statt aus {@code message} geholt zu werden, ist die Paketregel: Ein
   * Fachpaket importiert nicht aus einem Nachbarpaket.
   */
  public boolean existiert(MandantContext mandant, String messageId) {
    return glassfishDsl.fetchExists(
        glassfishDsl
            .selectOne()
            .from(MESSAGE)
            .where(MESSAGE.MESSAGEID.eq(messageId))
            .and(mandantenkette(mandant)));
  }

  /**
   * <b>Abfrage (a): die Zaehlung je Typ</b> — die wahre Gesamtzahl, unabhaengig von der Deckelung.
   *
   * <p>Sie laeuft ausschliesslich ueber den Index: {@code ref} auf {@code PRIMARY} mit {@code Using
   * index}, derselbe Zugriff, den M11, M26‑1b, M28‑2 und M39‑1 jedes Mal gemessen haben. Kein
   * einziger Wert wird gelesen — gezaehlt werden Indexeintraege. Die beiden {@code LEFT JOIN}s
   * treffen reine Stammdaten (62 beziehungsweise 69 Zeilen, gemessen am 12.08.2026).
   *
   * <p><b>Die Zaehlung steht in einer abgeleiteten Tabelle, und das ist keine Kosmetik.</b> Die
   * erste Fassung jointe {@code MessageBAMType} und {@code MessageBAMMandant} direkt neben {@code
   * MessageBAM} und gruppierte darueber. Der Plan sah gut aus — {@code Using index} auf {@code
   * MessageBAM}, {@code eq_ref} auf beide Stammdatentabellen —, und trotzdem kostete sie auf der
   * fettesten Nachricht <b>68,9 Millisekunden</b>: Die beiden Nachschlagevorgaenge liefen <i>je
   * Zeile</i> statt je Gruppe, also 3.409-mal statt neunmal. Erst gruppieren, dann beschriften
   * senkt denselben Fall auf <b>3,8 ms</b> (Faktor 18) und den teuersten bekannten von 53,4 auf 8,3
   * ms. Die Zahlen stehen in {@code docs/bam-werte.md} §4.
   *
   * <p><b>Beide Joins sind {@code LEFT}, und beide aus einem eigenen Grund.</b> Fehlt die Zeile in
   * {@code MessageBAMType}, darf die Gruppe nicht verschwinden — sie bekommt statt der Beschriftung
   * ihre Typnummer. Und fehlt sie in {@code MessageBAMMandant}, erst recht nicht: <b>Die
   * Konfiguration ist eine Sichtbarkeitsentscheidung und keine Aussage ueber den Bestand</b> (M40).
   * {@code WOC} traegt 2.067 BAM-Zeilen unter Typ 9014, den seine Konfiguration nicht kennt — ein
   * {@code JOIN} statt eines {@code LEFT JOIN} liesse diesen Mandanten <b>nichts</b> sehen.
   *
   * <p><b>{@code MessageBAMMandant} wird auf den aktiven Mandanten eingeschraenkt, und zwar in der
   * Join-Bedingung.</b> Stuende die Bedingung im {@code WHERE}, waere aus dem {@code LEFT JOIN} ein
   * innerer geworden, und der unkonfigurierte Typ fiele wieder heraus.
   *
   * <p>Sortiert wird hier nur nach Typnummer — sie macht das Ergebnis reproduzierbar. <b>Die
   * fachliche Ordnung der Gruppen entsteht in {@link BamService#ORDNUNG}</b>, damit sie ohne
   * Datenbank pruefbar ist.
   */
  public List<BamTypZeile> zaehleJeTyp(MandantContext mandant, String messageId) {
    Table<?> gezaehlt =
        glassfishDsl
            .select(MESSAGEBAM.MESSAGEBAMTYPE.as(TYP), DSL.count().as(GESAMT))
            .from(MESSAGE)
            .join(MESSAGEBAM)
            .on(MESSAGEBAM.MESSAGEID.eq(MESSAGE.MESSAGEID))
            .where(MESSAGE.MESSAGEID.eq(messageId))
            .and(mandantenkette(mandant))
            .groupBy(MESSAGEBAM.MESSAGEBAMTYPE)
            .asTable(GEZAEHLT);

    Field<Short> typ = DSL.field(DSL.name(GEZAEHLT, TYP), Short.class);
    Field<Integer> gesamt = DSL.field(DSL.name(GEZAEHLT, GESAMT), Integer.class);

    return glassfishDsl
        .select(
            typ,
            MESSAGEBAMTYPE.MESSAGEBAMTYPEDESCRIPTION,
            gesamt,
            MESSAGEBAMMANDANT.MESSAGEBAMTYPESORTINDEX)
        .from(gezaehlt)
        .leftJoin(MESSAGEBAMTYPE)
        .on(MESSAGEBAMTYPE.MESSAGEBAMTYPE_.eq(typ))
        .leftJoin(MESSAGEBAMMANDANT)
        .on(MESSAGEBAMMANDANT.MESSAGEBAMTYPE.eq(typ))
        .and(MESSAGEBAMMANDANT.MANDANTID.eq(mandant.mandantId()))
        .orderBy(typ.asc())
        .fetch(satz -> new BamTypZeile(satz.value1(), satz.value2(), satz.value3(), satz.value4()));
  }

  /**
   * <b>Abfrage (b): die angezeigten Werte</b> — je Typ die ersten {@link #WERTE_JE_GRUPPE},
   * aufsteigend nach {@code MessageBAMValue}.
   *
   * <p><b>Die Deckelung sitzt im Statement.</b> {@code ROW_NUMBER() OVER (PARTITION BY
   * MessageBAMType ORDER BY MessageBAMValue)} nummeriert je Typ durch, die aeussere Bedingung
   * schneidet ab. Damit ist die Obergrenze eine Eigenschaft des Endpunkts und keine Hoffnung ueber
   * die Daten: hoechstens 18 Typen × 20 Werte.
   *
   * <p><b>Warum nicht alles laden und in Java deckeln.</b> Ueber Fenster B stehen auf einer
   * einzigen Nachricht bis zu <b>9.296</b> Werte (M41) — ausgerechnet auf einem <i>Kind</i>, obwohl
   * Kinder fast nie Werte tragen. Ueber den <b>ganzen Bestand ist das Maximum unbekannt</b>; M41
   * hat einen Monat gemessen. Der zweite Zugriff laeuft ueber denselben Index wie die Zaehlung und
   * kostet gemessen weniger als das Laden der ungedeckelten Menge.
   *
   * <p><b>Sortiert wird ueber den Wert und nicht ueber eine fachliche Ordnung.</b> Eine solche gibt
   * es nicht: {@code MessageBAM} traegt keine Reihenfolge und keinen Zeitstempel. Der Wert ist das
   * einzige stabile Kriterium — und ein stabiles ist noetig, damit zweimal dasselbe herauskommt.
   */
  public List<BamWertZeile> findeWerte(MandantContext mandant, String messageId) {
    Field<Integer> rang =
        DSL.rowNumber()
            .over(
                DSL.partitionBy(MESSAGEBAM.MESSAGEBAMTYPE)
                    .orderBy(MESSAGEBAM.MESSAGEBAMVALUE.asc()))
            .as(RANG);

    Table<?> gedeckelt =
        glassfishDsl
            .select(MESSAGEBAM.MESSAGEBAMTYPE, MESSAGEBAM.MESSAGEBAMVALUE, rang)
            .from(MESSAGE)
            .join(MESSAGEBAM)
            .on(MESSAGEBAM.MESSAGEID.eq(MESSAGE.MESSAGEID))
            .where(MESSAGE.MESSAGEID.eq(messageId))
            .and(mandantenkette(mandant))
            .asTable(GEDECKELT);

    Field<Short> typ =
        DSL.field(DSL.name(GEDECKELT, MESSAGEBAM.MESSAGEBAMTYPE.getName()), Short.class);
    Field<String> wert =
        DSL.field(DSL.name(GEDECKELT, MESSAGEBAM.MESSAGEBAMVALUE.getName()), String.class);

    return glassfishDsl
        .select(typ, wert)
        .from(gedeckelt)
        .where(DSL.field(DSL.name(GEDECKELT, RANG), Integer.class).le(WERTE_JE_GRUPPE))
        .orderBy(typ.asc(), wert.asc())
        .fetch(satz -> new BamWertZeile(satz.value1(), satz.value2()));
  }

  /**
   * Die Mandantenkette {@code Message → Process → ProjectMandant} als {@code EXISTS} — Wort fuer
   * Wort dieselbe wie in {@code NachrichtenRepository}, {@code NachrichtendetailRepository} und
   * {@code KettenRepository}.
   *
   * <p>Als {@code EXISTS} und nicht als Join: {@code ProjectMandant} ist im Schema n:m, ein Join
   * koennte Zeilen vervielfachen, sobald ein Projekt mehreren Mandanten gehoert. Heute tut er das
   * nicht (M3: alle 134 Projekte gehoeren genau einem Mandanten) — aber eine Antwort, deren
   * Zeilenzahl an einer Stammdatenpflege haengt, ist die falsche Grundlage fuer eine
   * Sicherheitsgrenze. <b>Hier waere die Folge doppelt sichtbar</b>: Die Zaehlung nennte ein
   * Vielfaches, und die gedeckelten Werte zeigten jeden Wert mehrfach.
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
}
