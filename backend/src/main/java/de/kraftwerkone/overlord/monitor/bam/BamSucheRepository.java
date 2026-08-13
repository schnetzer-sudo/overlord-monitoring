package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAM;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMTYPE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOSACTION;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.BAM_SOLLAENGE;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Messagebam;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 * Die drei Statements der BAM-Suche — der Einstieg über den <b>Wert</b>, den Teil 1 ausdrücklich
 * ausgespart hat.
 *
 * <p><b>Jedes einzelne trägt den Mandantenfilter</b> (Regel M3), als {@code EXISTS} über {@code
 * Process → ProjectMandant} in genau der Form, die {@code NachrichtenRepository}, {@code
 * BamRepository} und die übrigen verwenden — nicht als nachgelagerte Prüfung. <b>Auch die zweite
 * Abfrage</b>, obwohl ihre {@code MessageID}s aus der ersten stammen: Eine Menge, die einmal
 * gefiltert war, bleibt es nicht dadurch, dass jemand es weiß.
 *
 * <h2>Die Abfrageform: Selbstjoin, kein {@code STRAIGHT_JOIN}, keine {@code HAVING}-Bauform</h2>
 *
 * <p><b>Selbstjoin auf {@code MessageBAM}</b>, ein zusätzlicher Join je weiterem Begriff. Der
 * Optimierer steigt in <i>jeder</i> gemessenen Konstellation über den <b>selteneren</b> Begriff ein
 * und tauscht die Tabellen selbst (M42‑1) — die Verundung eines seltenen mit dem schlimmsten Wert
 * der Runde kostet <b>0,672 ms</b> statt 10.646 ms.
 *
 * <p><b>Kein {@code STRAIGHT_JOIN}, und das steht gegen die ursprüngliche Erwartung.</b> Er ist
 * hier nicht die Abhilfe, sondern der Schaden: Mit ihm wird die Reihenfolge bindend und die
 * Eingabereihenfolge des Nutzers zur Leistungsfrage — gemessen Faktor <b>219</b> bei zwei und
 * <b>1.094</b> bei fünf Begriffen (M42‑1, M42‑2). Dass er in der Nachrichtenliste vorkommt, ist
 * kein Grund, ihn hier nachzurüsten; dort löste er eine andere Lage (L15).
 *
 * <p><b>Nicht die Bauform {@code IN} plus {@code GROUP BY … HAVING COUNT(DISTINCT …)}.</b> Sie
 * kommt ohne n Joins aus und ist die, die man zuerst schreibt — sie fällt aber in vier von acht
 * gemessenen Konstellationen ab, im Normalfall um Faktor <b>873</b> (K2) bis <b>954</b> (fünf
 * Begriffe). Der Selbstjoin verliert nur in einer (K5c), und beide schlechten Fälle liegen bei rund
 * 900 ms. Die beiden Bauformen sind <b>gegenläufig, nicht gestuft</b> (M42‑3): Die eine bezahlt die
 * Trefferzahl des seltensten Begriffs mal die BAM-Werte je Kandidatennachricht, die andere die
 * Summe aller Trefferzahlen.
 *
 * <h2>Warum die Anzeigespalten erst über der Deckelung hängen</h2>
 *
 * <p>Der Kern liefert höchstens {@link #HOECHSTENS_TREFFER}+1 Zeilen; {@code Process}, {@code
 * Project}, {@code SOS} und {@code SOSAction} hängen <b>darüber</b> und nicht daneben. Das ist
 * derselbe Befund wie in Teil 1 ({@code docs/bam-werte.md} §4): Dort kostete die naheliegende
 * Fassung mit den Stammdaten-Joins neben {@code MessageBAM} das <b>Achtzehnfache</b>, weil die
 * Nachschlagevorgänge je Zeile liefen statt je Gruppe. Hier wäre der Unterschied größer, nicht
 * kleiner — der schlimmste gemessene Wert erzeugt <b>234.159</b> Kandidatenzeilen (M33).
 */
@Repository
public class BamSucheRepository {

  /**
   * Das <b>harte Limit</b> der Trefferliste. Kein Cursor, kein Nachladen.
   *
   * <p>Erkannt wird die Abschneidung über die {@code n+1}-te Zeile, wie bei jeder Seite dieses
   * Projekts — und damit <b>nach</b> dem Mandantenfilter, weil der im Statement steht. Eine Meldung
   * auf Basis der Rohtreffer sagte einem Nutzer etwas über die Datenmenge fremder Mandanten: genau
   * die Sorte Leck, gegen die die 404-Regel beim Mandantenwechsel gebaut ist.
   */
  public static final int HOECHSTENS_TREFFER = 50;

