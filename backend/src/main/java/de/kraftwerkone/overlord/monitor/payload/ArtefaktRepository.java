package de.kraftwerkone.overlord.monitor.payload;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEPROPERTY;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SERVICE;

import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Process;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die drei Statements des Rohdatenzugriffs: Artefaktliste, Existenznachweis und Aufloesung einer
 * Ablagenkennung.
 *
 * <h2>Mandantenpruefung im Statement, nicht nachgelagert</h2>
 *
 * <p>Jedes der drei traegt den Mandantenfilter als {@code EXISTS} ueber {@code Process →
 * ProjectMandant} — Wort fuer Wort dieselbe Kette wie in {@code NachrichtenRepository} und {@code
 * NachrichtendetailRepository} (Regel M3). Als {@code EXISTS} und nicht als Join, weil {@code
 * ProjectMandant} im Schema n:m ist: Ein Join koennte Zeilen vervielfachen, sobald ein Projekt
 * mehreren Mandanten gehoert. Hier waere die Folge besonders unangenehm — jedes Artefakt erschiene
 * doppelt in der Liste.
 *
 * <p><b>Auch die Aufloesung der Ablagenkennung traegt die Kette</b>, obwohl {@code Service} keinen
 * Mandanten kennt. Ohne sie waere die Methode eine Auskunftsstelle fuer Verbindungszeichenketten,
 * die nur deshalb sicher ist, weil der Aufrufer vorher das Richtige getan hat. Mit ihr steht im
 * Statement selbst, was gelten soll: <b>Eine Ablagenadresse gibt es nur zu einer Nachricht, die der
 * Mandant sehen darf.</b> Der Preis ist ein zusaetzlicher Primaerschluesselzugriff je Abruf.
 *
 * <h2>Regel L4</h2>
 *
 * <p>Der Einstieg laeuft ausschliesslich ueber die {@code MessageID}. Die zusaetzliche Bedingung
 * auf {@code MessagePropertyName} ist <b>kein Verstoss</b>: Verboten ist das Filtern, Gruppieren
 * und Sortieren ueber den <i>Wert</i>, dessen Indizes Praefix-Indizes ueber 50 Zeichen sind (M14).
 * Der <i>Name</i> ist die zweite Spalte des Primaerschluessels {@code (MessageID,
 * MessagePropertyName, MessageActionID)}.
 *
 * <p><b>Der Wert wird nie zerlegt, gejoint oder gefiltert.</b> Die Aufloesung der Ablage ist ein
 * <i>zweites</i> Statement mit dem Ergebnis des ersten als Parameter — kein Join ueber {@code
 * SUBSTRING_INDEX(MessagePropertyValue, '|', 1)}. Ein solcher Join waere genau das Filtern ueber
 * den Wert, das L4 verbietet, und die Trennung ist ohnehin die gemessene Form (M58 (1) und (2)).
 *
 * <h2>Kein Zeitfenster</h2>
 *
 * <p>Regel L1 gilt fuer <i>Listen</i> ueber {@code Message}. Hier ist die Nachricht ueber ihren
 * Primaerschluessel benannt; ein Zeitfenster koennte nur noch ausschliessen, was der Aufrufer
 * bereits genannt hat. Dieselbe Begruendung wie im Nachrichtendetail.
 *
 * <p>Messungen nach Regel L7 stehen in {@code docs/rohdaten-backend.md}.
 */
@Repository
public class ArtefaktRepository {

  /**
   * Der Alias fuer die Mandantenkette. Er muss ein anderer sein als der einer aeusseren Abfrage;
   * hier haengt zwar keine, aber die Form bleibt die der Nachbarrepositories.
   *
   * <p>Der Typ {@code Process} ist die generierte Tabelle des Quellschemas; der Import verdeckt in
   * dieser Datei {@code java.lang.Process}.
   */
  private static final Process MANDANTEN_PROCESS = PROCESS.as("mandanten_process");

  private final DSLContext glassfishDsl;

  ArtefaktRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Alle Artefakte einer Nachricht — die beiden gemessenen Namensmuster, sonst nichts.
   *
   * <p>Eine leere Liste bedeutet <b>nicht</b> „gibt es nicht": Sie kommt auch fuer eine fremde oder
   * erfundene {@code MessageID} zurueck. Die Unterscheidung trifft {@link #existiert}, und der
   * Service macht aus beidem dieselbe {@code 404}.
   *
   * <p>Sortiert nach Schritt und dann Name. Nach Schritt zuerst, weil die Oberflaeche die Artefakte
   * <i>je Schritt</i> gruppiert; der Primaerschluessel fuehrt die andere Reihenfolge, der
   * Sortierlauf ueber drei bis fuenfzehn Zeilen (M55) kostet nichts.
   */
  public List<Artefaktzeile> findeArtefakte(MandantContext mandant, String messageId) {
    return glassfishDsl
        .select(
            MESSAGEPROPERTY.MESSAGEPROPERTYNAME,
            MESSAGEPROPERTY.MESSAGEACTIONID,
            MESSAGEPROPERTY.MESSAGEPROPERTYVALUE)
        .from(MESSAGE)
        .join(MESSAGEPROPERTY)
        .on(MESSAGEPROPERTY.MESSAGEID.eq(MESSAGE.MESSAGEID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(artefaktnamen())
        .and(mandantenkette(mandant))
        .orderBy(MESSAGEPROPERTY.MESSAGEACTIONID.asc(), MESSAGEPROPERTY.MESSAGEPROPERTYNAME.asc())
        .fetch(
            satz ->
                new Artefaktzeile(
                    satz.value1(), satz.value2() == null ? 0 : satz.value2(), satz.value3()));
  }

  /**
   * Ob die Nachricht fuer diesen Mandanten sichtbar ist.
   *
   * <p>Gebraucht, damit „keine Artefakte" und „gibt es nicht" nicht zu derselben Antwort werden.
   * Ohne diesen Nachweis waere die Antwort auf eine fremde Nachricht eine leere Liste statt {@code
   * 404} — und „leer" und „gibt es nicht" waeren zwei verschiedene Auskuenfte, aus denen sich der
   * Bestand abfragen liesse. Dass eine Nachricht ohne Artefakte im gemessenen Bestand nicht
   * vorkommt (M55: Minimum 3), macht die Zeile nicht ueberfluessig — die Tabelle erzwingt es nicht.
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
   * Die Verbindungszeichenkette einer Ablage — <b>nur zu einer Nachricht, die der Mandant sehen
   * darf</b>.
   *
   * <p>Die Kennung ist die {@code Service.ServiceID}, der Zugriff geht ueber den Primaerschluessel
   * (M52). Der Wert wird <b>unveraendert und ohne Anhaengsel</b> weitergereicht (Q1): Der {@code
   * ServiceConnectString} beider gemessener Ablagen endet auf {@code
   * /WebApplication/FileStoreSoapReceiver} und ist bereits die vollstaendige Adresse des
   * SOAP-Empfaengers.
   *
   * <p><b>Keine Aufloesung heisst {@link Artefaktzustand#ABLAGE_NICHT_ERREICHBAR}</b>, nicht „nimm
   * eine andere". Ein Rueckfall waere falsch, auch wenn er gelegentlich funktionierte: 20 von 20
   * Kreuzabrufen scheitern, dieselben Verweise gelingen am eigenen Knoten (M68).
   *
   * @return leer, wenn es die Kennung nicht gibt <b>oder</b> die Nachricht fuer diesen Mandanten
   *     nicht sichtbar ist. Die beiden Faelle sind von aussen ununterscheidbar, und das ist Absicht
   */
  public Optional<String> findeVerbindung(
      MandantContext mandant, String messageId, String ablagenkennung) {
    return glassfishDsl
        .select(SERVICE.SERVICECONNECTSTRING)
        .from(SERVICE)
        .where(SERVICE.SERVICEID.eq(ablagenkennung))
        .andExists(
            DSL.selectOne()
                .from(MESSAGE)
                .where(MESSAGE.MESSAGEID.eq(messageId))
                .and(mandantenkette(mandant)))
        .fetchOptional(SERVICE.SERVICECONNECTSTRING)
        .filter(adresse -> !adresse.isBlank());
  }

  /**
   * Der Originaldateiname <b>auf einem Schritt</b> — {@code %.FileProperty.OriginalFilename}.
   *
   * <p><b>Nur auf dem Download-Pfad.</b> Die Anzeige braucht ihn nicht, und ein Statement, das bei
   * jedem Listenaufruf mitlaeuft, ohne dass es jemand liest, ist ein Statement zu viel.
   *
   * <p>Wieder ueber die {@code MessageID} und den Namen, nie ueber den Wert (Regel L4) — und mit
   * derselben Mandantenkette wie die Artefaktliste. Vorhanden ist der Name fuer rund 69,6 % der
   * Nachrichten (M17); fehlt er, konstruiert {@link Downloaddateiname} einen.
   *
   * <p><b>Gesucht wird ueber das Muster, nicht ueber die Familie des Artefakts</b> — die
   * Begruendung steht bei {@link Downloaddateiname#likeMuster()}. Kurz: Den Namen tragen nur die
   * Lesedienste, und der Eingang heisst {@code Message.Payload.GUID}; ueber seine Familie gesucht
   * bliebe er ohne Namen.
   *
   * @return leer, wenn es den Namen nicht gibt, er leer ist <b>oder</b> die Nachricht fuer diesen
   *     Mandanten nicht sichtbar ist
   */
  public Optional<String> findeOriginaldateiname(
      MandantContext mandant, String messageId, short schritt) {
    return glassfishDsl
        .select(MESSAGEPROPERTY.MESSAGEPROPERTYVALUE)
        .from(MESSAGE)
        .join(MESSAGEPROPERTY)
        .on(MESSAGEPROPERTY.MESSAGEID.eq(MESSAGE.MESSAGEID))
        .where(MESSAGE.MESSAGEID.eq(messageId))
        .and(MESSAGEPROPERTY.MESSAGEPROPERTYNAME.like(Downloaddateiname.likeMuster()))
        .and(MESSAGEPROPERTY.MESSAGEACTIONID.eq(schritt))
        .and(mandantenkette(mandant))
        // Nach Namen sortiert, damit die Auswahl bei mehreren Lesediensten auf einem Schritt
        // festliegt und nicht davon abhaengt, in welcher Reihenfolge MariaDB liefert. Gemessen
        // kommt dieser Fall nicht vor: FileReader und FTPReader schliessen einander aus (M56 a).
        .orderBy(MESSAGEPROPERTY.MESSAGEPROPERTYNAME.asc())
        .limit(1)
        .fetchOptional(MESSAGEPROPERTY.MESSAGEPROPERTYVALUE)
        .map(String::trim)
        .filter(wert -> !wert.isEmpty());
  }

  /**
   * Die beiden Namensmuster als {@code LIKE}-Bedingung.
   *
   * <p>Der einzige Platzhalter darin ist {@code %}. Ein {@code _} kaeme mit derselben Wirkung vor
   * und ist deshalb in keinem der beiden Muster enthalten — dieselbe Falle, an der das naive {@code
   * LIKE 'ERROR_%'} scheitert (Regel F1, {@code docs/message-status.md}).
   */
  private static Condition artefaktnamen() {
    Condition bedingung = DSL.noCondition();
    for (String muster : Artefaktnamen.likeMuster()) {
      bedingung = bedingung.or(MESSAGEPROPERTY.MESSAGEPROPERTYNAME.like(muster));
    }
    return bedingung;
  }

  /**
   * Die Mandantenkette {@code Message → Process → ProjectMandant} als {@code EXISTS} — Wort fuer
   * Wort dieselbe wie in {@code NachrichtendetailRepository}.
   *
   * <p><b>Die Bedingung auf {@code ProjectMandant.MandantID} gehoert in die Unterabfrage</b>, nicht
   * daneben: {@code ProjectMandant} kommt in der aeusseren Abfrage gar nicht vor. Stuende die
   * Bedingung aussen, waere sie entweder ein Uebersetzungsfehler oder — schlimmer — ein
   * stillschweigend hinzugefuegter Join, der die Mandantengrenze aufweichte.
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
