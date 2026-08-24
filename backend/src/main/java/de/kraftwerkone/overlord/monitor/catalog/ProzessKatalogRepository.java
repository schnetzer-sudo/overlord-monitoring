package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record10;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Der Datenzugriff des Prozess-Katalogs — <b>zwei {@code DSLContext}, und die Zuordnung ist nicht
 * beliebig.</b>
 *
 * <table border="1">
 *   <caption>Welcher Kontext wofuer</caption>
 *   <tr>
 *     <td><b>Lesen</b> ({@code Process} x {@code Project} x {@code ProjectMandant} x {@code
 *         process_catalog})</td>
 *     <td>schemauebergreifend, also zwingend ueber den <b>Lese-Pool</b> und eine einzige Verbindung.
 *         Schemanamen voll qualifiziert, kein {@code defaultSchema}</td>
 *   </tr>
 *   <tr>
 *     <td><b>Schreiben</b> in {@code process_catalog}</td>
 *     <td>ueber den <b>Schreib-Kontext</b>, ausschliesslich {@code overlord_monitor}</td>
 *   </tr>
 * </table>
 *
 * <p>Der Schreib-Kontext koennte den Join nicht: Sein Benutzer darf {@code GlassfishDB} zwar lesen,
 * aber die Trennung der Kontexte ist die Stelle, an der das Schreibverbot im Code sitzt — der
 * Lese-Kontext weist ueber seinen {@code ReadOnlyExecuteListener} jeden Nicht-Lesezugriff ab, auch
 * einen auf das eigene Schema.
 *
 * <h2>Der Mandant ist erster Pflichtparameter jeder Methode</h2>
 *
 * <p>Regel M2, und hier maschinell erzwungen: Sobald eine Klasse einen Typ aus {@code
 * jooq.glassfish} beruehrt, verlangt {@code PaketstrukturTest} den {@link MandantContext} als
 * ersten Parameter <b>jeder</b> oeffentlichen Methode — auch der rein schreibenden. Das ist hier
 * kein Formalismus: {@code process_catalog} traegt <b>keine</b> Mandantenspalte (E3, der Bezug
 * faellt aus dem Join ueber {@code ProjectMandant}), und eine Schreibmethode ohne Mandantennachweis
 * aenderte deshalb ohne Weiteres fremde Kuratierung.
 *
 * <p><b>Wo der Mandant im Statement steht und wo nicht.</b> Jede lesende Methode traegt den Filter
 * ueber {@code ProjectMandant} als Bestandteil des Statements (Regel M3) — wer eine fremde {@code
 * ProcessID} erraet, bekommt null Zeilen. Die schreibenden Methoden setzen ihn nicht noch einmal in
 * die Bedingung, sondern schreiben ausschliesslich auf Zeilen, die eine mandantengefilterte Lesung
 * ergeben hat; {@link #speichereFeld} holt sich diese Menge sogar selbst.
 */
@Repository
public class ProzessKatalogRepository {

  private final DSLContext glassfishDsl;
  private final DSLContext monitorDsl;

  ProzessKatalogRepository(
      @Qualifier("glassfishDsl") DSLContext glassfishDsl,
      @Qualifier("monitorDsl") DSLContext monitorDsl) {
    this.glassfishDsl = glassfishDsl;
    this.monitorDsl = monitorDsl;
  }

  /**
   * Die Pflegeliste des aktiven Mandanten.
   *
   * <p><b>{@code process_catalog} haengt als {@code LEFT JOIN} daran</b> und nicht umgekehrt: Die
   * Liste zeigt <b>alle</b> Prozesse des Mandanten (E5), auch die ohne Katalogzeile und auch die
   * ohne eine einzige Nachricht. Ein innerer Join zeigte nur, was schon kuratiert ist — also genau
   * die Zeilen, um die es nicht geht.
   *
   * <p><b>Sortiert nach {@code ProjectID}, dann {@code ProcessID}</b> (E6 in der eindeutigen
   * Fassung vom 20.08.2026). Beides sind Schluessel und damit stabil. {@code ProcessName} ist eine
   * prosaische Bezeichnung und ueber den Bestand <b>nicht eindeutig</b> — eine Liste ohne
   * Paginierung, die nach einem nicht eindeutigen Feld ohne Zweitschluessel sortiert, hat keine
   * feste Reihenfolge. Die Projektkennung kommt dabei aus {@code Process} und nicht aus dem {@code
   * LEFT JOIN} auf {@code Project}: Sie ist es, die auch in der Antwort steht, und sie ist nie
   * {@code null}.
   *
   * <p><b>Gemessen ist die andere Fassung.</b> L12 und M80-1 sind wortgleich mit {@code ORDER BY
   * pr.ProjectName, p.ProcessName} gefahren; die Entscheidung fiel danach. Der Sortierschritt geht
   * ueber dieselbe Zeilenmenge — {@code Using filesort} steht bereits im gemessenen {@code EXPLAIN}
   * —, nachgemessen ist er aber nicht.
   *
   * <p><b>Keine Paginierung</b> (E8) und kein Zeitfenster. Die Regeln L1 und L3 gelten fuer {@code
   * Message} mit seinen 3,3 Millionen Zeilen; hier stehen 1.503 Stammdatenzeilen ueber alle
   * Mandanten. Ein Zeitfenster blendete ausgerechnet die stillen Prozesse aus — und die sind der
   * Anlass der Kuratierung.
   *
   * @param nurOffene wenn {@code true}, bleiben gepflegte Zeilen weg. <b>Pflicht und nicht
   *     Bequemlichkeit</b> (E7): 733 Prozesse bei {@code NEXANS}, und eine Sortierung ersetzt
   *     keinen Arbeitsmodus. Prozesse ohne Katalogzeile gelten dabei als offen — deshalb steht das
   *     {@code IS NULL} in der Bedingung
   */
  public List<KatalogzeileResponse> findePflegeliste(MandantContext mandant, boolean nurOffene) {
    Condition bedingung = PROJECTMANDANT.MANDANTID.eq(mandant.mandantId());
    if (nurOffene) {
      bedingung =
          bedingung.and(
              PROCESS_CATALOG
                  .PFLEGESTATUS
                  .isNull()
                  .or(PROCESS_CATALOG.PFLEGESTATUS.ne(Pflegestatus.GEPFLEGT.name())));
    }
    return glassfishDsl
        .select(
            PROCESS.PROCESSID,
            PROCESS.PROJECTID,
            PROJECT.PROJECTNAME,
            PROCESS.PROCESSNAME,
            PROCESS_CATALOG.PARTNER,
            PROCESS_CATALOG.RICHTUNG,
            PROCESS_CATALOG.PFLEGESTATUS,
            PROCESS_CATALOG.VORSCHLAG_HERKUNFT,
            PROCESS_CATALOG.TRAEGT_NACHRICHTEN,
            PROCESS_CATALOG.BESTAND_GEPRUEFT_AM)
        .from(PROCESS)
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROJECT)
        .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROCESS_CATALOG)
        .on(PROCESS_CATALOG.PROCESS_ID.eq(PROCESS.PROCESSID))
        .where(bedingung)
        .orderBy(PROCESS.PROJECTID.asc(), PROCESS.PROCESSID.asc())
        .fetch(ProzessKatalogRepository::zeile);
  }

  /**
   * Eine einzelne Zeile der Pflegeliste — oder nichts, wenn der Prozess dem aktiven Mandanten nicht
   * gehoert <b>oder gar nicht existiert</b>.
   *
   * <p>Die beiden Faelle sind hier bewusst <b>nicht</b> unterscheidbar: Wer eine fremde {@code
   * ProcessID} erraet, bekommt dieselbe leere Antwort wie bei einer erfundenen. Daraus wird oben
   * {@code 404} und niemals {@code 403} — ein {@code 403} verriete, dass es den Prozess gibt.
   */
  public Optional<KatalogzeileResponse> findeZeile(MandantContext mandant, String processId) {
    return glassfishDsl
        .select(
            PROCESS.PROCESSID,
            PROCESS.PROJECTID,
            PROJECT.PROJECTNAME,
            PROCESS.PROCESSNAME,
            PROCESS_CATALOG.PARTNER,
            PROCESS_CATALOG.RICHTUNG,
            PROCESS_CATALOG.PFLEGESTATUS,
            PROCESS_CATALOG.VORSCHLAG_HERKUNFT,
            PROCESS_CATALOG.TRAEGT_NACHRICHTEN,
            PROCESS_CATALOG.BESTAND_GEPRUEFT_AM)
        .from(PROCESS)
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROJECT)
        .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROCESS_CATALOG)
        .on(PROCESS_CATALOG.PROCESS_ID.eq(PROCESS.PROCESSID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .and(PROCESS.PROCESSID.eq(processId))
        .fetchOptional(ProzessKatalogRepository::zeile);
  }

  /**
   * Die abgeleitete Partner-Auswahlliste des aktiven Mandanten.
   *
   * <p><b>Keine Tabelle {@code partner}</b> (E2): Ohne eigene Pflegeoberflaeche und ohne Attribute
   * ueber den Namen hinaus liefert eine Stammdatentabelle nichts, was diese Ableitung nicht auch
   * liefert — sie kostete eine Migration und eine Sicherungspflicht.
   *
   * <p><b>Der Mandantenbezug entsteht im Join</b> und nicht in einer Spalte (E3): {@code BAYER} bei
   * {@code VOTG} und {@code BAYER} bei {@code SUTTONS} sind zwei Werte — dieselbe Firma, zwei
   * EDI-Beziehungen.
   *
   * <p>Leere Werte bleiben weg. Das ist keine Unterschlagung: Ein leerer Partner ist ein gueltiger
   * <i>Pflegezustand</i> (E4), aber kein waehlbarer <i>Wert</i>.
   */
  public List<String> findePartner(MandantContext mandant) {
    return glassfishDsl
        .selectDistinct(PROCESS_CATALOG.PARTNER)
        .from(PROCESS_CATALOG)
        .join(PROCESS)
        .on(PROCESS.PROCESSID.eq(PROCESS_CATALOG.PROCESS_ID))
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .and(PROCESS_CATALOG.PARTNER.isNotNull())
        .and(PROCESS_CATALOG.PARTNER.ne(""))
        .orderBy(PROCESS_CATALOG.PARTNER.asc())
        .fetch(satz -> satz.value1());
  }

  /**
   * Ob das Projekt dem aktiven Mandanten gehoert.
   *
   * <p>Sie steht neben {@link #findeProjektbestand} und nicht darin, weil ein Projekt <b>ohne
   * Prozesse</b> sonst dieselbe leere Antwort gaebe wie ein fremdes — und das waere der Unterschied
   * zwischen „nichts zu tun" und {@code 404}.
   */
  public boolean projektGehoertZumMandanten(MandantContext mandant, String projectId) {
    return glassfishDsl.fetchExists(
        glassfishDsl
            .selectOne()
            .from(PROJECTMANDANT)
            .where(PROJECTMANDANT.PROJECTID.eq(projectId))
            .and(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())));
  }

  /**
   * <b>Das eine Statement, das beide Modi der Massenzuordnung teilen.</b>
   *
   * <p>Vorschau und Ausfuehrung rufen dieselbe Methode mit derselben Bedingung. Getrennt gebaut
   * driften sie auseinander, und der Nutzer bestaetigt dann eine Zahl, die nicht die ist, die
   * passiert (§5). {@link #speichereFeld} ruft sie deshalb selbst auf, statt eine Liste
   * entgegenzunehmen.
   *
   * <p>Der {@code LEFT JOIN} ist noetig, weil auch Prozesse <b>ohne</b> Katalogzeile betroffen sind
   * — fuer sie legt die Zuordnung eine an.
   */
  public List<Projektzeile> findeProjektbestand(MandantContext mandant, String projectId) {
    return glassfishDsl
        .select(PROCESS.PROCESSID, PROCESS_CATALOG.PFLEGESTATUS)
        .from(PROCESS)
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROCESS_CATALOG)
        .on(PROCESS_CATALOG.PROCESS_ID.eq(PROCESS.PROCESSID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .and(PROCESS.PROJECTID.eq(projectId))
        .fetch(satz -> new Projektzeile(satz.value1(), pflegestatus(satz.value2())));
  }

  /**
   * Der Bestand des aktiven Mandanten, wie ihn der Heuristik-Lauf braucht.
   *
   * <p><b>Warum sie nicht die Pflegeliste wiederverwendet:</b> Die Pflegeliste ebnet einen
   * Unterschied bewusst ein — ein Prozess ohne Katalogzeile erscheint dort als {@link
   * Pflegestatus#OFFEN}, weil er fuer den Nutzer genau das ist. Der Lauf muss die beiden Faelle
   * aber auseinanderhalten: „keine Zeile" heisst <i>anlegen</i>, „Zeile mit Status offen" heisst
   * <i>auffrischen</i>, und die Antwort nennt beide Zahlen getrennt. Deshalb steht der gespeicherte
   * Status hier roh, mit {@code null} fuer „keine Zeile".
   *
   * <p>Ohne Projektnamen und ohne Sortierung: Der Lauf zeigt niemandem etwas an, er rechnet.
   */
  public List<Bestandszeile> findeBestand(MandantContext mandant) {
    return glassfishDsl
        .select(PROCESS.PROCESSID, PROCESS.PROJECTID, PROCESS_CATALOG.PFLEGESTATUS)
        .from(PROCESS)
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROCESS_CATALOG)
        .on(PROCESS_CATALOG.PROCESS_ID.eq(PROCESS.PROCESSID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .fetch(
            satz ->
                new Bestandszeile(
                    satz.value1(),
                    satz.value2(),
                    satz.value3() == null ? null : Pflegestatus.valueOf(satz.value3())));
  }

  /**
   * <b>Der Bestandslauf, lesende Hälfte:</b> für jeden Prozess des aktiven Mandanten, ob im Bestand
   * mindestens eine Nachricht an ihm hängt (E14).
   *
   * <p><b>Das ist Fassung A aus M83‑1, und die Form ist gemessen und nicht frei gewählt.</b> Der
   * Einstieg steht über {@code ProjectMandant}, je Prozess steht ein {@code EXISTS}, und die
   * Unterabfrage vergleicht {@code Message.ProcessID} gegen {@code Process.ProcessID}. Gemessen:
   * <b>22,154 ms</b> für {@code NEXANS} (733 Zeilen, beste von fünf) und <b>1,109 ms</b> für {@code
   * SUTTONS} (17 Zeilen); der Plan trägt dreimal {@code Using index} und rührt die Datenseiten von
   * {@code Message} nie an.
   *
   * <p><b>Warum nicht die naheliegende Fassung B</b> ({@code SELECT DISTINCT m.ProcessID FROM
   * Message …}, Komplement im Dienst): Sie kostet <b>4.797 ms</b> statt 22 — <b>Faktor 216,5</b>.
   * Der Grund ist keine Feinheit des Plans, sondern die Bezugsgröße: Fassung A zahlt <b>je
   * Prozess</b>, Fassung B <b>je Nachricht</b>. Die Prozesszahl steht still, die Nachrichtenzahl
   * wächst — <b>Fassung A wird nicht teurer, Fassung B schon</b> (M83‑2).
   *
   * <p><b>Sie liefert {@code true} und {@code false}, nicht nur die lebenden Prozesse.</b> Der Lauf
   * schreibt beides; eine Fassung, die nur die lebenden nennt, zwänge den Dienst zum Komplement und
   * verlöre den Unterschied zwischen „geprüft und tot" und „nie geprüft".
   *
   * <p><b>Kein {@code STRAIGHT_JOIN}</b>, auch nicht als Reparatur, falls der Plan je kippt (M42).
   */
  public List<Bestandsflag> findeBestandsflags(MandantContext mandant) {
    Field<Boolean> traegt =
        DSL.field(
            DSL.exists(
                DSL.selectOne().from(MESSAGE).where(MESSAGE.PROCESSID.eq(PROCESS.PROCESSID))));
    return glassfishDsl
        .select(PROCESS.PROCESSID, traegt)
        .from(PROJECTMANDANT)
        .join(PROCESS)
        .on(PROCESS.PROJECTID.eq(PROJECTMANDANT.PROJECTID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .fetch(satz -> new Bestandsflag(satz.value1(), satz.value2()));
  }

  /**
   * <b>Der Bestandslauf, schreibende Hälfte — und er schont gepflegte Zeilen ausdrücklich nicht</b>
   * (E15).
   *
   * <p>Das ist der Unterschied zu {@link #speichereVorschlaege}, und er steht bewusst in einer
   * <b>eigenen Methode mit eigenem Namen</b> statt in einem Schalter: Wer den Code liest, muss
   * sehen, dass die eine Hälfte des Laufs {@link Pflegestatus#GEPFLEGT} verschont und die andere
   * über alles schreibt. Als Parameter wäre E15 nicht zu erkennen und würde beim nächsten Anfassen
   * „repariert".
   *
   * <p><b>Warum das E13 nicht verletzt:</b> E13 schützt <b>Kuratierung</b>, nicht
   * <b>Beobachtung</b>. Partner, Richtung, Pflegestatus und Herkunft hat ein Mensch entschieden;
   * {@code traegt_nachrichten} sagt, was die Datenbank sagt. Eine Beobachtung, die für gepflegte
   * Zeilen stehenbliebe, wäre nach dem ersten Lauf falsch — und gerade dort ist sie wertvoll:
   * „kuratiert <b>und</b> ohne Verkehr" ist die Aussage, auf die Schritt 10 aufsetzt.
   *
   * <p><b>Ein {@code UPDATE} und kein {@code INSERT … ON DUPLICATE KEY}.</b> Das ist keine
   * Geschmacksfrage: Ein {@code INSERT} müsste die {@code NOT NULL}-Spalten mitliefern — {@code
   * pflegestatus}, {@code vorschlag_herkunft}, {@code geaendert_am}, {@code geaendert_von} — und
   * überschriebe damit genau die Kuratierung, die E15 unangetastet lässt. Das {@code UPDATE} fasst
   * <b>ausschließlich die zwei Beobachtungsspalten</b> an.
   *
   * <p><b>{@code geaendert_am} und {@code geaendert_von} bleiben stehen.</b> Der Bestandslauf ist
   * keine Änderung an der Zeile im Sinne der Kuratierung; er trägt seinen eigenen Zeitstempel in
   * {@code bestand_geprueft_am}. Würde er {@code geaendert_am} mitziehen, sähe jede Zeile des
   * Mandanten nach jedem Knopfdruck frisch bearbeitet aus, und die Spalte verlöre ihre Aussage.
   *
   * <p>Zeilen ohne Katalogeintrag trifft das {@code UPDATE} nicht. Das ist folgenlos, weil der
   * Dienst den Bestandslauf <b>nach</b> {@link #speichereVorschlaege} fährt und danach jeder
   * Prozess des Mandanten eine Zeile hat — gezählt wird trotzdem, was wirklich geschrieben wurde.
   *
   * @return die Zahl der tatsächlich geschriebenen Zeilen
   */
  public int speichereBestandsflags(
      MandantContext mandant, List<Bestandsflag> flags, LocalDateTime jetztUtc) {
    if (flags.isEmpty()) {
      return 0;
    }
    BatchBindStep stapel =
        monitorDsl.batch(
            monitorDsl
                .update(PROCESS_CATALOG)
                // Wie bei speichereFeld: jede Zeile der Vorlage ist ein Bindeplatz, gebunden wird
                // unten in genau dieser Reihenfolge.
                .set(PROCESS_CATALOG.TRAEGT_NACHRICHTEN, (Boolean) null)
                .set(PROCESS_CATALOG.BESTAND_GEPRUEFT_AM, (LocalDateTime) null)
                .where(PROCESS_CATALOG.PROCESS_ID.eq((String) null)));
    for (Bestandsflag flag : flags) {
      stapel = stapel.bind(flag.traegtNachrichten(), jetztUtc, flag.processId());
    }
    int[] ergebnis = stapel.execute();
    int geschrieben = 0;
    for (int zeilen : ergebnis) {
      // MariaDB meldet 0 betroffene Zeilen, wenn der Wert sich nicht geaendert hat. Gezaehlt wird
      // deshalb jede Anweisung, die nicht fehlgeschlagen ist — sonst zaehlte der zweite Lauf null.
      if (zeilen >= 0) {
        geschrieben++;
      }
    }
    return geschrieben;
  }

  /**
   * Setzt Partner und Richtung einer einzelnen Zeile und macht sie <b>gepflegt</b>.
   *
   * <p>Ein {@code null}-Partner ist hier ein gueltiger Wert und keine Loeschung des Vorgangs: Er
   * bedeutet zusammen mit {@link Pflegestatus#GEPFLEGT} „hingesehen, es gibt nichts" (E4).
   *
   * <p><b>{@code vorschlag_herkunft} bleibt beim Aktualisieren stehen.</b> Sie beschreibt, welche
   * Regelfassung diese Zeile einmal vorgeschlagen hat — die Angabe wird durch eine Kuratierung
   * nicht falsch, sondern historisch. Nur beim <i>Anlegen</i> steht dort {@link
   * VorschlagHerkunft#KEINE}, denn dann hat keine Regel etwas beigetragen.
   */
  public void speichereZuordnung(
      MandantContext mandant,
      String processId,
      String partner,
      Richtung richtung,
      LocalDateTime jetztUtc,
      String benutzer) {
    monitorDsl
        .insertInto(PROCESS_CATALOG)
        .set(PROCESS_CATALOG.PROCESS_ID, processId)
        .set(PROCESS_CATALOG.PARTNER, partner)
        .set(PROCESS_CATALOG.RICHTUNG, richtung == null ? null : richtung.name())
        .set(PROCESS_CATALOG.PFLEGESTATUS, Pflegestatus.GEPFLEGT.name())
        .set(PROCESS_CATALOG.VORSCHLAG_HERKUNFT, VorschlagHerkunft.KEINE.name())
        .set(PROCESS_CATALOG.GEAENDERT_AM, jetztUtc)
        .set(PROCESS_CATALOG.GEAENDERT_VON, benutzer)
        .onDuplicateKeyUpdate()
        .set(PROCESS_CATALOG.PARTNER, DSL.excluded(PROCESS_CATALOG.PARTNER))
        .set(PROCESS_CATALOG.RICHTUNG, DSL.excluded(PROCESS_CATALOG.RICHTUNG))
        .set(PROCESS_CATALOG.PFLEGESTATUS, DSL.excluded(PROCESS_CATALOG.PFLEGESTATUS))
        .set(PROCESS_CATALOG.GEAENDERT_AM, DSL.excluded(PROCESS_CATALOG.GEAENDERT_AM))
        .set(PROCESS_CATALOG.GEAENDERT_VON, DSL.excluded(PROCESS_CATALOG.GEAENDERT_VON))
        .execute();
  }

  /**
   * Setzt <b>ein</b> Feld fuer alle Prozesse eines Projekts und macht die Zeilen gepflegt (E11).
   *
   * <p>Sie ermittelt die betroffene Menge ueber {@link #findeProjektbestand} — <b>dasselbe
   * Statement mit derselben Bedingung, das auch die Vorschau nennt</b> — und gibt sie zurueck,
   * damit die Antwort die Zahl nennen kann, die tatsaechlich geschrieben wurde.
   *
   * <p><b>Sie ueberschreibt gepflegte Zeilen</b> (E12). Der Schutzmodus „nur offene Zeilen" machte
   * genau die Korrektur unmoeglich, fuer die man sie braucht: einem ganzen Projekt einen falschen
   * Partner in einem Zug richtigzustellen.
   *
   * <p>Das jeweils <b>andere</b> Feld bleibt unangetastet — auch beim Anlegen einer fehlenden
   * Zeile, wo es {@code NULL} wird.
   */
  public List<Projektzeile> speichereFeld(
      MandantContext mandant,
      String projectId,
      Zuordnungsfeld feld,
      String wert,
      LocalDateTime jetztUtc,
      String benutzer) {
    List<Projektzeile> betroffen = findeProjektbestand(mandant, projectId);
    if (betroffen.isEmpty()) {
      return betroffen;
    }
    var gesetzt =
        feld == Zuordnungsfeld.PARTNER ? PROCESS_CATALOG.PARTNER : PROCESS_CATALOG.RICHTUNG;
    BatchBindStep stapel =
        monitorDsl.batch(
            monitorDsl
                .insertInto(PROCESS_CATALOG)
                // Jede Zeile der Vorlage ist ein Bindeplatz, auch die mit festem Wert. Sie stehen
                // deshalb alle als null-Platzhalter da und werden unten in genau dieser Reihenfolge
                // gebunden — sonst rutschen die Werte um eine Spalte.
                .set(PROCESS_CATALOG.PROCESS_ID, (String) null)
                .set(gesetzt, (String) null)
                .set(PROCESS_CATALOG.PFLEGESTATUS, (String) null)
                .set(PROCESS_CATALOG.VORSCHLAG_HERKUNFT, (String) null)
                .set(PROCESS_CATALOG.GEAENDERT_AM, (LocalDateTime) null)
                .set(PROCESS_CATALOG.GEAENDERT_VON, (String) null)
                .onDuplicateKeyUpdate()
                .set(gesetzt, DSL.excluded(gesetzt))
                .set(PROCESS_CATALOG.PFLEGESTATUS, DSL.excluded(PROCESS_CATALOG.PFLEGESTATUS))
                .set(PROCESS_CATALOG.GEAENDERT_AM, DSL.excluded(PROCESS_CATALOG.GEAENDERT_AM))
                .set(PROCESS_CATALOG.GEAENDERT_VON, DSL.excluded(PROCESS_CATALOG.GEAENDERT_VON)));
    for (Projektzeile zeile : betroffen) {
      stapel =
          stapel.bind(
              zeile.processId(),
              wert,
              Pflegestatus.GEPFLEGT.name(),
              VorschlagHerkunft.KEINE.name(),
              jetztUtc,
              benutzer);
    }
    stapel.execute();
    return betroffen;
  }

  /**
   * Schreibt die Vorschlaege eines Heuristik-Laufs — <b>immer mit Status {@link
   * Pflegestatus#OFFEN}</b>, denn die Heuristik schlaegt vor und entscheidet nicht.
   *
   * <p>Welche Zeilen hier ankommen, entscheidet der Service: gepflegte sind bereits aussortiert und
   * erreichen diese Methode nie (E13).
   *
   * <p><b>Zum {@link MandantContext} in der Signatur:</b> Er steht nicht in der Bedingung des
   * {@code INSERT}, weil {@code process_catalog} keine Mandantenspalte traegt (E3). Er ist der
   * maschinell erzwungene Nachweis, dass der Aufrufer die Zeilenmenge unter einem Mandanten
   * aufgeloest hat — genau die Zusicherung, die dieser Tabelle sonst fehlte.
   */
  public void speichereVorschlaege(
      MandantContext mandant,
      List<Vorschlagszeile> zeilen,
      LocalDateTime jetztUtc,
      String benutzer) {
    if (zeilen.isEmpty()) {
      return;
    }
    BatchBindStep stapel =
        monitorDsl.batch(
            monitorDsl
                .insertInto(PROCESS_CATALOG)
                // Wie bei speichereFeld: jede Zeile ein Bindeplatz, auch der feste Status.
                .set(PROCESS_CATALOG.PROCESS_ID, (String) null)
                .set(PROCESS_CATALOG.PARTNER, (String) null)
                .set(PROCESS_CATALOG.RICHTUNG, (String) null)
                .set(PROCESS_CATALOG.PFLEGESTATUS, (String) null)
                .set(PROCESS_CATALOG.VORSCHLAG_HERKUNFT, (String) null)
                .set(PROCESS_CATALOG.GEAENDERT_AM, (LocalDateTime) null)
                .set(PROCESS_CATALOG.GEAENDERT_VON, (String) null)
                .onDuplicateKeyUpdate()
                .set(PROCESS_CATALOG.PARTNER, DSL.excluded(PROCESS_CATALOG.PARTNER))
                .set(PROCESS_CATALOG.RICHTUNG, DSL.excluded(PROCESS_CATALOG.RICHTUNG))
                .set(PROCESS_CATALOG.PFLEGESTATUS, DSL.excluded(PROCESS_CATALOG.PFLEGESTATUS))
                .set(
                    PROCESS_CATALOG.VORSCHLAG_HERKUNFT,
                    DSL.excluded(PROCESS_CATALOG.VORSCHLAG_HERKUNFT))
                .set(PROCESS_CATALOG.GEAENDERT_AM, DSL.excluded(PROCESS_CATALOG.GEAENDERT_AM))
                .set(PROCESS_CATALOG.GEAENDERT_VON, DSL.excluded(PROCESS_CATALOG.GEAENDERT_VON)));
    for (Vorschlagszeile zeile : zeilen) {
      Partnervorschlag vorschlag = zeile.vorschlag();
      stapel =
          stapel.bind(
              zeile.processId(),
              vorschlag.partner(),
              vorschlag.richtung() == null ? null : vorschlag.richtung().name(),
              Pflegestatus.OFFEN.name(),
              vorschlag.herkunft().name(),
              jetztUtc,
              benutzer);
    }
    stapel.execute();
  }

  private static KatalogzeileResponse zeile(
      Record10<
              String,
              String,
              String,
              String,
              String,
              String,
              String,
              String,
              Boolean,
              LocalDateTime>
          satz) {
    return new KatalogzeileResponse(
        satz.value1(),
        satz.value2(),
        satz.value3(),
        satz.value4(),
        satz.value5(),
        richtung(satz.value6()),
        pflegestatus(satz.value7()),
        herkunft(satz.value8()),
        // Bewusst NICHT durch false ersetzt, anders als bei pflegestatus und herkunft darunter:
        // null heisst hier "noch nie geprueft" und ist ein eigener Zustand (E14). Ein Prozess ohne
        // Katalogzeile traegt ihn genauso wie eine Zeile, ueber die noch kein Bestandslauf ging.
        satz.value9(),
        satz.value10());
  }

  /**
   * Ein Prozess ohne Katalogzeile ist <b>offen</b> und nicht zustandslos (E4). Die Umsetzung des
   * {@code NULL} passiert hier und nicht in der Oberflaeche, damit kein Aufrufer die Frage anders
   * beantwortet.
   */
  private static Pflegestatus pflegestatus(String gespeichert) {
    return gespeichert == null ? Pflegestatus.OFFEN : Pflegestatus.valueOf(gespeichert);
  }

  private static VorschlagHerkunft herkunft(String gespeichert) {
    return gespeichert == null ? VorschlagHerkunft.KEINE : VorschlagHerkunft.valueOf(gespeichert);
  }

  private static Richtung richtung(String gespeichert) {
    return gespeichert == null ? null : Richtung.valueOf(gespeichert);
  }

  /**
   * Ein Prozess eines Projekts mit seinem Pflegezustand — die Zeile, auf der die Massenzuordnung
   * arbeitet.
   */
  public record Projektzeile(String processId, Pflegestatus pflegestatus) {}

  /**
   * Ein Prozess des Mandanten mit seinem <b>rohen</b> Katalogzustand.
   *
   * @param gespeicherterStatus {@code null}, wenn es zu diesem Prozess noch <b>keine</b> Zeile in
   *     {@code process_catalog} gibt. Das ist der Unterschied zwischen „anlegen" und „auffrischen"
   */
  public record Bestandszeile(
      String processId, String projectId, Pflegestatus gespeicherterStatus) {}

  /** Ein zu schreibender Vorschlag. */
  public record Vorschlagszeile(String processId, Partnervorschlag vorschlag) {}

  /**
   * Was der Bestandslauf über einen Prozess erhoben hat.
   *
   * @param traegtNachrichten <b>nie {@code null}</b>: Das Ergebnis eines {@code EXISTS} ist immer
   *     {@code true} oder {@code false}. Das {@code null} der Spalte entsteht nicht hier, sondern
   *     davor — es heißt „für diese Zeile hat nie ein Lauf stattgefunden" (E14)
   */
  public record Bestandsflag(String processId, boolean traegtNachrichten) {}
}