  /** Der Name der abgeleiteten Tabelle mit den gedeckelten Treffern — an einer Stelle. */
  private static final String KERN = "treffer";

  /** Das Präfix der Selbstjoin-Aliasse: {@code b1}, {@code b2}, … */
  private static final String BEGRIFF_ALIAS = "b";

  /**
   * Der Alias für die Mandantenkette. Er muss ein anderer sein als der der äußeren Abfrage — dort
   * hängt {@code Process} schon für den Anzeigenamen.
   *
   * <p>Der Typ {@code Process} ist die generierte Tabelle des Quellschemas; der Import verdeckt in
   * dieser Datei {@code java.lang.Process}.
   */
  private static final Process MANDANTEN_PROCESS = PROCESS.as("mandanten_process");

  /**
   * Die Spalten, die der Kern liefert — <b>ausschließlich aus {@code Message}</b>.
   *
   * <p>Die vier Verkettungsspalten stehen dabei, weil sie <b>nichts kosten</b> (E4): Sie liegen auf
   * derselben Zeile. {@code SOSID} und {@code SOSActionID} sind keine Antwortfelder, sondern die
   * Schlüssel, über die die Anzeigenamen darüber angebunden werden.
   */
  private static final List<SelectFieldOrAsterisk> KERNFELDER =
      List.of(
          MESSAGE.MESSAGEID,
          MESSAGE.MESSAGELASTUPDATE,
          MESSAGE.MESSAGESTATUS,
          MESSAGE.PROCESSID,
          MESSAGE.SOSID,
          MESSAGE.SOSACTIONID,
          MESSAGE.SOURCE,
          MESSAGE.SOURCEMESSAGEID,
          MESSAGE.TARGETMESSAGEID,
          MESSAGE.TARGET);

  private final DSLContext glassfishDsl;

  BamSucheRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Die Sollängen-Kuratierung dieses Mandanten — <b>ein Vollabzug, kein Join</b>.
   *
   * <p>{@code overlord_monitor.bam_sollaenge} hat sechzehn Zeilen und wird <b>nie</b> gegen {@code
   * GlassfishDB} gejoint ({@code docs/bam-sollaengen.md} §6). Die Suchvarianten entstehen aus ihr
   * <b>vor</b> dem Statement (siehe {@link Sollaengen}), nicht darin.
   *
   * <p><b>Der Mandant ist erster Pflichtparameter, obwohl hier kein Quellschema angefasst wird</b>
   * (Regel M2). Der Schlüssel der Tabelle <i>trägt</i> den Mandanten — eine Methode ohne ihn läse
   * die Kuratierung fremder Mandanten, und ihre Sollängen bestimmten, wonach gesucht wird. Dass
   * ArchUnit die Regel nur für {@code jooq.glassfish} erzwingt, ist der Grund für diesen Absatz und
   * nicht für eine Ausnahme. Gelesen wird über den <b>Lese-Kontext</b>; der darf beide Schemata
   * lesen ({@code docs/datenzugriff.md} §3).
   *
   * <p><b>{@code sollaenge} ist {@code NULL}-fähig</b> und kommt als {@code org.jooq.types.UByte} —
   * der Preis für {@code TINYINT UNSIGNED}. {@code .intValue()} macht die Zahl daraus.
   */
  public List<BamSollaengeZeile> findeSollaengen(MandantContext mandant) {
    return glassfishDsl
        .select(
            BAM_SOLLAENGE.MESSAGE_BAM_TYPE,
            BAM_SOLLAENGE.SOLLAENGE,
            BAM_SOLLAENGE.FUEHRENDES_LEERZEICHEN)
        .from(BAM_SOLLAENGE)
        .where(BAM_SOLLAENGE.MANDANT_ID.eq(mandant.mandantId()))
        .orderBy(BAM_SOLLAENGE.MESSAGE_BAM_TYPE)
        .fetch(
            satz ->
                new BamSollaengeZeile(
                    satz.value1(), sollaenge(satz.value2()), Boolean.TRUE.equals(satz.value3())));
  }

  private static Integer sollaenge(UByte roh) {
    return roh == null ? null : roh.intValue();
  }

