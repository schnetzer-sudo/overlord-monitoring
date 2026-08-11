package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEACTION;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEPROPERTY;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SOSACTION;

import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die Statements des Nachrichtendetails — vier fuer die Detailansicht, eines fuer die
 * Eigenschaften.
 *
 * <p><b>Jedes einzelne traegt den Mandantenfilter</b> (Regel M3), und zwar als {@code EXISTS} ueber
 * {@code Process → ProjectMandant} in genau der Form, die die Nachrichtenliste verwendet — nicht
 * als nachgelagerte Pruefung und nicht ueber die View {@code MessageMandantID}, deren Zugriffspfad
 * mit den Rechten dieser Anwendung strukturell nicht einsehbar ist (Fehler 1142 und 1345).
 *
 * <p><b>Jedes Statement steigt ueber {@code Message} ein</b>, auch das, das am Ende Zeilen aus
 * {@code SOSAction} liefert. Das ist der Grund, warum keines von ihnen fuer sich genommen
 * mandantendurchlaessig sein kann: Es gibt keinen Weg in die Tabellen hinein, der nicht durch die
 * Nachricht und damit durch den Filter fuehrt.
 *
 * <p><b>Warum vier Statements und nicht eines.</b> Die Schrittfolge braucht die
 * <i>Ablaufdefinition</i>, die Namensaufloesung aber nicht nur den <i>einen</i> passenden Schritt,
 * sondern <b>alle</b> Schritte jedes beruehrten Ablaufs — sonst liesse sich Stufe 2 der Aufloesung
 * nicht auf Eindeutigkeit pruefen (M19). In einem Statement zusammengelegt waere das ein
 * Kreuzprodukt aus ausgefuehrten Aktionen und geplanten Schritten; getrennt ist jedes Statement
 * einzeln erklaerbar und einzeln messbar. Der Preis sind vier Zugriffe von zusammen deutlich unter
 * einer Millisekunde ({@code docs/nachrichtendetail.md}).
 *
 * <p><b>Kein Zeitfenster</b>, und das ist keine Nachlaessigkeit gegenueber Regel L1: Die gilt fuer
 * <i>Listen</i> ueber {@code Message}. Hier ist die Nachricht ueber ihren Primaerschluessel
 * festgelegt; ein Zeitfenster koennte nur noch etwas ausschliessen, was der Aufrufer bereits
 * benannt hat.
 */
@Repository
public class NachrichtendetailRepository {

  /**
   * Die harte Obergrenze eines einzelnen Eigenschaftswerts, in <b>Bytes</b> — an genau einer
   * Stelle.
   *
   * <p><b>Bemessungsgrundlage ist die groesste in M17 gemessene Laenge, nicht die Rechtfertigung,
   * es zu lassen.</b> Gemessen sind 2.124 Zeichen ueber den dichten Tag und 12.732 ueber den
   * dichten Monat; die Spalte ist {@code mediumtext} und laesst <b>16 MB je Zelle</b> zu. Die
   * Produktion ist nicht die Testkopie, und ein einzelner Wert soll die Antwort nicht sprengen
   * koennen. 16 KiB liegen ueber allem Gemessenen — der laengste bekannte Wert passt vollstaendig
   * hinein — und drei Groessenordnungen unter dem, was der Typ erlaubt.
   *
   * <p><b>Die Zahl wirkt zweimal, bleibt aber eine Zahl.</b> In der Abfrage begrenzt sie den Wert
   * auf so viele <i>Zeichen</i>; das ist eine sichere Ueberholung, weil ein UTF-8-Zeichen nie
   * weniger als ein Byte belegt — was in die Byte-Grenze passt, ist damit garantiert noch enthalten
   * und die Uebertragung trotzdem gedeckelt. Die genaue Kappung auf Bytes geschieht danach im
   * Backend, auf einer Zeichengrenze.
   */
  public static final int WERT_GRENZE_BYTES = 16 * 1024;

