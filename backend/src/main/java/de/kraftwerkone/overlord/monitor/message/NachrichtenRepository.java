package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOSACTION;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.MessageStatusKind;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 * Die Nachrichtenliste — das einzige Statement des Projekts, das {@code Message} zeilenweise liest.
 *
 * <p><b>Der Mandantenfilter ist Bestandteil des Statements</b> (Regel M3), nicht nachgelagerte
 * Pruefung: Wer eine fremde {@code MessageID} errät, bekommt null Zeilen — die Zeile hat fuer ihn
 * nie existiert. Umgesetzt als {@code EXISTS} ueber {@code Process → ProjectMandant} und
 * <b>nicht</b> ueber die vorhandene View {@code MessageMandantID}; die Begruendung steht in {@code
 * docs/nachrichtenliste.md} und laesst sich in einem Satz sagen: Der Zugriffspfad der View ist mit
 * den Rechten dieser Anwendung strukturell nicht einsehbar (Fehler 1142 und 1345), und was nicht
 * einsehbar ist, kann Regel L7 nicht erfuellen.
 *
 * <p><b>Ohne Zeitfenster wird nicht gelesen</b> (Regel L1) und <b>ohne {@code OFFSET}</b>
 * geblättert (Regel L3). Beides steht nicht zur Wahl: Mit Zeitfenster arbeitet MariaDB im {@code
 * range}-Zugriff ueber {@code MessageLastUpdateIDX}, ohne wird jede Abfrage ueber {@code Message}
 * zu einem vollen Durchlauf.
 *
 * <p><b>Die BAM-Werte sind hier ausgezogen</b> (Nachbesserung zu Schritt 4). Sie waren eine zweite
 * Abfrage je Seite und sind jetzt gar keine mehr: Messung M11 zeigt, dass der bestbelegte der 40
 * konfigurierten Typen 16,25 Prozent der Nachrichten erreicht und die beiden kuratierten 98,93
 * beziehungsweise 96,97 Prozent der Zeilen leer lassen. Eine Spalte, die fast immer leer ist,
 * behauptet, es gaebe dort etwas zu sehen. <b>{@code bam_spalte}, die Migration und {@code
 * common/BamSpaltenRegel} bleiben unangetastet</b> — Schritt 7 baut die BAM-Suche darauf, mit einem
 * eigenen Statement im Paket {@code bam} (Fachpakete kennen einander nicht).
 */
@Repository
public class NachrichtenRepository {

  /**
   * Der Alias fuer die Mandantenkette. Er muss ein anderer sein als der der aeusseren Abfrage —
   * dort haengt {@code Process} schon fuer den Anzeigenamen.
   *
   * <p>Der Typ {@code Process} ist die generierte Tabelle des Quellschemas; der Import verdeckt in
   * dieser Datei {@code java.lang.Process}.
   */
  private static final Process MANDANTEN_PROCESS = PROCESS.as("mandanten_process");

  private final DSLContext glassfishDsl;
  private final MessageStatusClassifier statusClassifier;

  NachrichtenRepository(
      @Qualifier("glassfishDsl") DSLContext glassfishDsl,
      MessageStatusClassifier statusClassifier) {
    this.glassfishDsl = glassfishDsl;
    this.statusClassifier = statusClassifier;
  }