  /**
   * <b>Abfrage (a): die Trefferzeilen</b> — höchstens {@link #HOECHSTENS_TREFFER}+1, absteigend
   * nach Zeitpunkt.
   *
   * <p><b>Verdichtung auf {@code MessageID}.</b> M37 misst, dass bei 4,17 Prozent der Paare
   * derselbe Wert unter mehreren Typen steht; ohne die Verdichtung stünde dieselbe Nachricht
   * mehrfach in der Liste. Sie kostet nichts — {@code GROUP BY} und {@code ORDER BY … DESC} legen
   * ohnehin eine temporäre Tabelle an.
   *
   * <p><b>Das Zeitfenster ist Pflicht</b> (Regel L1) und wirkt hier über die Größe genau dieser
   * temporären Tabelle: Nicht der Indexzugriff wird billiger, sondern das, was danach kommt. Beim
   * schlimmsten gemessenen Wert kostet ein Tagesfenster 90,5 ms, ein Monat 1.652,0 ms, ein Jahr
   * 8.664,4 ms und kein Fenster 10.752,8 ms — die Laufzeit folgt der Zahl der überlebenden Zeilen
   * fast linear (M35).
   */
  public List<BamTrefferZeile> findeTreffer(
      MandantContext mandant, List<Suchbedingung> bedingungen, Zeitfenster fenster) {

    Table<?> kern = kern(mandant, bedingungen, fenster);

    Field<String> messageId = kernfeld(MESSAGE.MESSAGEID);
    Field<LocalDateTime> zeitpunkt = kernfeld(MESSAGE.MESSAGELASTUPDATE);
    Field<String> processId = kernfeld(MESSAGE.PROCESSID);
    Field<String> sosId = kernfeld(MESSAGE.SOSID);
    Field<Short> sosActionId = kernfeld(MESSAGE.SOSACTIONID);

    try {
      return glassfishDsl
          .select(
              messageId,
              zeitpunkt,
              kernfeld(MESSAGE.MESSAGESTATUS),
              processId,
              PROCESS.PROCESSNAME,
              PROJECT.PROJECTNAME,
              SOS.SOSNAME,
              SOSACTION.SOSACTIONNAME,
              kernfeld(MESSAGE.SOURCE),
              kernfeld(MESSAGE.SOURCEMESSAGEID),
              kernfeld(MESSAGE.TARGETMESSAGEID),
              kernfeld(MESSAGE.TARGET))
          .from(kern)
          // Alle vier als LEFT JOIN und aus demselben Grund wie in der Nachrichtenliste: Der
          // Anzeigename darf nicht darueber entscheiden, ob eine gefundene Zeile erscheint.
          .leftJoin(PROCESS)
          .on(PROCESS.PROCESSID.eq(processId))
          .leftJoin(PROJECT)
          .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
          .leftJoin(SOS)
          .on(SOS.SOSID.eq(sosId))
          // Ueber BEIDE Spalten des zusammengesetzten Primaerschluessels — ueber die Kennung allein
          // waere es ein Kreuzprodukt: Die SOSActionID 2 gibt es 694-mal (M13).
          .leftJoin(SOSACTION)
          .on(SOSACTION.SOSID.eq(sosId))
          .and(SOSACTION.SOSACTIONID.eq(sosActionId))
          .orderBy(zeitpunkt.desc(), messageId.desc())
          .fetch(
              satz ->
                  new BamTrefferZeile(
                      satz.value1(),
                      satz.value2(),
                      satz.value3(),
                      satz.value4(),
                      satz.value5(),
                      satz.value6(),
                      satz.value7(),
                      satz.value8(),
                      satz.value9(),
                      satz.value10(),
                      satz.value11(),
                      satz.value12()));
    } catch (DataAccessException fehler) {
      throw anDerZeitgrenze(fehler);
    }
  }

  /**
   * Der gedeckelte Kern: der Selbstjoin, das Zeitfenster, der Mandantenfilter, die Verdichtung und
   * das Limit — alles, was über die <b>Menge</b> entscheidet, und nichts, was sie nur beschriftet.
   */
  private Table<?> kern(
      MandantContext mandant, List<Suchbedingung> bedingungen, Zeitfenster fenster) {

    List<Messagebam> tabellen = new ArrayList<>(bedingungen.size());
    for (int i = 0; i < bedingungen.size(); i++) {
      tabellen.add(MESSAGEBAM.as(BEGRIFF_ALIAS + (i + 1)));
    }
    Messagebam erste = tabellen.getFirst();

    SelectJoinStep<Record> schritt = glassfishDsl.select(KERNFELDER).from(erste);
    for (int i = 1; i < tabellen.size(); i++) {
      Messagebam weitere = tabellen.get(i);
      schritt = schritt.join(weitere).on(weitere.MESSAGEID.eq(erste.MESSAGEID));
    }
    schritt = schritt.join(MESSAGE).on(MESSAGE.MESSAGEID.eq(erste.MESSAGEID));

    List<Condition> wo = new ArrayList<>();
    for (int i = 0; i < bedingungen.size(); i++) {
      wo.add(begriffsbedingung(tabellen.get(i), bedingungen.get(i)));
    }
    wo.add(MESSAGE.MESSAGELASTUPDATE.ge(fenster.von()));
    wo.add(MESSAGE.MESSAGELASTUPDATE.le(fenster.bis()));
    wo.add(mandantenkette(mandant));

    return schritt
        .where(wo)
        // GROUP BY ueber den Primaerschluessel von Message; alle uebrigen Spalten haengen
        // funktional daran. Gemessen ist, dass @@sql_mode auf der Zielinstanz kein
        // ONLY_FULL_GROUP_BY fuehrt (messungen-schritt7.md §0 und M47).
        .groupBy(MESSAGE.MESSAGEID)
        .orderBy(MESSAGE.MESSAGELASTUPDATE.desc(), MESSAGE.MESSAGEID.desc())
        .limit(HOECHSTENS_TREFFER + 1)
        .asTable(KERN);
  }