  /**
   * Der Alias fuer die Mandantenkette. Er muss ein anderer sein als der der aeusseren Abfrage —
   * dort haengt {@code Process} schon fuer den Anzeigenamen.
   *
   * <p>Der Typ {@code Process} ist die generierte Tabelle des Quellschemas; der Import verdeckt in
   * dieser Datei {@code java.lang.Process}.
   */
  private static final Process MANDANTEN_PROCESS = PROCESS.as("mandanten_process");

  private final DSLContext glassfishDsl;

  NachrichtendetailRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Der Kopf — oder {@code null}, wenn es die Nachricht nicht gibt <b>oder</b> sie einem fremden
   * Mandanten gehoert. Beides ist von hier aus ununterscheidbar, und genau das ist der Zweck: Der
   * Aufrufer bekommt in beiden Faellen dieselbe {@code 404}-Antwort (Regel M3).
   *
   * <p>Drei {@code LEFT JOIN}s fuer Anzeigenamen und einer fuer den naechsten Schritt. Alle vier
   * sind {@code LEFT}, weil ein fehlender Anzeigename nicht darueber entscheiden darf, ob die
   * Nachricht ueberhaupt erscheint — sonst verschwaende eine Nachricht, weil ihr Ablauf umbenannt
   * wurde.
   *
   * <p><b>Der naechste Schritt wird immer mitgelesen und nur manchmal ausgeliefert.</b> Der Join
   * ist ein {@code eq_ref} ueber den zusammengesetzten Primaerschluessel von {@code SOSAction} und
   * kostet nichts; ob er in der Antwort erscheint, entscheidet der offene Zustand im Service.
   * Dieselbe Aufteilung wie beim aktuellen Schritt der Liste.
   *
   * <p><b>{@code Message.SOSID} und {@code Message.SOSActionID} kommen roh dazu</b> (10.08.2026).
   * Sie sind die beiden Spalten, ueber die der Service {@link OffenerZustand#WARTET_IN} von {@link
   * OffenerZustand#WARTET_VOR} unterscheidet. Sie kosten nichts — der Join darauf steht ohnehin —,
   * und sie hier zu lesen ist der Grund, warum die Oberflaeche keine Kennungen mehr vergleicht.
   *
   * <p><b>Die Anzahl der Eigenschaften kommt als Unterabfrage</b> und nicht aus einem fuenften
   * Zugriff. Sie zaehlt ueber das Praefix des Primaerschluessels von {@code MessageProperty} und
   * liest dabei <b>keinen einzigen Wert</b> — {@code Using index}. Ein {@code COUNT} ueber {@code
   * MessageProperty} <i>fuer eine Nachricht</i> ist nicht die Live-Aggregation, die Regel L2
   * ausschliesst; die zielt auf Kennzahlen ueber {@code Message}.
   *
   * <p><b>Die vier Verkettungsspalten kommen roh dazu</b> (11.08.2026, Schritt 6 Teil 2b). Aus
   * ihnen entstehen die Rollen der Nachricht — <b>in {@code common/Kettenrollen} und nicht
   * hier</b>. Sie <b>kosten keinen Join und kein zweites Statement</b>: Alle vier stehen auf der
   * {@code Message}-Zeile, die dieses Statement ohnehin liest. Genau das ist die Auskunft von E4 —
   * ob eine Nachricht eine Kette hat, steht auf der Zeile, ohne Abfrage. Der Zugriffspfad aus §8
   * aendert sich damit nicht; {@code Message} bleibt {@code const}.
   */
  public NachrichtKopfZeile findeKopf(MandantContext mandant, String messageId) {
    Field<Integer> eigenschaftenAnzahl =
        DSL.selectCount()
            .from(MESSAGEPROPERTY)
            .where(MESSAGEPROPERTY.MESSAGEID.eq(MESSAGE.MESSAGEID))
            .asField("eigenschaften_anzahl");

    return glassfishDsl
        .select(
            MESSAGE.MESSAGEID,
            MESSAGE.MESSAGESTATUS,
            MESSAGE.MESSAGELASTUPDATE,
            MESSAGE.MESSAGETIMEOUT,
            MESSAGE.PROCESSID,
            PROCESS.PROCESSNAME,
            PROJECT.PROJECTNAME,
            SOS.SOSNAME,
            MESSAGE.SOSID,
            MESSAGE.SOSACTIONID,
            SOSACTION.SOSACTIONNAME,
            eigenschaftenAnzahl,
            MESSAGE.SOURCE,
            MESSAGE.SOURCEMESSAGEID,
            MESSAGE.TARGETMESSAGEID,
            MESSAGE.TARGET)
        .from(MESSAGE)
        .leftJoin(PROCESS)
        .on(PROCESS.PROCESSID.eq(MESSAGE.PROCESSID))
        .leftJoin(PROJECT)
        .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(SOS)
        .on(SOS.SOSID.eq(MESSAGE.SOSID))
        .leftJoin(SOSACTION)
        .on(SOSACTION.SOSID.eq(MESSAGE.SOSID))
        .and(SOSACTION.SOSACTIONID.eq(MESSAGE.SOSACTIONID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(mandantenkette(mandant))
        .fetchOne(
            satz ->
                new NachrichtKopfZeile(
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
                    satz.value12(),
                    satz.value13(),
                    satz.value14(),
                    satz.value15(),
                    satz.value16()));
  }

  /**
   * Gibt es die Nachricht fuer diesen Mandanten? Die Existenzfrage fuer den Eigenschaften-Endpunkt.
   *
   * <p><b>Ohne sie waere die Antwort auf eine fremde Nachricht eine leere Liste statt {@code
   * 404}</b> — und „leer" und „gibt es nicht" waeren zwei verschiedene Auskuenfte, aus denen sich
   * der Bestand abfragen liesse. Eine Nachricht ohne jede Eigenschaft ist ausserdem moeglich: In
   * der Testkopie hat zwar jede der 6.249 Nachrichten des Tagesfensters welche (M17 1), aber die
   * Tabelle erzwingt es nicht.
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
   * Alle Aktionen der Nachricht — <b>einschliesslich</b> des Metadaten-Schritts.
   *
   * <p>Er wird hier mitgelesen und erst beim Bau der Antwort ausgenommen, weil er zwei Dinge
   * beitraegt, die sonst fehlten: den <b>fachlichen Start</b> ({@code MIN(MessageActionStart)}
   * ueber alle Aktionen, Regel Q2 — an ihm kommt die Nachricht ins System) und die Antwort auf
   * „gibt es ueberhaupt eine Aktion" fuer {@link OffenerZustand}. Wer ihn schon in der Abfrage
   * wegliesse, muesste den Start ein zweites Mal erfragen.
   *
   * <p>Sortiert nach {@code MessageActionStart}, bei Gleichstand nach {@code MessageActionID}.
   * MariaDB stellt {@code NULL} dabei nach vorn; einen Start ohne Wert gibt es im Gesamtbestand
   * nicht (M22, {@code 0} von 10.308.590), die Spalte laesst ihn aber zu.
   */
  public List<MessageAktion> findeAktionen(MandantContext mandant, String messageId) {
    return glassfishDsl
        .select(
            MESSAGEACTION.MESSAGEACTIONID,
            MESSAGEACTION.SOSID,
            MESSAGEACTION.SOSACTIONID,
            MESSAGEACTION.MESSAGEACTIONSTART,
            MESSAGEACTION.MESSAGEACTIONEND,
            MESSAGEACTION.SOSACTIONSERVICEPROPERTIES,
            MESSAGEACTION.SOSACTIONTIMEOUT)
        .from(MESSAGE)
        .join(MESSAGEACTION)
        .on(MESSAGEACTION.MESSAGEID.eq(MESSAGE.MESSAGEID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(mandantenkette(mandant))
        .orderBy(MESSAGEACTION.MESSAGEACTIONSTART.asc(), MESSAGEACTION.MESSAGEACTIONID.asc())
        .fetch(
            satz ->
                new MessageAktion(
                    satz.value1(),
                    satz.value2(),
                    satz.value3(),
                    satz.value4(),
                    satz.value5(),
                    satz.value6(),
                    satz.value7()));
  }

  /**
   * <b>Alle</b> geplanten Schritte <b>jedes</b> Ablaufs, den die Nachricht beruehrt hat.
   *
   * <p>Gejoint wird ueber {@code sa.SOSID = ma.SOSID} — nur die erste Haelfte des zusammengesetzten
   * Schluessels. Das ist Absicht: Die Namensaufloesung braucht nicht den einen passenden Schritt,
   * sondern die ganze Ablaufdefinition, weil sonst Stufe 2 („genau ein Schritt mit derselben ersten
   * Marke") nicht pruefbar waere.
   *
   * <p><b>Ueber {@code MessageAction.SOSID} und niemals ueber {@code Message.SOSID}</b> (M15). Und
   * <b>nicht ueber einen angenommenen einzigen Ablauf</b>: 2,51 Prozent der Nachrichten haben
   * Schritte aus mehr als einem, bis zu drei (M20). Das {@code DISTINCT} faengt genau diese Faelle
   * ab — mehrere Aktionen desselben Ablaufs wuerden dieselben Definitionszeilen sonst mehrfach
   * liefern.
   *
   * <p>Die Menge ist von Natur aus klein: Ein Ablauf hat ein bis sechs Schritte (M13), eine
   * Nachricht beruehrt ein bis drei Ablaeufe.
   */
  public List<Ablaufschritt> findeAblaufschritte(MandantContext mandant, String messageId) {
    return glassfishDsl
        .selectDistinct(
            SOSACTION.SOSID,
            SOSACTION.SOSACTIONID,
            SOSACTION.SOSACTIONNAME,
            SOSACTION.SOSACTIONSERVICEPROPERTIES)
        .from(MESSAGE)
        .join(MESSAGEACTION)
        .on(MESSAGEACTION.MESSAGEID.eq(MESSAGE.MESSAGEID))
        .join(SOSACTION)
        .on(SOSACTION.SOSID.eq(MESSAGEACTION.SOSID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(mandantenkette(mandant))
        .fetch(
            satz -> new Ablaufschritt(satz.value1(), satz.value2(), satz.value3(), satz.value4()));
  }

  /**
   * Die kuratierten Eigenschaften — <b>ausschliesslich ueber die {@code MessageID}</b> (Regel L4).
   *
   * <p>Die zusaetzliche Bedingung auf {@code MessagePropertyName} ist <b>kein Verstoss gegen
   * L4</b>: Verboten ist das Filtern, Gruppieren und Sortieren ueber den <i>Wert</i>, dessen
   * Indizes Praefix-Indizes ueber 50 Zeichen sind (M14, gleich zweimal). Der <i>Name</i> ist
   * dagegen die zweite Spalte des Primaerschluessels {@code (MessageID, MessagePropertyName,
   * MessageActionID)} — Kennung plus Name ist genau der Bereich, den dieser Schluessel abbildet.
   *
   * <p>Es wird bewusst <b>nicht</b> vorausgesetzt, dass ein Name je Nachricht nur einmal vorkommt.
   * Der Primaerschluessel erlaubt denselben Namen auf mehreren Schritten, und {@code
   * Converter.Payload.GUID} nutzt das (7.862 Zeilen auf 6.149 Nachrichten, M17 3). Fuer die beiden
   * kuratierten Namen ist im Tagesfenster je genau ein Schritt gemessen — die Abfrage liefert
   * trotzdem alle Vorkommen und die Antwort zeigt sie.
   */
  public List<MessageEigenschaft> findeKuratierteEigenschaften(
      MandantContext mandant, String messageId) {
    return glassfishDsl
        .select(
            MESSAGEPROPERTY.MESSAGEPROPERTYNAME,
            MESSAGEPROPERTY.MESSAGEPROPERTYVALUE,
            MESSAGEPROPERTY.MESSAGEACTIONID,
            DSL.octetLength(MESSAGEPROPERTY.MESSAGEPROPERTYVALUE))
        .from(MESSAGE)
        .join(MESSAGEPROPERTY)
        .on(MESSAGEPROPERTY.MESSAGEID.eq(MESSAGE.MESSAGEID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(MESSAGEPROPERTY.MESSAGEPROPERTYNAME.in(KuratierteEigenschaften.namen()))
        .and(mandantenkette(mandant))
        .orderBy(MESSAGEPROPERTY.MESSAGEPROPERTYNAME.asc(), MESSAGEPROPERTY.MESSAGEACTIONID.asc())
        .fetch(
            satz ->
                new MessageEigenschaft(
                    satz.value1(),
                    satz.value2(),
                    satz.value3(),
                    satz.value4() == null ? 0 : satz.value4()));
  }

  /**
   * <b>Alle</b> Eigenschaften der Nachricht, ueber die {@code MessageID} und sonst nichts (Regel
   * L4).
   *
   * <p><b>Der Wert wird schon in der Abfrage begrenzt</b>, nicht erst nach der Uebertragung: Ein
   * {@code mediumtext} laesst 16 MB je Zelle zu, und was der Treiber einmal geholt hat, hat die
   * Leitung schon belastet. {@link #WERT_GRENZE_BYTES} wirkt hier als <i>Zeichen</i>-Grenze — eine
   * sichere Ueberholung, weil ein UTF-8-Zeichen nie weniger als ein Byte belegt. Danebengestellt
   * wird die <b>ungekappte</b> Laenge in Bytes; nur so ist ablesbar, dass gekappt wurde und wie
   * viel fehlt.
   *
   * <p>Sortiert nach Name und dann Schritt — also in der Reihenfolge des Primaerschluessels, ohne
   * Sortierlauf. Die Zuordnung zum Schritt kommt als Spalte mit; <b>gefiltert wird nichts
   * danach.</b> Ob die Oberflaeche die von M17 (3) gemessene Familiengrenze nutzt — {@code
   * Message.*} auf dem Metadaten-Schritt, dienstbezogene am jeweiligen Schritt —, entscheidet Teil
   * 2.
   */
  public List<MessageEigenschaft> findeEigenschaften(MandantContext mandant, String messageId) {
    Field<String> begrenzterWert =
        DSL.left(MESSAGEPROPERTY.MESSAGEPROPERTYVALUE, WERT_GRENZE_BYTES);
    Field<Integer> laengeInBytes = DSL.octetLength(MESSAGEPROPERTY.MESSAGEPROPERTYVALUE);

    return glassfishDsl
        .select(
            MESSAGEPROPERTY.MESSAGEPROPERTYNAME,
            begrenzterWert,
            MESSAGEPROPERTY.MESSAGEACTIONID,
            laengeInBytes)
        .from(MESSAGE)
        .join(MESSAGEPROPERTY)
        .on(MESSAGEPROPERTY.MESSAGEID.eq(MESSAGE.MESSAGEID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(mandantenkette(mandant))
        .orderBy(MESSAGEPROPERTY.MESSAGEPROPERTYNAME.asc(), MESSAGEPROPERTY.MESSAGEACTIONID.asc())
        .fetch(
            satz ->
                new MessageEigenschaft(
                    satz.value1(),
                    satz.value2(),
                    satz.value3(),
                    satz.value4() == null ? 0 : satz.value4()));
  }

  /**
   * Die Mandantenkette {@code Message → Process → ProjectMandant} als {@code EXISTS} — Wort fuer
   * Wort dieselbe wie in {@code NachrichtenRepository}.
   *
   * <p>Als {@code EXISTS} und nicht als Join: {@code ProjectMandant} ist im Schema n:m, ein Join
   * koennte Zeilen vervielfachen, sobald ein Projekt mehreren Mandanten gehoert. Heute tut er das
   * nicht (M3: alle 134 Projekte gehoeren genau einem Mandanten) — aber eine Antwort, deren
   * Zeilenzahl an einer Stammdatenpflege haengt, ist die falsche Grundlage fuer eine
   * Sicherheitsgrenze. Beim Detail waere die Folge sogar sichtbarer als bei der Liste: Jeder
   * Schritt erschiene doppelt.
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
