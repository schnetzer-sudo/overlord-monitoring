package de.kraftwerkone.overlord.monitor.common;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Die <b>einzige</b> Stelle, an der ein {@code MessageStatus} fachlich eingeordnet wird. Liste,
 * Dashboard, Rollup und spaeter der Chatbot verwenden sie und bauen sie nicht nach — sonst driftet
 * die Einordnung ueber die Ausbaustufen auseinander.
 *
 * <p>{@code MessageStatus} ist <b>freier Text</b>, kein Aufzaehlungstyp ({@code CKECKED} ist der
 * Beweis). Unbekannte Werte werden zu {@link MessageStatusKind#UNGEKLAERT}, niemals zu einem
 * geratenen Wert. Ein Datenbanktest vergleicht {@code SELECT DISTINCT MessageStatus} gegen {@link
 * #bekannteStatuswerte()} und wird rot, sobald im Altsystem ein neuer Wert auftaucht — nur deshalb
 * ist die neutrale Behandlung vertretbar. Erhebung: {@code docs/message-status.md}.
 */
@Component
public class MessageStatusClassifier {

  /**
   * Die bekannten Statuswerte samt Einordnung, Stand {@code docs/message-status.md}. {@code
   * RUNNING} ist enthalten, kommt in der Testkopie aber null Mal vor (fluechtig).
   */
  private static final Map<String, MessageStatusKind> BEKANNT =
      Map.ofEntries(
          Map.entry("FINISHED", MessageStatusKind.ABGESCHLOSSEN),
          Map.entry("EERP_RECEIVED", MessageStatusKind.QUITTIERT),
          Map.entry("COMMIT_RECEIVED", MessageStatusKind.QUITTIERT),
          Map.entry("MERGED", MessageStatusKind.ZUSAMMENGEFUEHRT),
          Map.entry("SPLITTED", MessageStatusKind.AUFGETEILT),
          Map.entry("SUSPENDED", MessageStatusKind.WARTEND),
          Map.entry("RUNNING", MessageStatusKind.LAEUFT),
          Map.entry("ERROR_DUPLICATE", MessageStatusKind.FEHLER),
          Map.entry("ERROR_TIMEOUT", MessageStatusKind.FEHLER),
          Map.entry("COMMIT_REJECTED", MessageStatusKind.FEHLER),
          Map.entry("COMMIT_SENT", MessageStatusKind.UNGEKLAERT),
          Map.entry("CHECKED", MessageStatusKind.UNGEKLAERT),
          Map.entry("CKECKED", MessageStatusKind.UNGEKLAERT));

  /**
   * Das Praefix, das eine Nachricht zum Fehlerfall macht — die eine Stelle, an der es im Java-Code
   * steht. Sein Gegenstueck in SQL ist {@link #fehlerBedingung(Field)}.
   */
  private static final String FEHLER_PRAEFIX = "ERROR_";

  /** Menge aller bekannten Statuswerte (13). */
  public Set<String> bekannteStatuswerte() {
    return BEKANNT.keySet();
  }

  /**
   * Einordnung eines Rohwertes. Unbekanntes und {@code null} werden zu {@link
   * MessageStatusKind#UNGEKLAERT} — nie zu einem geratenen Wert.
   *
   * <p><b>Eine Ausnahme, ergaenzt am 06.08.2026 mit dem Listen-Endpunkt:</b> Ein unbekannter Wert
   * mit dem Praefix {@code ERROR_} wird zu {@link MessageStatusKind#FEHLER}. Das ist kein Raten,
   * sondern dieselbe Regel, die {@link #fehlerBedingung(Field)} seit Schritt 2 in SQL anwendet und
   * die {@code docs/datenmodell.md} §4 nennt: „Alles mit Praefix {@code ERROR_}. Der Teil dahinter
   * ist die Fehlerart." Ohne diese Zeile fielen Java und SQL auseinander — der Statusfilter {@code
   * FEHLER} liefert eine Zeile, die die Liste danach als „Bedeutung nicht verifiziert" beschriftet.
   * Ueber die Fehlerart wird weiterhin nichts behauptet, nur ueber die Kategorie.
   *
   * <p><b>Der Rohwert wird vor jedem Vergleich hochgestellt</b> (ergaenzt am 06.08.2026, Aufgabe
   * 10b). Die Sortierung des Quellschemas ist {@code utf8mb4_general_ci}, der Vergleich in SQL also
   * unabhaengig von der Schreibweise; {@link String#startsWith} und {@link Map#get} sind es nicht.
   * Ohne diese Zeile fiele ein Wert {@code error_x} in SQL unter {@link #fehlerBedingung(Field)}
   * und in Java unter {@code UNGEKLAERT} — der Statusfilter {@code FEHLER} lieferte also eine
   * Zeile, die die Liste anschliessend als „Bedeutung nicht verifiziert" beschriftet. Dieselbe
   * Unstimmigkeit gaelte fuer {@code commit_rejected} und fuer jeden anderen bekannten Wert.
   *
   * <p><b>Angeglichen wird in Java, nicht in SQL.</b> Ein {@code UPPER()} in der Bedingung kostete
   * jeden Indexbereich — {@code MessageStatusIDX} traegt den Statusfilter (Messungen L5 und L6),
   * und eine Funktion um die Spalte macht ihn unbrauchbar. {@link Locale#ROOT}, damit die
   * Umwandlung nicht an der Systemsprache haengt: Im tuerkischen Gebietsschema wird aus {@code i}
   * ein {@code İ}, und {@code FINISHED} traefe seinen eigenen Eintrag nicht mehr.
   */
  public MessageStatusKind einordnung(String status) {
    if (status == null) {
      return MessageStatusKind.UNGEKLAERT;
    }
    String hochgestellt = status.toUpperCase(Locale.ROOT);
    MessageStatusKind bekannt = BEKANNT.get(hochgestellt);
    if (bekannt != null) {
      return bekannt;
    }
    return hochgestellt.startsWith(FEHLER_PRAEFIX)
        ? MessageStatusKind.FEHLER
        : MessageStatusKind.UNGEKLAERT;
  }

  /**
   * Die <b>bekannten</b> Rohwerte einer Einordnung, aufsteigend sortiert.
   *
   * <p>Fuer {@link MessageStatusKind#UNGEKLAERT} und {@link MessageStatusKind#FEHLER} ist diese
   * Menge <b>nicht vollstaendig</b>: Beide sind nach oben offen (jeder unbekannte Wert bzw. jedes
   * unbekannte {@code ERROR_}-Praefix gehoert dazu). Wer filtern will, nimmt deshalb {@link
   * #bedingung(MessageStatusKind, Field)} und nicht diese Liste.
   */
  public SortedSet<String> rohwerte(MessageStatusKind einordnung) {
    return BEKANNT.entrySet().stream()
        .filter(eintrag -> eintrag.getValue() == einordnung)
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Der <b>eine</b> Rohwert einer Einordnung, die genau einen traegt.
   *
   * <p><b>Wozu es die Methode gibt.</b> Die Kacheln <i>Laeuft</i> und <i>Wartend</i> des Dashboards
   * vergleichen mit {@code =} auf den <b>Rohwert</b> und nicht ueber {@link
   * #bedingung(MessageStatusKind, Field)}: {@code MessageStatusIDX} traegt den Rohwert, und ein
   * {@code IN} ueber eine einelementige Menge waere derselbe Zugriff mit einer Unwahrheit darin —
   * es behauptete, es koenne mehrere geben.
   *
   * <p><b>Der Rohwert wird trotzdem hier geholt und nicht dort hingeschrieben.</b> Ein Literal
   * {@code "SUSPENDED"} im Dashboard waere dieselbe Zuordnung ein zweites Mal, und sie driftete
   * beim naechsten Statuswert von {@link #BEKANNT} weg — genau die Bauform, die {@code
   * docs/message-status.md} fuer die Einordnung selbst ausschliesst.
   *
   * @throws IllegalStateException wenn die Einordnung keinen oder mehr als einen bekannten Rohwert
   *     traegt. <b>Das ist Absicht und kein Versaeumnis:</b> Bekommt {@link
   *     MessageStatusKind#WARTEND} je einen zweiten Rohwert, ist die Kachel eine andere Frage
   *     geworden, und das soll auffallen — nicht stillschweigend zu einer halben Antwort werden.
   */
  public String einzigerRohwert(MessageStatusKind einordnung) {
    SortedSet<String> gefunden = rohwerte(einordnung);
    if (gefunden.size() != 1) {
      throw new IllegalStateException(
          "Die Einordnung "
              + einordnung
              + " traegt "
              + gefunden.size()
              + " bekannte Rohwerte statt genau einem: "
              + gefunden);
    }
    return gefunden.first();
  }

  /**
   * Der feste Text fuer {@code COMMIT_REJECTED} — <b>der einzige Anzeigetext in dieser Klasse</b>,
   * und er steht hier, weil {@code PROJEKTBESCHREIBUNG.md} §4.2 ihn woertlich vorschreibt: <i>„bei
   * {@code COMMIT_REJECTED} der feste Text ‚Vom Partner abgelehnt'"</i>.
   *
   * <p><b>Er ist eine Ausnahme und kein Muster.</b> Anzeigetexte gehoeren in die Oberflaeche;
   * dieser gehoert hierher, weil er keine Uebersetzung eines Rohwertes ist, sondern eine
   * <b>fachliche Festlegung</b>: {@code COMMIT_REJECTED} traegt keinen Namensteil hinter {@code
   * ERROR_}, aus dem sich eine Fehlerart ableiten liesse. Die Oberflaeche kann ihn ueber den
   * mitgelieferten Rohwert jederzeit uebersetzen.
   */
  public static final String ABGELEHNT_VOM_PARTNER = "Vom Partner abgelehnt";

  /**
   * Die <b>Fehlerart</b> zu einem Rohstatus — {@code PROJEKTBESCHREIBUNG.md} §4.2, an genau einer
   * Stelle.
   *
   * <ul>
   *   <li>{@code ERROR_DUPLICATE} → {@code DUPLICATE} — der Namensteil hinter dem Praefix
   *   <li>{@code COMMIT_REJECTED} → {@link #ABGELEHNT_VOM_PARTNER}
   *   <li><b>alles andere → der Rohwert, unveraendert</b>
   * </ul>
   *
   * <p><b>Der letzte Fall ist Regel Q4 und keine Nachlaessigkeit.</b> Ein unbekannter Wert bekommt
   * hier keine geratene Art; er steht so da, wie das Altsystem ihn geschrieben hat. Dasselbe gilt
   * fuer ein blankes {@code ERROR_} ohne Namensteil: Daraus laesst sich nichts ableiten, also wird
   * nichts abgeleitet.
   *
   * <p><b>Der Rohwert bleibt daneben stehen</b> — diese Methode ersetzt ihn nicht. Wer die Art
   * anzeigt, zeigt sie <i>zu</i> einem Rohwert; das Dashboard liefert beide ({@code
   * dashboard/FehlerartResponse}).
   *
   * <p>Verglichen wird auf dem <b>hochgestellten</b> Wert, aus demselben Grund wie in {@link
   * #einordnung(String)}: Die Sortierung des Quellschemas ist {@code utf8mb4_general_ci}, {@link
   * String#startsWith} ist es nicht. <b>Zurueckgegeben wird der Ausschnitt aus dem Original</b> —
   * angeglichen wird der Vergleich, nicht die Auskunft.
   */
  public String fehlerart(String status) {
    if (status == null) {
      return null;
    }
    String hochgestellt = status.toUpperCase(Locale.ROOT);
    if ("COMMIT_REJECTED".equals(hochgestellt)) {
      return ABGELEHNT_VOM_PARTNER;
    }
    if (hochgestellt.startsWith(FEHLER_PRAEFIX) && status.length() > FEHLER_PRAEFIX.length()) {
      return status.substring(FEHLER_PRAEFIX.length());
    }
    return status;
  }

  /**
   * Ist die Nachricht als Zeile fertig?
   *
   * <p><b>Diese Methode gehoert der Ueberfaelligkeitsrechnung und sonst niemandem.</b> Sie
   * beantwortet genau eine Frage: „Kann fuer diese Zeile noch eine Frist ablaufen?" Sie ist
   * <b>nicht</b> die Grundlage eines Filters „nur offene Nachrichten" — dort wuerde sie 1.051
   * {@code COMMIT_SENT}-Zeilen lautlos verschwinden lassen, obwohl {@code docs/message-status.md}
   * ausdruecklich festhaelt, dass wir ueber diese Zeilen <b>nichts wissen</b>. Für {@code
   * UNGEKLAERT} liefert sie {@code true}, und das ist hier die vorsichtige Antwort (keine
   * Behauptung, die Nachricht haenge); in einem Sichtbarkeitsfilter waere dieselbe {@code true} die
   * unvorsichtige. Wer „offen" im Sinne der Oberflaeche braucht, definiert das dort und begruendet
   * es dort.
   *
   * <p><b>Offen sind allein {@link MessageStatusKind#WARTEND} und {@link
   * MessageStatusKind#LAEUFT}.</b> Alles andere gilt als Endstatus — auch {@code AUFGETEILT} und
   * {@code ZUSAMMENGEFUEHRT}: Eine gemergte oder gesplittete Nachricht wird nicht wieder angefasst,
   * sie ist als <i>Zeile</i> fertig, auch wenn der fachliche Vorgang ueber die Verkettung
   * weiterlaeuft.
   *
   * <p><b>Die Aufteilung von {@code ZWISCHENSCHRITT} in zwei Werte (11.08.2026) aendert hier
   * nichts.</b> Beide neuen Werte liefern weiterhin {@code true}; M6 stuetzt das unveraendert.
   * Waeren sie offen, waeren bei {@code NEXANS} 39,6 Prozent aller Zeilen Kandidaten fuer
   * „ueberfaellig" — und die Kategorie waere Rauschen.
   *
   * <p>Messung M6 stuetzt das: {@code SPLITTED} und {@code MERGED} verteilen sich ueber fuenfzehn
   * Monate in stabiler Groessenordnung (38.428 bis 57.426 bzw. 20.609 bis 34.937 je Monat) und
   * ballen sich <b>nicht</b> am aktuellen Rand — waeren sie fluechtige Zwischenzustaende, saehe die
   * Verteilung anders aus. Zaehlten sie als offen, waeren <b>34,38 Prozent aller Zeilen</b>
   * Kandidaten fuer „ueberfaellig" und die Kategorie waere Rauschen.
   *
   * <p><b>{@code UNGEKLAERT} zaehlt hier als Endstatus</b> ({@code CHECKED}, {@code CKECKED},
   * {@code COMMIT_SENT}). Das ist keine Behauptung, die Nachricht sei fertig, sondern die
   * Weigerung, das Gegenteil zu behaupten: Sie als offen zu fuehren hiesse, wir wuessten, dass sie
   * <i>nicht</i> fertig ist — und genau das wissen wir nicht. So bleibt sie aus allen drei
   * Problemkategorien heraus, statt sie mit einer Vermutung zu fuellen.
   *
   * <p>Bewusst ein vollstaendiges {@code switch} ohne {@code default}: Ein neuer Wert in {@link
   * MessageStatusKind} soll hier einen Compilerfehler ausloesen und keine stille Voreinstellung
   * erben.
   */
  public boolean istEndstatus(MessageStatusKind einordnung) {
    return switch (einordnung) {
      case ABGESCHLOSSEN, QUITTIERT, FEHLER, AUFGETEILT, ZUSAMMENGEFUEHRT, UNGEKLAERT -> true;
      case WARTEND, LAEUFT -> false;
    };
  }

  /** Dasselbe fuer einen Rohwert. Unbekanntes wird ueber {@link #einordnung(String)} aufgeloest. */
  public boolean istEndstatus(String status) {
    return istEndstatus(einordnung(status));
  }

  /**
   * Die Einheit von {@code Message.MessageTimeout} — <b>an genau einer Stelle</b>.
   *
   * <p>Sekunden, nicht Minuten (entschieden am 01.08.2026, Messung M8): {@code SOSActionTimeout =
   * 1800} steht 37.120-mal neben dem Ablaufschritt {@code WAIT|30M}, und 1800 Sekunden sind exakt
   * 30 Minuten; der einzige andere vorkommende Wert {@code 300} passt zum Schritt {@code 5M}. Unter
   * der Minuten-Lesart staende eine Frist von 30 <i>Stunden</i> neben einem Schritt, der 30
   * <i>Minuten</i> wartet.
   *
   * <p>Die Belegkette hat eine benannte Schwachstelle: Gemessen ist {@code SOSActionTimeout}, nicht
   * {@code Message.MessageTimeout} selbst — auf der Testkopie ist kein direkter Beleg zu bekommen,
   * weil dort keine Nachricht existiert, an der diese Frist sichtbar ablaeuft. Deshalb steht die
   * Einheit hier als Konstante: Ein Gegenbeleg aus der Produktion kostet ein Wort und keine Suche.
   * Vollstaendig in {@code docs/messungen-schritt4.md}, Abschnitt M8.
   *
   * <h2>⚠️ Die Konstante traegt seit dem 03.09.2026 eine andere Bedeutung — und keinen Verbraucher
   * mehr</h2>
   *
   * <p><b>{@code MessageTimeout} ist die Frist des Waechters im Altsystem, und sie gilt nur fuer
   * {@code RUNNING}.</b> Eine Nachricht, die laenger als {@code MessageTimeout} in {@code RUNNING}
   * steht, wird vom Altsystem automatisch auf {@code ERROR_TIMEOUT} gesetzt; {@code SUSPENDED}
   * wartet absichtlich und wird nie automatisch beendet.
   *
   * <p><b>Herkunft:</b> fachliche Auskunft des Auftraggebers vom 03.09.2026. <b>Nicht gemessen.</b>
   * Die Testkopie kann sie nicht belegen: {@code RUNNING} kommt dort null Mal vor, und die 538
   * {@code SUSPENDED} sind der Bestand <i>eines</i> Status in <i>einer</i> Gestalt. <b>Gegen die
   * Produktion zu pruefen</b> mit der Abfrage in {@code docs/message-status.md}, Abschnitt „Die
   * offene Pruefung".
   *
   * <p><b>Sie steht hier ohne Verbraucher im Anwendungscode</b>, seit die Ueberfaelligkeitsrechnung
   * entfallen ist (E‑71). Das ist Absicht und dieselbe Behandlung, die {@code --ueberfaellig} in
   * {@code globals.css} bekommt (E‑77): Die Einheit ist teuer erarbeitet, die Belegkette steht, und
   * sie wird an dem Tag wieder gebraucht, an dem eine echte Schwelle fuer {@code RUNNING}
   * zurueckkommt. {@code MessageStatusClassifierTest} haelt sie fest.
   */
  public static final ChronoUnit TIMEOUT_EINHEIT = ChronoUnit.SECONDS;

  /*
   * Hier standen bis zum 03.09.2026 `timeoutZeitpunkt(messageLastUpdate, messageTimeout)` und
   * `istUeberfaellig(status, messageLastUpdate, messageTimeout, jetzt)` — die Problemkategorie
   * `Ueberfaellig` in Java, und darunter `ueberfaelligBedingung(...)` als ihr Gegenstueck in SQL,
   * dazu `offeneRohwerte()` und `TIMEOUT_DATEPART` als deren Bausteine.
   *
   * ALLE FUENF SIND ENTFALLEN (Entscheidung E-71, Schritt 10b-4). Der Grund ist nicht Aufraeumen,
   * sondern eine fachliche Auskunft des Auftraggebers vom 03.09.2026: `SUSPENDED` wartet
   * absichtlich und wird nie ueberfaellig; `RUNNING` ueber der Frist ist nur der Spalt zwischen
   * Fristablauf und dem Zuschlagen des Waechters, danach heisst der Status `ERROR_TIMEOUT` und
   * damit FEHLER. Damit hat die Kategorie im eingeschwungenen Zustand keine wahren Treffer.
   *
   * Gemessen an der Testkopie markierte `istUeberfaellig` im Gesamtbestand 538 Zeilen -- davon 538
   * `SUSPENDED` und 0 `RUNNING`. Nach der Regel sind das 538 Fehlalarme und kein einziger Treffer.
   *
   * Die Auskunft ist NICHT GEMESSEN. Vollstaendig samt Herkunftsvermerk und der offenen Pruefung
   * gegen die Produktion in docs/message-status.md, Abschnitt "Ueberfaelligkeit"; die Kategorie
   * selbst in PROJEKTBESCHREIBUNG.md Paragraf 4.2, Punkt 2.
   *
   * `istEndstatus` ist ausdruecklich GEBLIEBEN: Sie beantwortet weiterhin eine eigene, richtige
   * Frage, und die Kacheln `Laeuft` und `Wartend` sind ueber `einzigerRohwert` an sie gebunden.
   */

  /**
   * Die <b>eine</b> wiederverwendbare Fehlerbedingung fuer SQL:
   *
   * <pre>MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'</pre>
   *
   * Nicht {@code LEFT(MessageStatus, 6) = 'ERROR_'}: diese Form kann {@code MessageStatusIDX} nicht
   * nutzen und erzwingt in Kombination mit der Oder-Bedingung einen vollen Durchlauf. Die {@code
   * LIKE}-Fassung ergibt zwei Indexbereiche, die MariaDB zusammenfuehren kann.
   *
   * <p>Das Feld wird uebergeben, damit dieser gemeinsame Baustein nicht auf generierte {@code
   * jooq.glassfish}-Typen zugreifen muss — die Fehlerbedingung bleibt in {@code common}.
   */
  public Condition fehlerBedingung(Field<String> messageStatus) {
    return messageStatus.like("ERROR\\_%", '\\').or(messageStatus.eq("COMMIT_REJECTED"));
  }

  /**
   * Die Uebersetzung <b>Einordnung → SQL</b> — das Gegenstueck zu {@link #einordnung(String)} und
   * mit ihm zusammen an genau einer Stelle.
   *
   * <p>Der Filter der Liste arbeitet ueber {@link MessageStatusKind}, nicht ueber Rohwerte: Ein
   * Nutzer sucht „Fehler", nicht {@code ERROR_DUPLICATE} — und die Menge der Rohwerte je Kategorie
   * gehoert dem Altsystem, nicht der Oberflaeche. Waere die Uebersetzung im Repository nachgebaut,
   * driftete sie beim naechsten neuen Statuswert von der Anzeige weg.
   *
   * <p>Die drei Faelle:
   *
   * <ul>
   *   <li>{@code FEHLER} nutzt {@link #fehlerBedingung(Field)} — nicht die Aufzaehlung der drei
   *       bekannten Rohwerte, sonst faende der Filter eine kuenftige {@code ERROR_}-Art nicht.
   *   <li>{@code UNGEKLAERT} ist der <b>Rest</b>: alles, was weder Fehler noch einer der anderen
   *       bekannten Werte ist — und {@code NULL}. Es kann keine Aufzaehlung sein, weil genau die
   *       unbekannten Werte hierher gehoeren.
   *   <li>Alle uebrigen sind geschlossene Mengen und werden aufgezaehlt.
   * </ul>
   *
   * <p><b>Schreibweise: erledigt am 06.08.2026.</b> Die Sortierung des Quellschemas ist {@code
   * utf8mb4_general_ci}, der Vergleich hier also gross-/kleinschreibungsunabhaengig. Angeglichen
   * wird in Java — {@link #einordnung(String)} stellt den Rohwert vor jedem Vergleich hoch —, nicht
   * durch ein {@code UPPER()} in dieser Bedingung: Das kostete jeden Indexbereich auf {@code
   * MessageStatusIDX}, den die Messungen L5 und L6 als Treiber des Statusfilters ausweisen.
   */
  public Condition bedingung(MessageStatusKind einordnung, Field<String> messageStatus) {
    return switch (einordnung) {
      case FEHLER -> fehlerBedingung(messageStatus);
      case UNGEKLAERT ->
          messageStatus
              .isNull()
              .or(
                  DSL.not(fehlerBedingung(messageStatus))
                      .and(messageStatus.notIn(eindeutigeRohwerte())));
      case ABGESCHLOSSEN, QUITTIERT, WARTEND, LAEUFT, AUFGETEILT, ZUSAMMENGEFUEHRT ->
          messageStatus.in(rohwerte(einordnung));
    };
  }

  /**
   * Mehrere Einordnungen, mit ODER verbunden. Eine leere Auswahl filtert nicht — sie bedeutet
   * „alle", nicht „keine".
   */
  public Condition bedingung(
      Collection<MessageStatusKind> einordnungen, Field<String> messageStatus) {
    return einordnungen.stream()
        .map(einordnung -> bedingung(einordnung, messageStatus))
        .reduce(Condition::or)
        .orElseGet(DSL::noCondition);
  }

  /*
   * Hier stand bis zum 11.08.2026 ein `ohne(einordnung, feld)` — das Gegenteil einer Einordnung,
   * gebaut ausschliesslich fuer `zwischenschritte=false`. Es ist mit dem Ausblende-Schalter
   * entfallen (docs/nachrichtenliste.md §5): Die Liste filtert nicht mehr nach Status, ausser der
   * Nutzer sagt es ausdruecklich. Braucht je wieder jemand eine Ausschlussbedingung, gehoert das
   * `IS NULL` davor wieder dazu — `NOT (status IN (…))` ist fuer eine NULL-Spalte selbst NULL und
   * damit nicht wahr, eine Zeile ohne Status fiele also aus der Liste.
   */

  /** Alle bekannten Rohwerte, deren Einordnung eine geschlossene Menge ist. */
  private SortedSet<String> eindeutigeRohwerte() {
    return BEKANNT.entrySet().stream()
        .filter(
            eintrag ->
                eintrag.getValue() != MessageStatusKind.UNGEKLAERT
                    && eintrag.getValue() != MessageStatusKind.FEHLER)
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
