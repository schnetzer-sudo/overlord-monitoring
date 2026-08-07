package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

  /** Dieselbe Zone wie die Anwendungsuhr — die Umrechnung nach UTC haengt daran. */
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  private static final LocalDateTime T0 = LocalDateTime.parse("2025-12-29T10:00:00");
  private static final LocalDateTime T1 = LocalDateTime.parse("2025-12-29T10:00:30");
  private static final LocalDateTime T2 = LocalDateTime.parse("2025-12-29T10:30:00");

  @Mock private NachrichtendetailRepository detailRepository;

  /** Kein Mock: Die Einordnung ist Fachlogik und soll hier mitlaufen. */
  private final MessageStatusClassifier statusClassifier = new MessageStatusClassifier();

  private NachrichtendetailService service() {
    return new NachrichtendetailService(detailRepository, statusClassifier, Clock.system(ZONE));
  }

  private static NachrichtKopfZeile kopf(String status) {
    return new NachrichtKopfZeile(
        MESSAGE_ID,
        status,
        LocalDateTime.parse("2025-12-29T10:30:00"),
        (short) 1800,
        "prozess-1",
        "40000_AMG_LAB_VDA",
        "300_KundenEingehend",
        "Versand Einzel IDOC aus Split",
        "Send Message to Pool",
        22);
  }

  /** Der Metadaten-Schritt: {@code SOSActionID = 0}, keine Bausteine (S1). */
  private static MessageAktion metadatenSchritt() {
    return new MessageAktion((short) 0, ABLAUF, (short) 0, T0, T0, null, (short) 0);
  }

  private static MessageAktion aktion(
      short position, short sosActionId, LocalDateTime start, LocalDateTime ende) {
    return new MessageAktion(
        position, ABLAUF, sosActionId, start, ende, "NXS_FILE_CONVERT|E2A", (short) 1800);
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

    @Test
    @DisplayName("WARTET_VOR: offen, aber jede Aktion beendet — der gemessene Normalfall")
    void wartet_vor() {
      gib(kopf("SUSPENDED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T0, T1)));

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand()).isEqualTo(OffenerZustand.WARTET_VOR);
      assertThat(detail.naechsterSchritt()).isEqualTo("Send Message to Pool");
      assertThat(detail.schritte()).allMatch(schritt -> !schritt.laeuftAuf());
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

    @Test
    @DisplayName("OHNE_SCHRITT: offen, aber gar keine Aktion")
    void ohne_schritt() {
      gib(kopf("SUSPENDED"), List.of());

      NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

      assertThat(detail.offenerZustand())
          .as(
              "ueber einer leeren Menge waere „jede Aktion ist beendet\" wahr — der Fall muss vorher"
                  + " abgefangen werden")
          .isEqualTo(OffenerZustand.OHNE_SCHRITT);
      assertThat(detail.naechsterSchritt()).isNull();
      assertThat(detail.start()).isNull();
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

  @Test
  @DisplayName("Zeitpunkte kommen als UTC, umgerechnet aus der Wanduhrzeit der Quelle")
  void zeitpunkte_kommen_als_utc() {
    gib(kopf("FINISHED"), List.of(metadatenSchritt(), aktion((short) 1, (short) 1, T1, T2)));

    NachrichtendetailResponse detail = service().detail(MANDANT, MESSAGE_ID);

    assertThat(detail.zeitpunkt()).isEqualTo(Instant.parse("2025-12-29T09:30:00Z"));
    assertThat(detail.schritte().getFirst().start()).isEqualTo(T1.atZone(ZONE).toInstant());
    assertThat(detail.timeoutSekunden()).isEqualTo(1800);
  }
}