  /**
   * Eine Seite der Liste, {@code limit + 1} Zeilen lang — die Zusatzzeile beantwortet „gibt es eine
   * naechste Seite" ohne {@code COUNT} (Regel L2).
   */
  public List<NachrichtZeile> finde(MandantContext mandant, Nachrichtenabfrage abfrage) {
    try {
      return glassfishDsl
          .select(
              MESSAGE.MESSAGEID,
              MESSAGE.MESSAGELASTUPDATE,
              MESSAGE.MESSAGESTATUS,
              MESSAGE.PROCESSID,
              PROCESS.PROCESSNAME,
              PROJECT.PROJECTNAME,
              SOS.SOSNAME,
              SOSACTION.SOSACTIONNAME)
          .from(MESSAGE)
          // LEFT JOIN, obwohl die Mandantenkette einen Prozess ohnehin erzwingt: Der Anzeigename
          // darf nicht darueber entscheiden, ob eine Zeile erscheint.
          .leftJoin(PROCESS)
          .on(PROCESS.PROCESSID.eq(MESSAGE.PROCESSID))
          .leftJoin(PROJECT)
          .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
          // Der Ablaufname, seit der Nachbesserung zu Schritt 4 die Spalte „Ablauf". Ebenfalls
          // LEFT JOIN und aus demselben Grund: Eine Zeile ohne aufloesbaren Ablauf verschwindet
          // nicht, sie zeigt an dieser Stelle nichts.
          .leftJoin(SOS)
          .on(SOS.SOSID.eq(MESSAGE.SOSID))
          // Der aktuelle Schritt. Gejoint wird ueber BEIDE Spalten des zusammengesetzten
          // Primaerschluessels — ueber die Kennung allein waere es ein Kreuzprodukt: Die
          // SOSActionID 2 gibt es 694-mal (M13). Dass 43,9 Prozent der Verweise ins Leere laufen,
          // ist der dokumentierte Normalfall und kostet nichts; bei den offenen Status ist die
          // Verknuepfung lueckenlos.
          .leftJoin(SOSACTION)
          .on(SOSACTION.SOSID.eq(MESSAGE.SOSID))
          .and(SOSACTION.SOSACTIONID.eq(MESSAGE.SOSACTIONID))
          .where(bedingungen(mandant, abfrage))
          .orderBy(sortierung(abfrage))
          .limit(abfrage.limit() + 1)
          .fetch(
              satz ->
                  new NachrichtZeile(
                      satz.value1(),
                      satz.value2(),
                      satz.value3(),
                      satz.value4(),
                      satz.value5(),
                      satz.value6(),
                      satz.value7(),
                      satz.value8()));
    } catch (DataAccessException fehler) {
      throw anDerZeitgrenze(fehler, abfrage.suchtreffer() != null);
    }
  }