  /**
   * Ein Begriff: die {@code IN}-Liste seiner Varianten, dazu die Typangabe, falls eine da ist.
   *
   * <p><b>Die Typangabe steht in der {@code WHERE}-Klausel und nicht hinter dem Limit.</b> M36
   * misst sie mit +1,5 bis +4 Prozent — im Rauschen; sie darf also dort stehen, wo sie fachlich
   * hingehört. Beschleunigen tut sie <b>nicht</b>, und das darf nirgends vorausgesetzt werden.
   */
  private static Condition begriffsbedingung(Messagebam tabelle, Suchbedingung bedingung) {
    Condition werte = tabelle.MESSAGEBAMVALUE.in(bedingung.werte());
    return bedingung.typ() == null ? werte : werte.and(tabelle.MESSAGEBAMTYPE.eq(bedingung.typ()));
  }

  /**
   * <b>Abfrage (b): welcher Typ mit welchem Wert getroffen hat</b> — für genau die Nachrichten aus
   * (a).
   *
   * <p><b>Warum das eine zweite Abfrage ist und kein {@code GROUP_CONCAT}.</b> Dieselbe Begründung
   * wie in Teil 1: Zusammengelegt hinge die Antwortgröße an einer Zahl, die niemand gemessen hat —
   * hier zusätzlich daran, wie viele Werte eine Trefferzeile trägt (bis zu 9.296 auf einer einzigen
   * Nachricht, M41). Die zweite Abfrage steigt über den <b>Präfix des Primärschlüssels</b> von
   * {@code MessageBAM} ein, denselben Pfad, den M11, M26‑1b, M28‑2 und M39‑1 jedes Mal als {@code
   * Using index} gemessen haben.
   *
   * <p><b>Die Bedingung ist je Begriff dieselbe wie in (a)</b> — verodert statt verundet. Damit
   * stehen hier genau die Zeilen, die die Nachricht zum Treffer gemacht haben: Wer {@code 9012:123}
   * sucht, bekommt nicht die 123, die auf derselben Nachricht unter 9018 steht.
   *
   * @param messageIds höchstens {@link #HOECHSTENS_TREFFER} Kennungen aus (a). Leer heißt: keine
   *     Abfrage
   */
  public List<BamTrefferWertZeile> findeTrefferWerte(
      MandantContext mandant, List<String> messageIds, List<Suchbedingung> bedingungen) {
    if (messageIds.isEmpty()) {
      return List.of();
    }

    Condition getroffen = DSL.noCondition();
    for (Suchbedingung bedingung : bedingungen) {
      getroffen = getroffen.or(begriffsbedingung(MESSAGEBAM, bedingung));
    }

    try {
      return glassfishDsl
          .select(
              MESSAGEBAM.MESSAGEID,
              MESSAGEBAM.MESSAGEBAMTYPE,
              MESSAGEBAM.MESSAGEBAMVALUE,
              MESSAGEBAMTYPE.MESSAGEBAMTYPEDESCRIPTION)
          .from(MESSAGE)
          .join(MESSAGEBAM)
          .on(MESSAGEBAM.MESSAGEID.eq(MESSAGE.MESSAGEID))
          // LEFT JOIN: Fehlt die Zeile im Altsystem, verschwindet der Treffer nicht — er bekommt
          // seine Typnummer (Typbezeichnung). Die Stammdatentabelle hat 62 Zeilen.
          .leftJoin(MESSAGEBAMTYPE)
          .on(MESSAGEBAMTYPE.MESSAGEBAMTYPE_.eq(MESSAGEBAM.MESSAGEBAMTYPE))
          .where(MESSAGE.MESSAGEID.in(messageIds))
          .and(getroffen)
          .and(mandantenkette(mandant))
          .orderBy(
              MESSAGEBAM.MESSAGEID.asc(),
              MESSAGEBAM.MESSAGEBAMTYPE.asc(),
              MESSAGEBAM.MESSAGEBAMVALUE.asc())
          .fetch(
              satz ->
                  new BamTrefferWertZeile(
                      satz.value1(), satz.value2(), satz.value3(), satz.value4()));
    } catch (DataAccessException fehler) {
      throw anDerZeitgrenze(fehler);
    }
  }

