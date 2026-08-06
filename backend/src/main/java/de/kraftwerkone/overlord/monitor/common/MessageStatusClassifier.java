package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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
          Map.entry("MERGED", MessageStatusKind.ZWISCHENSCHRITT),
          Map.entry("SPLITTED", MessageStatusKind.ZWISCHENSCHRITT),
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
   */
  public MessageStatusKind einordnung(String status) {
    if (status == null) {
      return MessageStatusKind.UNGEKLAERT;
    }
    MessageStatusKind bekannt = BEKANNT.get(status);
    if (bekannt != null) {
      return bekannt;
    }
    return status.startsWith(FEHLER_PRAEFIX)
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
   * Ist die Nachricht als Zeile fertig?
   *
   * <p><b>Offen sind allein {@link MessageStatusKind#WARTEND} und {@link
   * MessageStatusKind#LAEUFT}.</b> Alles andere gilt als Endstatus — auch {@code ZWISCHENSCHRITT}:
   * Eine gemergte oder gesplittete Nachricht wird nicht wieder angefasst, sie ist als <i>Zeile</i>
   * fertig, auch wenn der fachliche Vorgang ueber die Verkettung weiterlaeuft.
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
      case ABGESCHLOSSEN, QUITTIERT, FEHLER, ZWISCHENSCHRITT, UNGEKLAERT -> true;
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
   */
  public static final ChronoUnit TIMEOUT_EINHEIT = ChronoUnit.SECONDS;

  /**
   * Der Zeitpunkt, zu dem die Nachricht ueberfaellig wird — {@code MessageLastUpdate +
   * MessageTimeout}.
   *
   * <p>Leer, wenn es keinen gibt: bei {@code MessageTimeout = 0} („kein Timeout") und bei {@code
   * NULL}. {@code NULL} kommt in 3,34 Millionen Zeilen der Testkopie <b>kein einziges Mal</b> vor
   * (Messung M2), wird aber behandelt — die Spalte laesst es zu, und eine Neubefuellung oder die
   * Produktion muss sich nicht daran halten, was die Testkopie zufaellig enthaelt.
   *
   * <p>Ein negativer Wert wird wie {@code 0} behandelt: Eine Frist, die vor ihrem Beginn ablaeuft,
   * ist keine Frist. Vorgekommen ist das nicht.
   */
  public Optional<LocalDateTime> timeoutZeitpunkt(
      LocalDateTime messageLastUpdate, Integer messageTimeout) {
    if (messageLastUpdate == null || messageTimeout == null || messageTimeout <= 0) {
      return Optional.empty();
    }
    return Optional.of(messageLastUpdate.plus(messageTimeout, TIMEOUT_EINHEIT));
  }

  /**
   * Problemkategorie <b>Ueberfaellig</b>: Die Nachricht ist <b>nicht</b> in einem Endstatus, und
   * ihr Timeout-Zeitpunkt liegt in der Vergangenheit.
   *
   * <p>Getrennt von <i>Fehler</i> und <i>Unquittiert</i> und niemals mit ihnen zusammengefasst
   * (Regel Q3). {@code SUSPENDED} ist kein Fehler — aber es ist offen und kann damit ueberfaellig
   * werden; das ist der fachliche Kern dieser Kategorie.
   *
   * <p><b>„Laeuft noch" heisst „nicht in einem Endstatus"</b>, nicht {@code MessageStatus =
   * 'RUNNING'}: Diesen Wert gibt es in der Testkopie null Mal, er ist fluechtig und existiert nur,
   * solange eine Nachricht tatsaechlich in Arbeit ist.
   *
   * @param jetzt Referenzzeitpunkt, vom Aufrufer aus der <b>Anwendungsuhr</b> zu ziehen (Regel Z1)
   *     — im Profil {@code dev} die zurueckversetzte. Mit der Systemuhr waere lokal jede Nachricht
   *     ueberfaellig.
   */
  public boolean istUeberfaellig(
      String status, LocalDateTime messageLastUpdate, Integer messageTimeout, LocalDateTime jetzt) {
    if (istEndstatus(status)) {
      return false;
    }
    return timeoutZeitpunkt(messageLastUpdate, messageTimeout)
        .filter(zeitpunkt -> zeitpunkt.isBefore(jetzt))
        .isPresent();
  }

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
   * <p><b>Die eine bekannte Unschaerfe:</b> Die Sortierung des Quellschemas ist {@code
   * utf8mb4_general_ci}, der Vergleich in SQL also gross-/kleinschreibungsunabhaengig, {@link
   * String#startsWith} dagegen nicht. Ein Wert {@code error_x} wuerde in SQL als Fehler gefunden
   * und in Java als ungeklaert beschriftet. Alle zwoelf vorkommenden Werte sind durchgehend gross
   * geschrieben, und {@code DatenzugriffDbIT} wird rot, sobald ein dreizehnter auftaucht — deshalb
   * bleibt es bei der Indexnutzung statt eines {@code UPPER()}, das jeden Indexbereich kostete.
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
      case ABGESCHLOSSEN, QUITTIERT, WARTEND, LAEUFT, ZWISCHENSCHRITT ->
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

  /**
   * Das Gegenteil: alles ausser dieser Einordnung. Grundlage von {@code zwischenschritte=false}.
   *
   * <p>Das {@code IS NULL} davor ist kein Schmuck: {@code NOT (status IN (…))} ist fuer eine {@code
   * NULL}-Spalte selbst {@code NULL} und damit nicht wahr — eine Zeile ohne Status fiele aus der
   * Liste, sobald man <i>irgendetwas</i> ausschliesst. Vorgekommen ist das in der Testkopie nicht;
   * die Spalte laesst es zu.
   */
  public Condition ohne(MessageStatusKind einordnung, Field<String> messageStatus) {
    return messageStatus.isNull().or(DSL.not(bedingung(einordnung, messageStatus)));
  }

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