  /**
   * Der Abbruch an {@code max_statement_time} — <b>bei gesetztem Suchbegriff ein absehbarer Fall
   * und kein Systemfehler.</b>
   *
   * <p>Der Lese-Pool setzt {@code SET SESSION max_statement_time=10} ({@code docs/datenzugriff.md}
   * §1). Ohne Suchbegriff kommt kein Statement dieses Endpunkts auch nur in die Naehe (Messungen L1
   * bis L10, langsamster Fall 34 ms); mit Suchbegriff und langem Fenster schon — deshalb gibt es
   * die Fenstergrenze aus {@link NachrichtenFilter#SUCHE_FENSTER}. Sie macht den Abbruch
   * unwahrscheinlich, nicht unmoeglich: Unter Last oder in einem dichteren Bestand als dem
   * gemessenen kann derselbe Fall wieder auflaufen.
   *
   * <p><b>Gefangen wird genau eine Ausnahme, nicht pauschal alles.</b> Nachgeprueft gegen die
   * Testkopie am 06.08.2026: MariaDB meldet Fehler {@code 1969} mit SQLState {@code 70100} („Query
   * execution was interrupted"), der Treiber (Connector/J 3.5) macht daraus eine {@link
   * SQLTimeoutException}, und jOOQ verpackt sie in eine {@link DataAccessException} mit genau
   * dieser Ursache. Ein Syntaxfehler, eine abgerissene Verbindung oder ein fehlendes Recht kommen
   * ebenfalls als {@code DataAccessException} hier an und bleiben, was sie sind: technische Fehler
   * mit {@code 500}.
   *
   * <p><b>Und nur mit Suchbegriff.</b> Laeuft ein Statement <i>ohne</i> Suche in die Zeitgrenze,
   * ist das kein vorhergesehener Fall, sondern ein Befund — er gehoert mit Stacktrace ins Protokoll
   * und nicht in einen Hinweis, der dem Nutzer eine Verhaltensaenderung nahelegt, die nichts
   * aendern wuerde.
   *
   * <p>Der Text sagt, was zu tun ist, und nennt weder Tabelle noch Zeitgrenze noch Zeilenzahl.
   */
  static RuntimeException anDerZeitgrenze(DataAccessException fehler, boolean mitSuchbegriff) {
    if (!mitSuchbegriff || fehler.getCause(SQLTimeoutException.class) == null) {
      return fehler;
    }
    return new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        "suche-abgebrochen",
        "Suche abgebrochen",
        "Die Suche hat zu lange gedauert und wurde abgebrochen. Verkleinere den Zeitraum oder"
            + " schaerfe den Suchbegriff.",
        "Statement an der Zeitgrenze abgebrochen (Suchbegriff gesetzt)");
  }

  private List<Condition> bedingungen(MandantContext mandant, Nachrichtenabfrage abfrage) {
    List<Condition> bedingungen = new ArrayList<>();
    // Zuerst das Zeitfenster: Es ist der Zugriffspfad, alles andere ist Filter darauf.
    bedingungen.add(MESSAGE.MESSAGELASTUPDATE.ge(abfrage.fenster().von()));
    bedingungen.add(MESSAGE.MESSAGELASTUPDATE.le(abfrage.fenster().bis()));
    bedingungen.add(mandantenkette(mandant));

    if (!abfrage.status().isEmpty()) {
      bedingungen.add(statusClassifier.bedingung(abfrage.status(), MESSAGE.MESSAGESTATUS));
    }
    // Ausdruecklich gewaehlte Zwischenschritte schlagen die Vorgabe: Sonst filterte der Nutzer
    // ZWISCHENSCHRITT und bekaeme garantiert null Zeilen.
    if (!abfrage.zwischenschritte()
        && !abfrage.status().contains(MessageStatusKind.ZWISCHENSCHRITT)) {
      bedingungen.add(
          statusClassifier.ohne(MessageStatusKind.ZWISCHENSCHRITT, MESSAGE.MESSAGESTATUS));
    }
    if (!abfrage.prozessIds().isEmpty()) {
      bedingungen.add(MESSAGE.PROCESSID.in(abfrage.prozessIds()));
    }
    if (abfrage.suchtreffer() != null) {
      bedingungen.add(
          MESSAGE
              .PROCESSID
              .in(abfrage.suchtreffer().prozessIds())
              .or(MESSAGE.SOSID.in(abfrage.suchtreffer().sosIds())));
    }
    if (abfrage.cursor() != null) {
      bedingungen.add(cursorBedingung(abfrage.cursor(), abfrage.absteigend()));
    }
    return bedingungen;
  }

  /**
   * Die Mandantenkette {@code Message → Process → ProjectMandant} als {@code EXISTS}.
   *
   * <p>Als {@code EXISTS} und nicht als Join: {@code ProjectMandant} ist im Schema n:m, ein Join
   * koennte Zeilen vervielfachen, sobald ein Projekt mehreren Mandanten gehoert. In den Daten tut
   * er das heute nicht (Messung M3: alle 134 Projekte gehoeren genau einem Mandanten) — aber eine
   * Liste, deren Zeilenzahl an einer Stammdatenpflege haengt, ist die falsche Grundlage fuer eine
   * Sicherheitsgrenze.
   *
   * <p>Gemessen: Der Optimierer loest die Kette als zwei {@code eq_ref}-Zugriffe auf, je einer pro
   * Kandidatenzeile, und faellt damit nicht ins Gewicht — der Treiber bleibt das Zeitfenster.
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

  /**
   * Die Cursor-Bedingung in der <b>ODER-Form</b> statt als Tupelvergleich — gemessen, nicht
   * vermutet.
   *
   * <p>{@code MessageLastUpdateIDX} steht laut Schema nur auf {@code MessageLastUpdate}. InnoDB
   * haengt an jeden Sekundaerindex den Primaerschluessel, und MariaDB nutzt das ({@code
   * optimizer_switch: extended_keys=on}) — der Index ist damit faktisch {@code (MessageLastUpdate,
   * MessageID)} und genau der Sortierschluessel dieser Liste. Die ODER-Form uebersetzt MariaDB in
   * einen Bereich ueber beide Spalten ({@code key_len = 151}) und liest 52 Zeilen fuer eine Seite
   * von 50 (1,795 ms); der Tupelvergleich ergibt nur einen Bereich ueber den Zeitstempel ({@code
   * key_len = 5}), liest 245 Zeilen und braucht 2,551 ms. Zahlen in {@code
   * docs/messungen-schritt4.md}, Abschnitt L8.
   *
   * <p>Nebenbefund derselben Messung: Der Tiebreaker ueber {@code MessageID} loest <b>kein</b>
   * {@code filesort} aus — die Warnung in {@code docs/datenmodell.md} §3 zielte auf den
   * zusammengesetzten Index {@code MessageLastUpdateProcessMessageIDX}, der {@code ProcessID}
   * dazwischen hat. Gebraucht wird der hier gar nicht.
   */
  private static Condition cursorBedingung(Seitenposition position, boolean absteigend) {
    LocalDateTime zeitpunkt = position.zeitpunkt();
    return absteigend
        ? MESSAGE
            .MESSAGELASTUPDATE
            .lt(zeitpunkt)
            .or(MESSAGE.MESSAGELASTUPDATE.eq(zeitpunkt).and(MESSAGE.MESSAGEID.lt(position.id())))
        : MESSAGE
            .MESSAGELASTUPDATE
            .gt(zeitpunkt)
            .or(MESSAGE.MESSAGELASTUPDATE.eq(zeitpunkt).and(MESSAGE.MESSAGEID.gt(position.id())));
  }

  /**
   * Sortiert wird ausschliesslich ueber den Zeitpunkt; {@code MessageID} ist nur der Tiebreaker.
   */
  private static List<OrderField<?>> sortierung(Nachrichtenabfrage abfrage) {
    return abfrage.absteigend()
        ? List.of(MESSAGE.MESSAGELASTUPDATE.desc(), MESSAGE.MESSAGEID.desc())
        : List.of(MESSAGE.MESSAGELASTUPDATE.asc(), MESSAGE.MESSAGEID.asc());
  }

  /**
   * Loest einen Suchbegriff zu {@code ProcessID}s und {@code SOSID}s auf — <b>niemals</b> gegen
   * {@code Message} selbst.
   *
   * <p>Gesucht wird in den Stammdaten: {@code Process.ProcessName}, {@code Project.ProjectName} und
   * {@code SOS.SOSName}. Das sind 1.490, 140 und 1.818 Zeilen; ein voller Durchlauf ueber {@code
   * SOS} kostet 3,1 ms (Messung M5). Auf {@code SOSName} gibt es keinen Index, und bei dieser
   * Groesse braucht es auch keinen. Ein {@code LIKE} gegen {@code Message} dagegen waere ein
   * Durchlauf ueber 2,9 GB.
   *
   * <p>Auch die Vorfilterung traegt den Mandantenfilter — nicht aus Sicherheitsgruenden (den traegt
   * das Hauptstatement), sondern damit die Trefferzahl den Mandanten beschreibt und nicht den
   * gesamten Bestand.
   *
   * <p>{@code %} und {@code _} im Begriff werden maskiert. Ohne das waere {@code _} ein Platzhalter
   * fuer ein beliebiges Zeichen — derselbe Fallstrick wie in Regel Q1.
   *
   * <p><b>Die Vorfilterung ist billig, das Hauptstatement mit ihrem Ergebnis nicht</b> (Messungen
   * L7c, L11 und L13): Die {@code ODER}-Verknuepfung ueber {@code ProcessID} und {@code SOSID}
   * schliesst den Index auf {@code ProcessID} aus, MariaDB behaelt das Zeitfenster als Treiber. Was
   * das kostet, haengt am Begriff und nicht an der Fenstergroesse: Wer die Seite fuellt, zahlt
   * dasselbe ueber ein Jahr wie ueber 30 Tage (L13: 78.318 gelesene Zeilen, rund 0,5 s in beiden
   * Faellen). Wer sie nicht fuellt, zahlt das ganze Fenster — 1,2 s ueber 30 Tage, 15,6 s ueber ein
   * Jahr.
   *
   * <p><b>Beide vorgesehenen Umbauten sind gemessen und verworfen</b> (M10 und L11): Die Aufloesung
   * ueber {@code SOS.ProcessID} zu einer einzigen Kennungsliste verliert 9.101 Zeilen, und die
   * {@code UNION}-Fassung ist langsamer statt schneller, weil {@code Message.SOSID} keinen Index
   * hat und beide Zweige dasselbe Fenster lesen. Geblieben ist deshalb eine Grenze und kein Umbau:
   * {@link NachrichtenFilter#SUCHE_FENSTER}.
   */
  public Suchtreffer loeseSucheAuf(MandantContext mandant, String begriff, int hoechstens) {
    String muster = "%" + DSL.escape(begriff, '\\') + "%";
    List<String> prozessIds =
        glassfishDsl
            .selectDistinct(PROCESS.PROCESSID)
            .from(PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
            .leftJoin(PROJECT)
            .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
            .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
            .and(PROCESS.PROCESSNAME.like(muster, '\\').or(PROJECT.PROJECTNAME.like(muster, '\\')))
            .limit(hoechstens + 1)
            .fetch(PROCESS.PROCESSID);

    List<String> sosIds =
        glassfishDsl
            .selectDistinct(SOS.SOSID)
            .from(SOS)
            .join(PROCESS)
            .on(PROCESS.PROCESSID.eq(SOS.PROCESSID))
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
            .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
            .and(SOS.SOSNAME.like(muster, '\\'))
            .limit(hoechstens + 1)
            .fetch(SOS.SOSID);

    boolean zuUnscharf = prozessIds.size() > hoechstens || sosIds.size() > hoechstens;
    return new Suchtreffer(prozessIds, sosIds, zuUnscharf);
  }

  /**
   * Kommen beim Mandanten ueberhaupt Zwischenschritte vor? Eine Existenzfrage, kein {@code COUNT}.
   *
   * <p>Messung M12 ist der Anlass: <b>Fuenf von neun Mandanten mit Nachrichten haben ueber den
   * gesamten Bestand nicht eine einzige {@code SPLITTED}- oder {@code MERGED}-Zeile.</b> Fuer sie
   * kuendigt der Ausblenden-Schalter der Liste eine Wirkung an, die ausbleibt.
   *
   * <p><b>Ueber den Gesamtbestand und nicht ueber das Zeitfenster.</b> {@code VOTG} entscheidet die
   * Form: 40 Zeilen von 145.840 ueber den ganzen Bestand, im dichten Monat null. Ein Kriterium
   * ueber das gewaehlte Fenster schaltete den Schalter dort je nach Zeitraum an und aus — ein
   * Bedienelement, das erscheint und verschwindet, ohne dass ein Zusammenhang erkennbar waere.
   *
   * <p><b>{@code STRAIGHT_JOIN} ist hier kein Feinschliff, sondern die Bedingung, unter der das
   * Statement ueberhaupt tragbar ist</b> (Messung L15). Ohne die erzwungene Reihenfolge waehlt
   * MariaDB den Zugriffspfad <i>je Mandant verschieden</i> und faellt fuer manche auf einen vollen
   * Durchlauf ueber {@code Message} zurueck: gemessen 13,3 s fuer {@code IBIS} gegen 12 ms fuer
   * {@code WOC}, beide ohne einen einzigen Zwischenschritt. Der Grund ist die veraltete Statistik
   * der Quelle — {@code Message_ProcessFK} und {@code MessageStatusIDX} sind beide mit
   * Kardinalitaet 18 gefuehrt (M1), und darauf laesst sich keine Planwahl gruenden.
   *
   * <p>Mit {@code STRAIGHT_JOIN} laeuft die Kette in der einzigen Richtung, die selektiv ist:
   * {@code ProjectMandant} (Index auf {@code MandantID}) → {@code Process} → {@code Message} als
   * {@code ref} ueber {@code ProcessID}. <b>Die Kosten haengen damit am Bestand des Mandanten und
   * nicht an der Groesse der Tabelle</b> — die Eigenschaft, die das Statement in der Produktion
   * tragfaehig macht. Schlimmster gemessener Fall 331 ms ({@code IBIS}, 75.746 Nachrichten, keine
   * Zwischenschritte), {@code NEXANS} 0,8 ms.
   *
   * <p><b>Der schlimmste Fall bleibt der Mandant mit vielen Nachrichten und keinem
   * Zwischenschritt</b> — dort wird sein ganzer Bestand gelesen. Deshalb wird das Ergebnis
   * zwischengespeichert ({@link NachrichtenService}) und deshalb faellt ein Abbruch an der
   * Zeitgrenze dort auf „Schalter anzeigen" zurueck: der bisherige Zustand, also keine
   * Verschlechterung.
   */
  public boolean hatZwischenschritte(MandantContext mandant) {
    return glassfishDsl.fetchExists(
        glassfishDsl
            .selectOne()
            .from(PROJECTMANDANT)
            .straightJoin(PROCESS)
            .on(PROCESS.PROJECTID.eq(PROJECTMANDANT.PROJECTID))
            .straightJoin(MESSAGE)
            .on(MESSAGE.PROCESSID.eq(PROCESS.PROCESSID))
            .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
            .and(
                statusClassifier.bedingung(
                    MessageStatusKind.ZWISCHENSCHRITT, MESSAGE.MESSAGESTATUS)));
  }
}