  /**
   * Der Abbruch an {@code max_statement_time} — <b>ein absehbarer Fall und kein Systemfehler</b>.
   *
   * <p>Der Lese-Pool setzt {@code SET SESSION max_statement_time=10} ({@code docs/datenzugriff.md}
   * §1), und das Zeitfenster macht den Abbruch unwahrscheinlich, nicht unmöglich: Alle Zahlen aus
   * M35 und M42 stammen von einer <b>ruhenden</b> Testkopie, und der Jahresfall liegt dort bereits
   * bei 87 Prozent der Grenze. Unter Last läuft derselbe Fall wieder auf.
   *
   * <p><b>Es entsteht kein zweiter Problemtyp.</b> {@code suche-abgebrochen} ist der, den die
   * Nachrichtenliste seit Schritt 4 trägt, mit demselben Status und demselben Handlungshinweis —
   * die Suche benutzt denselben Weg und baut keinen eigenen. Dass die Übersetzung hier ein zweites
   * Mal im Code steht, ist die Paketregel und nicht Nachlässigkeit: Ein Fachpaket importiert nicht
   * aus einem Nachbarpaket. <b>Wandert sie nach {@code common}, wandert sie mit ihrem Test</b> —
   * dieselbe Abwägung wie bei der Existenzprüfung in {@code docs/bam-werte.md} §10.
   *
   * <p><b>Gefangen wird genau eine Ausnahme.</b> MariaDB meldet Fehler {@code 1969} mit SQLState
   * {@code 70100}, Connector/J 3.5 macht daraus eine {@link SQLTimeoutException}, und jOOQ verpackt
   * sie in eine {@link DataAccessException} mit genau dieser Ursache. Ein Syntaxfehler, ein
   * Verbindungsabriss oder ein fehlendes Recht kommen ebenfalls als {@code DataAccessException} an
   * und bleiben, was sie sind: technische Fehler mit {@code 500}.
   *
   * <p><b>Und anders als in der Nachrichtenliste ohne Bedingung.</b> Dort gilt der Pfad nur bei
   * gesetztem Suchbegriff, weil die Liste auch ohne einen aufgerufen wird. <b>Diesen Endpunkt gibt
   * es ohne Suchbegriff nicht</b> — jeder Aufruf trägt mindestens einen.
   */
  static RuntimeException anDerZeitgrenze(DataAccessException fehler) {
    if (fehler.getCause(SQLTimeoutException.class) == null) {
      return fehler;
    }
    return new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "suche-abgebrochen",
        "Suche abgebrochen",
        "Die Suche hat zu lange gedauert und wurde abgebrochen. Verkleinere den Zeitraum oder"
            + " nenne eine zweite Belegnummer.",
        "Statement der BAM-Suche an der Zeitgrenze abgebrochen");
  }

  /** Eine Spalte der abgeleiteten Tabelle, unter ihrem ursprünglichen Namen. */
  private static <T> Field<T> kernfeld(Field<T> quelle) {
    return DSL.field(DSL.name(KERN, quelle.getName()), quelle.getType());
  }

  /**
   * Die Mandantenkette {@code Message → Process → ProjectMandant} als {@code EXISTS} — Wort für
   * Wort dieselbe wie in {@code NachrichtenRepository}, {@code BamRepository} und {@code
   * KettenRepository}.
   *
   * <p>Als {@code EXISTS} und nicht als Join: {@code ProjectMandant} ist im Schema n:m, ein Join
   * könnte Zeilen vervielfachen, sobald ein Projekt mehreren Mandanten gehört. Heute tut er das
   * nicht (M3), aber eine Antwort, deren Zeilenzahl an einer Stammdatenpflege hängt, ist die
   * falsche Grundlage für eine Sicherheitsgrenze.
   *
   * <p><b>Hier hätte ein Join eine zweite Folge:</b> Er säße im Kern, also <i>vor</i> dem Limit —
   * die Vervielfachung fräße Plätze der Trefferliste, und die Abschneidung meldete zu früh „es gibt
   * mehr".
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
