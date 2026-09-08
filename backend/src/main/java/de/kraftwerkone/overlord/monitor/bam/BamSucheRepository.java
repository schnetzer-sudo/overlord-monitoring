package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAM;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMTYPE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEPROPERTY;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOSACTION;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.BAM_SOLLAENGE;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Messagebam;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Messageproperty;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Sos;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 *
 * <h2>Seit dem 08.09.2026: die Property-Suche im selben Kern</h2>
 *
 * <p>Derselbe Kern trägt seither auch die <b>Feldbegriffe</b> ({@link Feldbedingung}) — eine
 * Suchfläche, zwei Quellen (E‑99). Ein Feldbegriff, dessen Name eine <b>Spalte</b> benennt (Typ 0,
 * {@link Typ0Feld}), wird zum Spaltenprädikat auf {@code Message}, {@code Process} oder {@code SOS}
 * und fasst {@code MessageProperty} nicht an. Jeder andere wird zum <b>EAV-Zugriff</b>: ein Join
 * auf {@code MessageProperty} je Begriff, {@code MessagePropertyName = ? AND MessagePropertyValue =
 * ?} — die Fassung aus M166, <b>Einstieg über den Wertindex</b>, und damit die erste benannte
 * Ausnahme von Leistungsregel 4 (E‑102, {@code PROJEKTBESCHREIBUNG.md} §8). Der L4‑konforme Pfad
 * ist in vier Fassungen erzwungen gemessen worden und trägt die Suche nicht: über 30 Tage 3.186 bis
 * 8.195 ms gegen 789 bis 1.862 ms über den Wertindex, über 90 Tage Abbruch in jeder Form (M168,
 * M171).
 *
 * <p><b>Ohne Feldbegriffe ist das Statement Zeichen für Zeichen das von Teil 2b.</b> Die führende
 * Tabelle bleibt {@code b1}, die Bedingungen stehen in derselben Reihenfolge, nichts kommt hinzu;
 * {@code BamSucheStatementsTest} hält das gegen den eingefrorenen Text fest. Erst ein Feldbegriff
 * fügt Joins und Prädikate an — <b>hinter</b> den BAM-Tabellen und <b>vor</b> dem Zeitfenster.
 *
 * <p><b>Kein {@code STRAIGHT_JOIN}, auch hier.</b> M171 zeigt, dass er den kleinen Mandanten am
 * härtesten trifft: {@code SUTTONS} über 90 Tage durch 680.872 Indexeinträge für 64.553 Treffer,
 * Faktor 3,50 gegen die Form, die die Mandantenkette zulässt. Der Optimizer wählt selbst — und
 * wählt zwischen Zeit- und Werteinstieg je nach Fenster verschieden (M166).
 *
 * <p><b>Seit dem 08.09.2026, Teil 2: {@code Message.ProcessName} über die Stammdaten</b> (E‑109).
 * Der Name wird <b>vor</b> dem Kern zu {@code ProcessID}s aufgelöst ({@link #prozessKennungen}) und
 * im Kern als {@code ProcessID IN (…)} gesucht — die Form, die die Nachrichtenliste für ihren
 * Freitext seit Schritt 4 verwendet. Der Join {@code feld_process} aus Teil 1 ist damit entfallen;
 * die Rücknahme der Bauvorgabe steht datiert in {@code docs/property-suche.md} §10.
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

  /** Das Präfix der Aliasse für den EAV-Zugriff je Feldbegriff: {@code mp1}, {@code mp2}, … */
  private static final String FELD_ALIAS = "mp";

  /**
   * Der Alias für das eine Typ‑0‑Feld, das nicht auf {@code Message} liegt und <b>im Kern</b>
   * gejoint wird: {@code Message.SOSName} wohnt in {@code SOS} (M155). Ein eigener Alias, weil
   * {@code SOS} über der Deckelung noch einmal für den Anzeigenamen steht.
   *
   * <p><b>{@code Message.ProcessName} hat seit dem 08.09.2026 keinen Alias mehr</b> — es wird nicht
   * gejoint, sondern <b>vorab über die Stammdaten aufgelöst</b> ({@link #prozessKennungen}). Für
   * {@code SOSName} hülfe derselbe Umbau nicht: {@code Message} trägt keinen Index auf {@code
   * SOSID}, die Kennung liest das Fenster ebenso (docs/property-suche.md §6.4 und §10).
   */
  private static final Sos FELD_SOS = SOS.as("feld_sos");

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
    return findeTreffer(mandant, bedingungen, List.of(), fenster);
  }

  /**
   * <b>Abfrage (a) mit Feldbegriffen</b> — die Property-Suche im selben Kern.
   *
   * <p>Beide Listen dürfen leer sein, aber nicht beide zugleich: Eine Suche ohne Bedingung läse das
   * ganze Fenster, und die gibt es an diesem Endpunkt nicht. Ohne Feldbegriffe ist das Statement
   * dasselbe wie in {@link #findeTreffer(MandantContext, List, Zeitfenster)} — Zeichen für Zeichen.
   *
   * @param feldbedingungen die Feldbegriffe, je als Spaltenprädikat oder EAV-Zugriff
   */
  public List<BamTrefferZeile> findeTreffer(
      MandantContext mandant,
      List<Suchbedingung> bedingungen,
      List<Feldbedingung> feldbedingungen,
      Zeitfenster fenster) {

    Field<String> messageId = kernfeld(MESSAGE.MESSAGEID);
    Field<LocalDateTime> zeitpunkt = kernfeld(MESSAGE.MESSAGELASTUPDATE);
    Field<String> processId = kernfeld(MESSAGE.PROCESSID);
    Field<String> sosId = kernfeld(MESSAGE.SOSID);
    Field<Short> sosActionId = kernfeld(MESSAGE.SOSACTIONID);

    try {
      // Message.ProcessName: erst die Stammdaten, dann das Fenster (E-109). Trifft ein Name
      // keinen Prozess des Mandanten, ist die Verundung leer — und Message wird nicht angefasst.
      Map<String, List<String>> prozessKennungen = prozessKennungen(mandant, feldbedingungen);
      if (prozessKennungen.values().stream().anyMatch(List::isEmpty)) {
        return List.of();
      }

      Table<?> kern = kern(mandant, bedingungen, feldbedingungen, prozessKennungen, fenster);

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
      MandantContext mandant,
      List<Suchbedingung> bedingungen,
      List<Feldbedingung> feldbedingungen,
      Map<String, List<String>> prozessKennungen,
      Zeitfenster fenster) {
    if (bedingungen.isEmpty() && feldbedingungen.isEmpty()) {
      throw new IllegalArgumentException("Eine Suche ohne Bedingung gibt es nicht");
    }

    List<Messagebam> tabellen = new ArrayList<>(bedingungen.size());
    for (int i = 0; i < bedingungen.size(); i++) {
      tabellen.add(MESSAGEBAM.as(BEGRIFF_ALIAS + (i + 1)));
    }
    List<Feldbedingung> eigenschaften =
        feldbedingungen.stream().filter(f -> !f.istSpalte()).toList();
    List<Feldbedingung> spalten =
        feldbedingungen.stream().filter(Feldbedingung::istSpalte).toList();
    List<Messageproperty> eigenschaftstabellen = new ArrayList<>(eigenschaften.size());
    for (int i = 0; i < eigenschaften.size(); i++) {
      eigenschaftstabellen.add(MESSAGEPROPERTY.as(FELD_ALIAS + (i + 1)));
    }

    // Die fuehrende Tabelle im Text: b1, sonst mp1, sonst Message. Fuer den Optimizer ist das
    // folgenlos (kein STRAIGHT_JOIN); fuer den BAM-Pfad heisst es, dass sein Statement ohne
    // Feldbegriffe Zeichen fuer Zeichen das von Teil 2b bleibt.
    SelectJoinStep<Record> schritt;
    if (!tabellen.isEmpty()) {
      Messagebam erste = tabellen.getFirst();
      schritt = glassfishDsl.select(KERNFELDER).from(erste);
      for (int i = 1; i < tabellen.size(); i++) {
        Messagebam weitere = tabellen.get(i);
        schritt = schritt.join(weitere).on(weitere.MESSAGEID.eq(erste.MESSAGEID));
      }
      for (Messageproperty eigenschaft : eigenschaftstabellen) {
        schritt = schritt.join(eigenschaft).on(eigenschaft.MESSAGEID.eq(erste.MESSAGEID));
      }
      schritt = schritt.join(MESSAGE).on(MESSAGE.MESSAGEID.eq(erste.MESSAGEID));
    } else if (!eigenschaftstabellen.isEmpty()) {
      Messageproperty erste = eigenschaftstabellen.getFirst();
      schritt = glassfishDsl.select(KERNFELDER).from(erste);
      for (int i = 1; i < eigenschaftstabellen.size(); i++) {
        Messageproperty weitere = eigenschaftstabellen.get(i);
        schritt = schritt.join(weitere).on(weitere.MESSAGEID.eq(erste.MESSAGEID));
      }
      schritt = schritt.join(MESSAGE).on(MESSAGE.MESSAGEID.eq(erste.MESSAGEID));
    } else {
      schritt = glassfishDsl.select(KERNFELDER).from(MESSAGE);
    }
    // Das eine Typ-0-Feld, das im Kern eine andere Tabelle braucht: ein Join, auch wenn derselbe
    // Name zweimal genannt ist. ProcessName ist hier seit dem 08.09.2026 nicht mehr dabei.
    if (spalten.stream().anyMatch(f -> f.spalte() == Typ0Feld.SOS_NAME)) {
      schritt = schritt.join(FELD_SOS).on(FELD_SOS.SOSID.eq(MESSAGE.SOSID));
    }

    List<Condition> wo = new ArrayList<>();
    for (int i = 0; i < bedingungen.size(); i++) {
      wo.add(begriffsbedingung(tabellen.get(i), bedingungen.get(i)));
    }
    for (int i = 0; i < eigenschaften.size(); i++) {
      wo.add(eigenschaftsbedingung(eigenschaftstabellen.get(i), eigenschaften.get(i)));
    }
    for (Feldbedingung spalte : spalten) {
      wo.add(spaltenbedingung(spalte, prozessKennungen));
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
   * Ein Begriff: sein Wertprädikat, dazu die Typangabe, falls eine da ist.
   *
   * <p><b>Die Typangabe steht in der {@code WHERE}-Klausel und nicht hinter dem Limit.</b> M36
   * misst sie mit +1,5 bis +4 Prozent — im Rauschen; sie darf also dort stehen, wo sie fachlich
   * hingehört. Beschleunigen tut sie <b>nicht</b>, und das darf nirgends vorausgesetzt werden.
   *
   * <p><b>Das Wertprädikat ist die einzige Stelle, an der sich die beiden Modi unterscheiden.</b>
   * Alles andere — Mandantenfilter, {@code GROUP BY}, Deckelung, die vier Anzeigetabellen über der
   * Deckelung, die Sortierung, <b>kein {@code STRAIGHT_JOIN}</b> — bleibt Zeichen für Zeichen
   * gleich. M49‑3 hat belegt, dass der Optimierer auch mit {@code LIKE} über den seltensten Begriff
   * einsteigt, auch wenn dieser an fünfter Stelle steht; Regel L15 ist damit gemessen und nicht
   * angenommen.
   */
  private static Condition begriffsbedingung(Messagebam tabelle, Suchbedingung bedingung) {
    Condition werte =
        bedingung.modus() == Suchmodus.PRAEFIX
            ? praefixbedingung(tabelle, bedingung)
            : tabelle.MESSAGEBAMVALUE.in(bedingung.werte());
    return bedingung.typ() == null ? werte : werte.and(tabelle.MESSAGEBAMTYPE.eq(bedingung.typ()));
  }

  /**
   * Aus {@code MessageBAMValue IN (…)} wird {@code (MessageBAMValue LIKE ? ESCAPE '\' OR …)} — ein
   * Zweig je Fassung, verodert.
   *
   * <p><b>Das {@code ESCAPE} ist Pflicht und keine Vorsichtsmaßnahme.</b> M49‑4 hat gezählt, dass
   * {@code _} in 2.696 Werten des Bestands steht; ohne Maskierung wäre jedes davon in einer Eingabe
   * ein Platzhalter. Die Muster kommen fertig maskiert aus {@link Suchbedingung#muster()} — das
   * Repository hängt hier nichts an und schneidet nichts ab.
   *
   * <p><b>Der Plan bleibt derselbe.</b> M49‑3 misst über elf Fälle {@code range} auf {@code
   * MessageBAM_BAMValueOnly} und den Einstieg weiter über den seltensten Begriff. <b>M50 zeigt
   * allerdings die Grenze dieser Aussage</b>: Beim schlimmsten Präfix des Bestands kippt der
   * Optimierer auf {@code MessageLastUpdateIDX} und probt {@code MessageBAM} über den
   * Primärschlüssel — er wählt also weiter selbst, aber er wählt etwas anderes. Auch das ist ein
   * Grund, hier <b>keinen</b> {@code STRAIGHT_JOIN} nachzurüsten.
   */
  private static Condition praefixbedingung(Messagebam tabelle, Suchbedingung bedingung) {
    Condition muster = DSL.noCondition();
    for (String eines : bedingung.muster()) {
      muster = muster.or(tabelle.MESSAGEBAMVALUE.like(eines, Suchbedingung.ESCAPE));
    }
    return muster;
  }

  /**
   * <b>Der EAV-Zugriff eines Feldbegriffs: Name und Wert, beide {@code =}.</b> Das ist die Fassung
   * aus M166 und die benannte Ausnahme von Leistungsregel 4 (E‑102): Der Einstieg läuft über den
   * Wertindex — {@code MessagePropertyNameValueIDX} oder {@code MessagePropertyValueIDX}, das wählt
   * der Optimizer (M165: in zwei von sechs Fällen den reinen Wertindex, folgenlos) —, ein
   * Präfixindex über 50 Zeichen. Bei längeren Werten prüft MariaDB den Rest auf der Zeile nach; das
   * kostet bei eindeutigem Präfix nichts Messbares (M167).
   *
   * <p><b>Kein {@code LIKE}, kein Muster, keine Maskierung</b> — weil kein Präfixmodus über {@code
   * MessagePropertyValue} gebaut und keiner gemessen ist. {@code modus=praefix} wirkt
   * ausschließlich auf die BAM-Begriffe; {@code BamSucheStatementsTest} hält fest, dass auf dieser
   * Spalte nie ein {@code LIKE} steht.
   */
  private static Condition eigenschaftsbedingung(Messageproperty tabelle, Feldbedingung bedingung) {
    return tabelle
        .MESSAGEPROPERTYNAME
        .eq(bedingung.name())
        .and(tabelle.MESSAGEPROPERTYVALUE.eq(bedingung.wert()));
  }

  /**
   * <b>Das Spaltenprädikat eines Typ‑0‑Felds</b> — {@code MessageProperty} wird für diese Namen
   * nicht angefasst (E‑101). Die Zuordnung Name → Spalte ist die aus {@link Typ0Feld}, hier in
   * jOOQ-Feldern, weil nur Repository-Klassen die generierten Tabellen sehen.
   *
   * <p>{@code Message.Status} vergleicht gegen den <b>Rohwert</b> von {@code MessageStatus} und
   * nicht gegen eine Einordnung — der Nutzer hat einen konkreten Wert getippt, und die Suche findet
   * genau ihn. Die Übersetzung Einordnung → Bedingung gehört der Nachrichtenliste.
   */
  private static Condition spaltenbedingung(
      Feldbedingung bedingung, Map<String, List<String>> prozessKennungen) {
    String wert = bedingung.wert();
    return switch (bedingung.spalte()) {
      case MESSAGE_ID -> MESSAGE.MESSAGEID.eq(wert);
      case MESSAGE_ID_SOURCE -> MESSAGE.SOURCEMESSAGEID.eq(wert);
      case MESSAGE_ID_TARGET -> MESSAGE.TARGETMESSAGEID.eq(wert);
      case PROCESS_ID -> MESSAGE.PROCESSID.eq(wert);
      // Nicht der Name ueber einen Join, sondern die vorab aufgeloesten Kennungen (E-109). Bei
      // genau einer Kennung macht MariaDB aus IN (?) ein = ?, und die Mandantenkette wird zur
      // Konstante — das ist die 3-ms-Form aus docs/property-suche.md §6.4.
      case PROCESS_NAME -> MESSAGE.PROCESSID.in(prozessKennungen.get(wert));
      case SOS_ID -> MESSAGE.SOSID.eq(wert);
      case SOS_NAME -> FELD_SOS.SOSNAME.eq(wert);
      case STATUS -> MESSAGE.MESSAGESTATUS.eq(wert);
    };
  }

  /**
   * <b>{@code Message.ProcessName}, vorab über die Stammdaten aufgelöst</b> — je genanntem Namen
   * die {@code ProcessID}s, die ihn beim aktiven Mandanten tragen (E‑109, 08.09.2026).
   *
   * <p><b>Warum nicht der Join.</b> Gemessen kostete {@code Message.ProcessName} über den Join
   * {@code feld_process} <b>4.592 ms</b> über 30 Tage und brach über ein Jahr in allen sechs Läufen
   * ab, während {@code Message.ProcessID} — <b>dieselbe Menge</b> — <b>3 ms</b> kostete ({@code
   * docs/property-suche.md} §6.4). Der Unterschied ist allein, wo die Mandantenkette ausgewertet
   * wird: Steht die Kennung als Konstante im Statement, wird {@code EXISTS (… WHERE ProcessID = ?)}
   * zur Konstante und der Zeitindex nur bis zur Deckelung gelesen; kommt der Name über den Join,
   * läuft die Kette je Zeile und das ganze Fenster geht in die temporäre Tabelle.
   *
   * <p><b>Dieselbe Form wie der Freitextfilter der Nachrichtenliste</b> ({@code
   * NachrichtenRepository.loeseSucheAuf}, {@code docs/nachrichtenliste.md} §5: „niemals gegen
   * {@code Message}"): {@code Process} trägt 1.503 Zeilen, die Auflösung kostet nichts Messbares.
   * Anders als dort wird hier <b>exakt</b> verglichen ({@code =}, kein {@code LIKE}, keine
   * Maskierung) — der Nutzer hat einen konkreten Namen gewählt, und die Suche findet genau ihn.
   *
   * <p><b>Der Mandantenfilter steht auch hier</b>, aus demselben Grund wie in der Liste: nicht als
   * Sicherheitsgrenze (die trägt das Hauptstatement, Regel M3), sondern damit ein Prozessname eines
   * fremden Mandanten gar nicht erst zu einer Kennung wird. {@code FeldSucheIsolationDbIT} prüft
   * beides.
   *
   * <p><b>Keine Deckelung der Kennungsliste.</b> Sie ist durch die Tabelle begrenzt (1.503 Zeilen,
   * davon ein Bruchteil je Mandant), und ein Name trägt in aller Regel genau eine Kennung; eine
   * Deckelung änderte still die Treffermenge. Gemessen ist die Form mit einer Kennung (§10).
   *
   * @return je verschiedenem Prozessnamen unter den Feldbegriffen seine Kennungen — <b>leer</b>,
   *     wenn kein Prozess des Mandanten so heißt. Dann ist die Verundung leer, und der Aufrufer
   *     stellt kein Statement gegen {@code Message}
   */
  private Map<String, List<String>> prozessKennungen(
      MandantContext mandant, List<Feldbedingung> feldbedingungen) {
    Map<String, List<String>> kennungen = new LinkedHashMap<>();
    for (Feldbedingung bedingung : feldbedingungen) {
      if (bedingung.spalte() != Typ0Feld.PROCESS_NAME || kennungen.containsKey(bedingung.wert())) {
        continue;
      }
      List<String> gefunden =
          glassfishDsl
              .selectDistinct(PROCESS.PROCESSID)
              .from(PROCESS)
              .join(PROJECTMANDANT)
              .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
              .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
              .and(PROCESS.PROCESSNAME.eq(bedingung.wert()))
              .orderBy(PROCESS.PROCESSID)
              .fetch(PROCESS.PROCESSID);
      kennungen.put(bedingung.wert(), List.copyOf(gefunden));
    }
    return kennungen;
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
