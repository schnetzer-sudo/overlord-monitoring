package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Service aus den Rohzeilen macht: den offenen Zustand benennen, den Metadaten-Schritt
 * ausnehmen, Dauern rechnen, Werte kappen. <b>Ohne Datenbank</b> — die Statements pruefen die
 * Integrationstests und die Messungen.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NachrichtendetailServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final String MESSAGE_ID = "eine-nachricht";
  private static final String ABLAUF = "ablauf-a";
  private static final String ANDERER_ABLAUF = "ablauf-b";

  /** Dieselbe Zone wie die Anwendungsuhr — die Umrechnung nach UTC haengt daran. */
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final LocalDateTime T0 = LocalDateTime.parse("2025-12-29T10:00:00");
  private static final LocalDateTime T1 = LocalDateTime.parse("2025-12-29T10:00:30");
  private static final LocalDateTime T2 = LocalDateTime.parse("2025-12-29T10:30:00");

  /**
   * Der Stand der <b>Anwendungsuhr</b> in diesen Tests — fest, damit „wartet seit" eine pruefbare
   * Zahl ist und nicht die Laufzeit des Testlaufs.
   *
   * <p>Sie steht 90 Minuten hinter {@link #T2}, dem {@code MessageLastUpdate} des Kopfes. Mit einer
   * Frist von 1800 Sekunden liegt der Timeout-Zeitpunkt (11:00) damit in der Vergangenheit — eine
   * offene Nachricht ist hier also ueberfaellig, eine abgeschlossene nie.
   */
  private static final LocalDateTime JETZT = LocalDateTime.parse("2025-12-29T12:00:00");

  @Mock private NachrichtendetailRepository detailRepository;

  /** Kein Mock: Die Einordnung ist Fachlogik und soll hier mitlaufen. */
  private final MessageStatusClassifier statusClassifier = new MessageStatusClassifier();

  private NachrichtendetailService service() {
    return new NachrichtendetailService(
        detailRepository, statusClassifier, Clock.fixed(JETZT.atZone(ZONE).toInstant(), ZONE));
  }

  /** Ein Kopf, dessen Verweis auf den Schritt {@code 1} des Ablaufs {@code A} zeigt. */
  private static NachrichtKopfZeile kopf(String status) {
    return kopf(status, ABLAUF, (short) 1);
  }

  /**
   * Ein Kopf mit frei gewaehltem Verweis. Ueber ihn entscheidet sich {@link
   * OffenerZustand#WARTET_IN} gegen {@link OffenerZustand#WARTET_VOR} — die Entscheidung, die seit
   * dem 10.08.2026 hier faellt und nicht mehr in der Oberflaeche.
   */
  private static NachrichtKopfZeile kopf(String status, String sosId, Short sosActionId) {
    return kopf(status, sosId, sosActionId, null, null, null, null);
  }

  /**
   * Derselbe Kopf mit den vier Verkettungsspalten. Aus ihnen entstehen die Rollen — <b>in {@code
   * common/Kettenrollen}</b>, weshalb dieser Test nur prueft, dass der Service sie dort holt und
   * unveraendert weitergibt.
   */
  private static NachrichtKopfZeile kopf(
      String status,
      String sosId,
      Short sosActionId,
      Boolean source,
      String sourceMessageId,
      String targetMessageId,
      Boolean target) {
    return new NachrichtKopfZeile(
        MESSAGE_ID,
        status,
        T2,
        (short) 1800,
        "prozess-1",
        "40000_AMG_LAB_VDA",
        "300_KundenEingehend",
        "Versand Einzel IDOC aus Split",
        sosId,
        sosActionId,
        sosId == null ? null : "Send Message to Pool",
        22,
        // Neun BAM-Werte — die Gestalt der Ankernachricht aus M42-0. Der Service reicht die Zahl
        // nur durch; was daraus wird, entscheidet der BAM-Endpunkt im Fachpaket `bam`.
        9,
        source,
        sourceMessageId,
        targetMessageId,
        target);
  }

  /** Der Metadaten-Schritt: {@code SOSActionID = 0}, keine Bausteine (S1). */
  private static MessageAktion metadatenSchritt() {
    return metadatenSchritt(T0);
  }

  /**
   * Derselbe Schritt mit frei gewaehltem Ende. Es ist der Bezugspunkt von {@code
   * wartetSeitSekunden} bei {@link OffenerZustand#EMPFANGEN} — deshalb muss er sich hier vom Start
   * unterscheiden lassen.
   */
  private static MessageAktion metadatenSchritt(LocalDateTime ende) {
    return new MessageAktion((short) 0, ABLAUF, (short) 0, T0, ende, null, (short) 0);
  }

  private static MessageAktion aktion(
      short position, short sosActionId, LocalDateTime start, LocalDateTime ende) {
    return new MessageAktion(
        position, ABLAUF, sosActionId, start, ende, "NXS_FILE_CONVERT|E2A", (short) 1800);
  }

  /** Ein offener Metadaten-Schritt — er darf keinen Zustand tragen, er ist keine Zeile. */
  private static MessageAktion offenerMetadatenSchritt() {
    return metadatenSchritt(null);
  }

  private void gib(NachrichtKopfZeile kopf, List<MessageAktion> aktionen) {
    gib(kopf, aktionen, List.of(), List.of());
  }

  private void gib(
      NachrichtKopfZeile kopf,
      List<MessageAktion> aktionen,
      List<Ablaufschritt> ablaufschritte,
      List<MessageEigenschaft> kuratiert) {
    when(detailRepository.findeKopf(any(), anyString())).thenReturn(kopf);
    when(detailRepository.findeAktionen(any(), anyString())).thenReturn(aktionen);
    when(detailRepository.findeAblaufschritte(any(), anyString())).thenReturn(ablaufschritte);
    when(detailRepository.findeKuratierteEigenschaften(any(), anyString())).thenReturn(kuratiert);
  }

  @Nested
  @DisplayName("404 statt einer leeren Antwort")
  class NichtGefunden {

    @Test
    @DisplayName("Eine unsichtbare Nachricht ist 404 — fremd und erfunden sind ununterscheidbar")
    void unsichtbare_nachricht_ist_404() {
      when(detailRepository.findeKopf(any(), anyString())).thenReturn(null);

      assertThatThrownBy(() -> service().detail(MANDANT, MESSAGE_ID))
          .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Auch die Eigenschaften antworten 404 und nicht mit einer leeren Liste")
    void eigenschaften_antworten_404() {
      when(detailRepository.existiert(any(), anyString())).thenReturn(false);

      assertThatThrownBy(() -> service().eigenschaften(MANDANT, MESSAGE_ID))
          .as("eine leere Liste waere eine andere Auskunft als „gibt es nicht\"")
          .isInstanceOf(RessourceNichtGefundenException.class);
    }
  }

  @Nested
  @DisplayName("Der offene Zustand")
  class Zustand {

    /**
     * Der gemessene Normalfall (M29): Bei allen 538 wartenden Nachrichten der Testkopie zeigt der
     * Verweis auf den Schritt, der zuletzt gelaufen ist.
     */
    @Test
    @DisplayName("WARTET_IN: jede Aktion beendet, und der Verweis zeigt auf den letzten Schritt")
    void wartet_in() {
      gib(kopf("SUSPENDED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.WARTET_IN);
      assertThat(detail.naechsterSchritt())
          .as("der Verweis kommt auch hier mit — die Oberflaeche schreibt ihn in den Tooltip")
          .isEqualTo("Send Message to Pool");
      assertThat(detail.schritte()).allMatch(schritt -> !schritt.laeuftAuf());
    }

    /**
     * ⚠️ <b>In der Testkopie null Mal beobachtet</b> (M29, {@code n = 538}). Gebaut, hier
     * unit-getestet, nie gesehen — siehe {@code nachrichtendetail.md} §10.12.
     */
    @Test
    @DisplayName("WARTET_VOR: der Verweis zeigt auf einen anderen Schritt")
    void wartet_vor() {
      gib(
          kopf("SUSPENDED", ABLAUF, (short) 9),
          List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.WARTET_VOR);
      assertThat(detail.naechsterSchritt()).isEqualTo("Send Message to Pool");
    }

    @Test
    @DisplayName("Verglichen wird gegen den LETZTEN Schritt, nicht gegen irgendeinen gelaufenen")
    void vergleich_gegen_den_letzten_schritt() {
      List<MessageAktion> zweiSchritte =
          List.of(
              metadatenSchritt(),
              aktion((short) 1, (short) 1, T0, T1),
              aktion((short) 2, (short) 2, T1, T2));

      gib(kopf("SUSPENDED", ABLAUF, (short) 2), zweiSchritte);
      assertThat(service().detail(MANDANT, MESSAGE_ID).offenerZustand())
          .isEqualTo(OffenerZustand.WARTET_IN);

      gib(kopf("SUSPENDED", ABLAUF, (short) 1), zweiSchritte);
      assertThat(service().detail(MANDANT, MESSAGE_ID).offenerZustand())
          .as(
              "Schritt 1 ist gelaufen, aber er ist nicht der letzte — die Nachricht wartet nicht"
                  + " in ihm")
          .isEqualTo(OffenerZustand.WARTET_VOR);
    }

    @Test
    @DisplayName("Auch der Ablauf muss stimmen, nicht nur die Schrittkennung")
    void beide_haelften_des_schluessels_zaehlen() {
      gib(
          kopf("SUSPENDED", ANDERER_ABLAUF, (short) 1),
          List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).offenerZustand())
          .as("2,51 Prozent der Nachrichten haben Schritte aus mehr als einem Ablauf (M20)")
          .isEqualTo(OffenerZustand.WARTET_VOR);
    }

    @Test
    @DisplayName("Ohne Verweis wartet die Nachricht vor nichts Benanntem")
    void ohne_verweis_wartet_vor() {
      gib(
          kopf("SUSPENDED", null, null),
          List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand())
          .as("ueber den Gesamtbestand laeuft dieser Verweis zu 43,9 Prozent ins Leere (M13)")
          .isEqualTo(OffenerZustand.WARTET_VOR);
      assertThat(detail.naechsterSchritt())
          .as("die Oberflaeche benennt das und laesst es nicht weg")
          .isNull();
    }

    @Test
    @DisplayName("LAEUFT_AUF: offen, und eine Aktion hat kein Ende")
    void laeuft_auf() {
      gib(
          kopf("RUNNING"),
          List.of(
              metadatenSchritt(),
              aktion((short) 1, (short) 1, T0, T1),
              aktion((short) 2, (short) 2, T1, null)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.LAEUFT_AUF);
      assertThat(detail.naechsterSchritt())
          .as("der naechste Schritt gehoert dem Warten, nicht dem Laufen")
          .isNull();
      assertThat(detail.schritte())
          .filteredOn(SchrittResponse::laeuftAuf)
          .extracting(SchrittResponse::position)
          .containsExactly(2);
    }

    /**
     * <b>Der zweite der beiden Faelle, die bis zum 10.08.2026 {@code OHNE_SCHRITT} hiessen.</b> Er
     * ist eine Auskunft ueber die <b>Datenlage</b>: Zu dieser Nachricht ist kein Ablauf
     * protokolliert. In der Testkopie nicht beobachtet (M16 3: {@code ohne_jede_aktion = 0}), aber
     * nicht widerlegt — {@code MessageAction} kennt keinen Zwang, der ihn ausschloesse.
     */
    @Test
    @DisplayName("OHNE_AKTION: offen, und es gibt gar keine Aktion")
    void ohne_aktion() {
      gib(kopf("SUSPENDED"), List.of());

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand())
          .as(
              "ueber einer leeren Menge waere „jede Aktion ist beendet\" wahr — der Fall muss vorher"
                  + " abgefangen werden")
          .isEqualTo(OffenerZustand.OHNE_AKTION);
      assertThat(detail.naechsterSchritt()).isNull();
      assertThat(detail.start())
          .as("MIN(MessageActionStart) ueber eine leere Menge — es gibt keinen Anker")
          .isNull();
    }

    /**
     * <b>Der erste der beiden Faelle</b>, und eine Auskunft ueber die <b>Plattform</b>: Die
     * Nachricht ist angekommen und seitdem nicht weitergelaufen. Der Zustand wird ueber die
     * <b>Schrittfolge</b> bestimmt, also ohne den Metadaten-Schritt — fuer den Nutzer hat sie
     * keinen Schritt.
     */
    @Test
    @DisplayName("EMPFANGEN: nur der Metadaten-Schritt — angekommen, seitdem nichts")
    void nur_metadatenschritt_ist_empfangen() {
      gib(kopf("SUSPENDED"), List.of(metadatenSchritt(T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.EMPFANGEN);
      assertThat(detail.wartetSeitSekunden())
          .as(
              "ab dem Ende des Schritts 0 (T1 = 10:00:30) — er ist kein Verarbeitungsschritt, aber"
                  + " er ist das Ereignis mit einem Zeitpunkt")
          .isEqualTo(7170L);
      assertThat(detail.start())
          .as("fuer den fachlichen Start zaehlt er ohnehin (M17 3)")
          .isEqualTo(T0.atZone(ZONE).toInstant());
      assertThat(detail.naechsterSchritt()).isNull();
    }

    @Test
    @DisplayName("EMPFANGEN ohne Ende am Schritt 0 rechnet ab dessen Beginn")
    void empfangen_faellt_auf_den_beginn_zurueck() {
      gib(kopf("SUSPENDED"), List.of(offenerMetadatenSchritt()));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand())
          .as("sonst waere ein Schritt als laufend markiert, den die Zeitleiste gar nicht zeigt")
          .isEqualTo(OffenerZustand.EMPFANGEN);
      assertThat(detail.wartetSeitSekunden())
          .as("dieselbe Zeile, ersatzweise ihr Beginn (T0 = 10:00:00) — kein anderer Anker")
          .isEqualTo(7200L);
    }

    /**
     * Mit {@code EMPFANGEN} bekommt die Problemkategorie 2 zum ersten Mal einen Zustand, in dem sie
     * wirklich etwas sagt: eingegangen und ueber die Frist hinaus nicht weitergelaufen. Die
     * Rechnung selbst aendert sich dadurch nicht — sie haengt an {@code MessageLastUpdate +
     * MessageTimeout} und {@code istEndstatus}, nicht am offenen Zustand.
     */
    @Test
    @DisplayName("EMPFANGEN und ueber der Frist ist ueberfaellig")
    void empfangen_kann_ueberfaellig_sein() {
      gib(kopf("SUSPENDED"), List.of(metadatenSchritt(T1)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).ueberfaellig())
          .as("MessageLastUpdate 10:30 + 1800 s = 11:00, die Anwendungsuhr steht auf 12:00")
          .isTrue();
    }

    @Test
    @DisplayName("OHNE_AKTION traegt keine Wartedauer — es gibt keinen Anker")
    void ohne_aktion_hat_keine_wartedauer() {
      gib(kopf("SUSPENDED"), List.of());

      assertThat(service().detail(MANDANT, MESSAGE_ID).wartetSeitSekunden())
          .as("eine Dauer aus MessageLastUpdate waere eine erfundene Zahl")
          .isNull();
    }

    @Test
    @DisplayName("KEINER: die Nachricht ist nicht offen")
    void keiner() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.KEINER);
      assertThat(detail.naechsterSchritt()).isNull();
    }

    /**
     * M22: 39 der 95 offenen Aktionen des Gesamtbestands gehoeren zu {@code FINISHED}, sieben zu
     * {@code CHECKED}. Dort ist ein fehlendes Ende eine Protokolluecke und kein Haenger.
     */
    @Test
    @DisplayName("Ein fehlendes Ende auf einer abgeschlossenen Nachricht ist kein Haenger")
    void fehlendes_ende_ohne_offenen_status() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, null)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.KEINER);
      assertThat(detail.schritte()).allMatch(schritt -> !schritt.laeuftAuf());
      assertThat(detail.schritte().getFirst().ende())
          .as("das rohe Merkmal bleibt trotzdem ablesbar")
          .isNull();
    }

    @Test
    @DisplayName("UNGEKLAERT gilt hier nicht als offen")
    void ungeklaert_ist_nicht_offen() {
      gib(kopf("COMMIT_SENT"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, null)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).offenerZustand())
          .as(
              "istEndstatus gehoert der Ueberfaelligkeitsrechnung; „offen\" ist hier WARTEND/LAEUFT")
          .isEqualTo(OffenerZustand.KEINER);
    }
  }

  @Nested
  @DisplayName("Die Schrittfolge")
  class Schrittfolge {

    @Test
    @DisplayName("Der Metadaten-Schritt erscheint nicht — sein Start zaehlt aber fuer den Beginn")
    void metadatenschritt_erscheint_nicht() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T1, T2)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.schritte()).extracting(SchrittResponse::position).containsExactly(1);
      assertThat(detail.start())
          .as("an ihm kommt die Nachricht ins System (Regel Q2, M17 3)")
          .isEqualTo(T0.atZone(ZONE).toInstant());
    }

    @Test
    @DisplayName("Die Dauer wird in ganzen Sekunden geliefert")
    void dauer_in_sekunden() {
      gib(kopf("FINISHED"), List.of(aktion((short) 1, (short) 1, T0, T1)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).schritte().getFirst().dauerSekunden())
          .isEqualTo(30L);
    }

    @Test
    @DisplayName("Eine negative Dauer wird null und nicht negativ")
    void negative_dauer_wird_null() {
      gib(kopf("FINISHED"), List.of(aktion((short) 1, (short) 1, T2, T0)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).schritte().getFirst().dauerSekunden())
          .as("M16 (1) hat null solche Faelle gemessen — die Regel ist trotzdem billig")
          .isNull();
    }

    @Test
    @DisplayName("Ohne Ende gibt es keine Dauer")
    void ohne_ende_keine_dauer() {
      gib(kopf("RUNNING"), List.of(aktion((short) 1, (short) 1, T0, null)));

      SchrittResponse schritt = service().detail(MANDANT, MESSAGE_ID).schritte().getFirst();
      assertThat(schritt.dauerSekunden()).isNull();
      assertThat(schritt.ende()).isNull();
    }

    @Test
    @DisplayName("Name und Herkunft kommen aus der Ablaufdefinition")
    void name_und_herkunft() {
      gib(
          kopf("FINISHED"),
          List.of(aktion((short) 1, (short) 1, T0, T1)),
          List.of(
              new Ablaufschritt(ABLAUF, (short) 1, "Datei konvertiert", "NXS_FILE_CONVERT|E2A")),
          List.of());

      SchrittResponse schritt = service().detail(MANDANT, MESSAGE_ID).schritte().getFirst();

      assertThat(schritt.name()).isEqualTo("Datei konvertiert");
      assertThat(schritt.namensherkunft()).isEqualTo(Namensherkunft.DIREKT);
      assertThat(schritt.rohwert()).isEqualTo("NXS_FILE_CONVERT|E2A");
      assertThat(schritt.timeoutSekunden()).isEqualTo(1800);
    }

    @Test
    @DisplayName("Ohne Ablaufdefinition traegt jeder Schritt seinen Rohwert — keiner steht leer")
    void ohne_definition_traegt_jeder_seinen_rohwert() {
      gib(
          kopf("FINISHED"),
          List.of(aktion((short) 1, (short) 1, T0, T1), aktion((short) 2, (short) 2, T1, T2)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).schritte())
          .isNotEmpty()
          .allSatisfy(
              schritt -> {
                assertThat(schritt.name()).isNotBlank();
                assertThat(schritt.namensherkunft()).isEqualTo(Namensherkunft.ROHWERT);
              });
    }
  }

  /**
   * Wartedauer, Frist, Ueberfaelligkeit und Gesamtdauer — <b>alles gegen die Anwendungsuhr und
   * nichts im Browser</b>. Der Bezugspunkt {@link #JETZT} steht fest, damit hier Zahlen stehen und
   * keine Ungleichungen.
   */
  @Nested
  @DisplayName("Wartedauer, Frist und Ueberfaelligkeit")
  class WartenUndFrist {

    @Test
    @DisplayName("Beim Warten laeuft die Uhr ab dem Ende des letzten Schritts")
    void wartet_seit_dem_ende_des_letzten_schritts() {
      gib(kopf("SUSPENDED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).wartetSeitSekunden())
          .as("T1 = 10:00:30, die Anwendungsuhr steht auf 12:00:00")
          .isEqualTo(7170L);
    }

    @Test
    @DisplayName("Beim Laufen ab dem Beginn der offenen Aktion")
    void laeuft_seit_dem_beginn_der_offenen_aktion() {
      gib(
          kopf("RUNNING"),
          List.of(
              metadatenSchritt(),
              aktion((short) 1, (short) 1, T0, T1),
              aktion((short) 2, (short) 2, T1, null)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).wartetSeitSekunden())
          .as("die offene Aktion beginnt 10:00:30 — nicht das Ende der vorigen zaehlt")
          .isEqualTo(7170L);
    }

    @Test
    @DisplayName("Eine abgeschlossene Nachricht wartet auf nichts")
    void abgeschlossene_nachricht_wartet_nicht() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.wartetSeitSekunden()).isNull();
      assertThat(detail.ueberfaellig())
          .as("ein Endstatus kann nicht ueberfaellig werden — istEndstatus entscheidet das")
          .isFalse();
    }

    @Test
    @DisplayName("Ueberfaellig: offen und die Frist ist abgelaufen")
    void ueberfaellig_wenn_offen_und_frist_abgelaufen() {
      gib(kopf("SUSPENDED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.ueberfaellig())
          .as("10:30 + 1800 s = 11:00, die Anwendungsuhr steht auf 12:00")
          .isTrue();
      assertThat(detail.fristSekunden()).isEqualTo(1800);
    }

    @Test
    @DisplayName("Innerhalb der Frist ist nichts ueberfaellig")
    void innerhalb_der_frist_nicht_ueberfaellig() {
      NachrichtKopfZeile kopf =
          new NachrichtKopfZeile(
              MESSAGE_ID,
              "SUSPENDED",
              JETZT.minusMinutes(1),
              (short) 1800,
              "prozess-1",
              null,
              null,
              null,
              ABLAUF,
              (short) 1,
              null,
              0,
              0,
              null,
              null,
              null,
              null);
      gib(kopf, List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).ueberfaellig()).isFalse();
    }

    /**
     * {@code UNGEKLAERT} liefert {@code istEndstatus == true} — in dieser Rechnung die vorsichtige
     * Antwort: keine Behauptung, die Nachricht haenge. Genau dafuer ist die Methode da, und fuer
     * sonst nichts ({@code message-status.md}).
     */
    @Test
    @DisplayName("UNGEKLAERT wird nicht ueberfaellig — die vorsichtige Antwort")
    void ungeklaert_wird_nicht_ueberfaellig() {
      gib(kopf("COMMIT_SENT"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).ueberfaellig()).isFalse();
    }

    @Test
    @DisplayName("Ohne Frist gibt es keine Frist und keine Ueberfaelligkeit — keine erfundene Null")
    void ohne_frist_keine_ueberfaelligkeit() {
      for (Short timeout : new Short[] {null, 0}) {
        NachrichtKopfZeile kopf =
            new NachrichtKopfZeile(
                MESSAGE_ID,
                "SUSPENDED",
                T2,
                timeout,
                "prozess-1",
                null,
                null,
                null,
                ABLAUF,
                (short) 1,
                null,
                0,
                0,
                null,
                null,
                null,
                null);
        gib(kopf, List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

        NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

        assertThat(detail.fristSekunden()).as("MessageTimeout = %s", timeout).isNull();
        assertThat(detail.ueberfaellig()).as("MessageTimeout = %s", timeout).isFalse();
      }
    }

    @Test
    @DisplayName("Die Gesamtdauer laeuft vom fachlichen Start bis MessageLastUpdate")
    void gesamtdauer_vom_fachlichen_start() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T1, T2)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).gesamtdauerSekunden())
          .as("T0 = 10:00:00 (Metadaten-Schritt) bis T2 = 10:30:00")
          .isEqualTo(1800L);
    }

    @Test
    @DisplayName("Ohne Aktion gibt es keine Gesamtdauer")
    void ohne_aktion_keine_gesamtdauer() {
      gib(kopf("FINISHED"), List.of());

      assertThat(service().detail(MANDANT, MESSAGE_ID).gesamtdauerSekunden()).isNull();
    }
  }

  @Nested
  @DisplayName("Die kuratierten Eigenschaften")
  class Kuratiert {

    @Test
    @DisplayName("Leere Werte werden nicht geliefert")
    void leere_werte_fallen_heraus() {
      gib(
          kopf("FINISHED"),
          List.of(metadatenSchritt()),
          List.of(),
          List.of(
              new MessageEigenschaft("Message.SendingPartner", "  ", (short) 0, 2),
              new MessageEigenschaft("Message.SplitCount", "27", (short) 0, 2)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).kuratierteEigenschaften())
          .extracting(KuratierteEigenschaftResponse::name)
          .containsExactly("Message.SplitCount");
    }

    @Test
    @DisplayName("Der Rang gibt die Reihenfolge vor, nicht die Reihenfolge der Zeilen")
    void rang_gibt_die_reihenfolge_vor() {
      gib(
          kopf("FINISHED"),
          List.of(metadatenSchritt()),
          List.of(),
          List.of(
              new MessageEigenschaft("Message.SplitCount", "27", (short) 0, 2),
              new MessageEigenschaft("Message.SendingPartner", "PARTNER", (short) 0, 7)));

      assertThat(service().detail(MANDANT, MESSAGE_ID).kuratierteEigenschaften())
          .extracting(KuratierteEigenschaftResponse::name, KuratierteEigenschaftResponse::rang)
          .containsExactly(
              org.assertj.core.api.Assertions.tuple("Message.SendingPartner", 1),
              org.assertj.core.api.Assertions.tuple("Message.SplitCount", 2));
    }

    @Test
    @DisplayName("Die Anzahl aller Eigenschaften steht im Kopf, auch ohne kuratierte")
    void anzahl_steht_im_kopf() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt()));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.eigenschaftenAnzahl()).isEqualTo(22);
      assertThat(detail.kuratierteEigenschaften()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Die Kappung der Eigenschaftswerte")
  class Kappung {

    private static final int GRENZE = NachrichtendetailRepository.WERT_GRENZE_BYTES;

    @Test
    @DisplayName("Ein kurzer Wert bleibt unangetastet und traegt kein Kennzeichen")
    void kurzer_wert_bleibt() {
      EigenschaftResponse antwort =
          NachrichtendetailService.gekappt(
              new MessageEigenschaft("Message.GUID", "kurz", (short) 0, 4));

      assertThat(antwort.wert()).isEqualTo("kurz");
      assertThat(antwort.gekappt()).isFalse();
      assertThat(antwort.originalLaengeBytes()).isNull();
    }

    @Test
    @DisplayName("Ein zu langer Wert wird gekappt und traegt seine urspruengliche Laenge")
    void langer_wert_wird_gekappt() {
      String lang = "a".repeat(GRENZE + 500);

      EigenschaftResponse antwort =
          NachrichtendetailService.gekappt(
              new MessageEigenschaft("Message.IDOCNr", lang, (short) 0, lang.length()));

      assertThat(antwort.wert().getBytes(StandardCharsets.UTF_8)).hasSize(GRENZE);
      assertThat(antwort.gekappt()).isTrue();
      assertThat(antwort.originalLaengeBytes()).isEqualTo(GRENZE + 500);
    }

    @Test
    @DisplayName("Gekappt wird auf einer Zeichengrenze, nie mitten in ein Zeichen hinein")
    void kappung_auf_zeichengrenze() {
      // Ein Fuellwert, der genau ein Byte vor der Grenze endet, danach ein Zwei-Byte-Zeichen:
      // Der Schnitt faellt damit zwingend mitten in die UTF-8-Folge.
      String wert = "a".repeat(GRENZE - 1) + "ü" + "b".repeat(10);
      int laenge = wert.getBytes(StandardCharsets.UTF_8).length;

      EigenschaftResponse antwort =
          NachrichtendetailService.gekappt(
              new MessageEigenschaft("Message.Filename", wert, (short) 0, laenge));

      assertThat(antwort.wert())
          .as("ein Byte-Schnitt haette hier ein Ersatzzeichen erzeugt, das im Wert nie stand")
          .isEqualTo("a".repeat(GRENZE - 1));
      assertThat(antwort.gekappt()).isTrue();
    }

    @Test
    @DisplayName("Der laengste in M17 gemessene Wert passt vollstaendig hinein")
    void groesster_gemessener_wert_passt() {
      String gemessenesMaximum = "x".repeat(12_732);

      EigenschaftResponse antwort =
          NachrichtendetailService.gekappt(
              new MessageEigenschaft("Message.IDOCNr", gemessenesMaximum, (short) 0, 12_732));

      assertThat(antwort.gekappt())
          .as("die Grenze bemisst sich am Gemessenen, kappt es aber nicht weg")
          .isFalse();
    }

    @Test
    @DisplayName("Ein Wert ohne Inhalt bleibt ohne Inhalt")
    void null_wert_bleibt_null() {
      EigenschaftResponse antwort =
          NachrichtendetailService.gekappt(new MessageEigenschaft("Message.X", null, (short) 3, 0));

      assertThat(antwort.wert()).isNull();
      assertThat(antwort.gekappt()).isFalse();
      assertThat(antwort.position()).isEqualTo(3);
    }
  }

  /**
   * Die Rollen im Kopf — <b>immer vorhanden, leer statt fehlend</b>.
   *
   * <p>Geprueft wird hier <b>nicht die Rechenregel</b>: Die steht in {@code common/Kettenrollen}
   * und hat mit {@code KettenrollenTest} einen eigenen, vollstaendigen Test samt allen sechs
   * Paaren. Hier steht die Frage davor: Holt der Service sie von dort, gibt er sie unveraendert
   * weiter, und ist das Feld auch dann da, wenn es nichts zu sagen hat.
   */
  @Nested
  @DisplayName("Die Rollen im Kopf")
  class Rollen {

    @Test
    @DisplayName("Ohne Verkettung ist die Liste leer und nicht null")
    void ohne_verkettung_leer_statt_fehlend() {
      gib(kopf("FINISHED"), List.of(metadatenSchritt()));

      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen())
          .as("ein fehlendes Feld hiesse unbekannt, ein leeres heisst nicht in einer Kette")
          .isEmpty();
    }

    @Test
    @DisplayName("Ein Split-Kind traegt SPLIT_KIND, eine Wurzel SPLIT_WURZEL")
    void aufteilung() {
      gib(kopf("FINISHED", ABLAUF, (short) 1, null, "eltern-1", null, null), List.of());
      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen())
          .containsExactly(Kettenrolle.SPLIT_KIND);

      gib(kopf("SPLITTED", ABLAUF, (short) 1, true, null, null, null), List.of());
      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen())
          .containsExactly(Kettenrolle.SPLIT_WURZEL);
    }

    @Test
    @DisplayName("Ein Merge-Eingang traegt MERGE_EINGANG, ein Ergebnis MERGE_ERGEBNIS")
    void zusammenfuehrung() {
      gib(kopf("MERGED", ABLAUF, (short) 1, null, null, "ergebnis-1", null), List.of());
      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen())
          .containsExactly(Kettenrolle.MERGE_EINGANG);

      gib(kopf("EERP_RECEIVED", ABLAUF, (short) 1, null, null, null, true), List.of());
      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen())
          .containsExactly(Kettenrolle.MERGE_ERGEBNIS);
    }

    /**
     * 514 von 214.330 Zeilen tragen zwei Rollen (M28‑1c) — selten, aber nicht nie. Die Reihenfolge
     * ist die Deklarationsreihenfolge von {@link Kettenrolle} und haengt nicht daran, in welcher
     * Reihenfolge die Spalten gelesen wurden.
     */
    @Test
    @DisplayName("Eine Doppelrolle kommt vollstaendig durch, in Deklarationsreihenfolge")
    void doppelrolle_in_deklarationsreihenfolge() {
      gib(kopf("SPLITTED", ABLAUF, (short) 1, true, "eltern-1", null, null), List.of());

      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen())
          .containsExactly(Kettenrolle.SPLIT_WURZEL, Kettenrolle.SPLIT_KIND);
    }

    /**
     * Der Kopf liest {@code Source} und {@code Target} roh; beide Spalten sind {@code NULL}-faehig
     * mit Vorgabe {@code b'0'} (M23‑1). Ein fehlender Wert ist keine Behauptung, es gebe Kinder —
     * und eine leere Kennung ist keine Verkettung.
     */
    @Test
    @DisplayName("Ein null-Flag und eine leere Kennung zaehlen nicht als Rolle")
    void null_flag_und_leere_kennung() {
      gib(kopf("FINISHED", ABLAUF, (short) 1, null, "  ", "", null), List.of());

      assertThat(service().detail(MANDANT, MESSAGE_ID).rollen()).isEmpty();
    }
  }

  @Test
  @DisplayName("Zeitpunkte kommen als UTC, umgerechnet aus der Wanduhrzeit der Quelle")
  void zeitpunkte_kommen_als_utc() {
    gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T1, T2)));

    NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

    assertThat(detail.zeitpunkt()).isEqualTo(Instant.parse("2025-12-29T09:30:00Z"));
    assertThat(detail.schritte().getFirst().start()).isEqualTo(T1.atZone(ZONE).toInstant());
    assertThat(detail.fristSekunden()).isEqualTo(1800);
  }
}
