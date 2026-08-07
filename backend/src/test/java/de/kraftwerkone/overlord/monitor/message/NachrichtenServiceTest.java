package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Was der Service aus den Rohzeilen macht: Einordnung, Umrechnung der Wanduhrzeit nach UTC und der
 * Schnitt der Seite. Ohne Datenbank — die Statements pruefen die Integrationstests und die
 * Messungen.
 *
 * <p>Die Tests zur Gruppierung der BAM-Werte sind mit der Nachbesserung zu Schritt 4 entfallen: Die
 * Liste laedt keine BAM-Werte mehr nach (Messung M11). {@code common/BamSpaltenRegel} behaelt
 * seinen eigenen Test — die Regel bleibt, Schritt 7 braucht sie.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NachrichtenServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final LocalDateTime ZEITPUNKT = LocalDateTime.parse("2025-12-29T23:53:50");

  /** Die echte Uhr — bewusst weit weg von der Anwendungsuhr, damit eine Verwechslung auffiele. */
  private static final Instant SYSTEMZEIT = Instant.parse("2026-08-07T08:00:00Z");

  @Mock private NachrichtenRepository nachrichtenRepository;

  /** Kein Mock: Die Einordnung ist Fachlogik und soll hier mitlaufen, nicht wegdefiniert werden. */
  private final MessageStatusClassifier statusClassifier = new MessageStatusClassifier();

  private static NachrichtenFilter filter(String suche) {
    return NachrichtenFilter.aus(
        null,
        null,
        null,
        null,
        null,
        suche,
        null,
        null,
        null,
        null,
        10,
        Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), ZoneOffset.UTC));
  }

  private static NachrichtZeile zeile(String id) {
    return zeile(id, "FINISHED", "Versand Einzel IDOC aus Split");
  }

  private static NachrichtZeile zeile(String id, String status, String sosName) {
    return new NachrichtZeile(
        id, ZEITPUNKT, status, "prozess-1", "40000_AMG_LAB_VDA", "300_KundenEingehend", sosName);
  }

  /** Die Zone der Anwendungsuhr ist die eine Stelle, an der die Wanduhrzeit nach UTC kommt. */
  private NachrichtenService service(ZoneId zone) {
    return service(zone, Clock.fixed(SYSTEMZEIT, ZoneOffset.UTC));
  }

  /**
   * Die zweite Uhr ist die <b>System</b>uhr. Sie datiert den Zwischenspeicher der Merkmale — nicht
   * die Anwendungsuhr, die im Profil {@code dev} um Monate zurueckversetzt steht und deren
   * Haltbarkeit deshalb nie abliefe.
   */
  private NachrichtenService service(ZoneId zone, Clock systemuhr) {
    return new NachrichtenService(
        nachrichtenRepository,
        statusClassifier,
        Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), zone),
        systemuhr);
  }

  private NachrichtenService service() {
    return service(ZoneOffset.UTC);
  }

  @Test
  @DisplayName("Der Ablaufname wird durchgereicht — er ist die Spalte, die auf jeder Zeile traegt")
  void ablaufname_wird_durchgereicht() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of(zeile("m1")));

    NachrichtResponse zeile = service().liste(MANDANT, filter(null)).items().getFirst();

    assertThat(zeile.sosName()).isEqualTo("Versand Einzel IDOC aus Split");
    assertThat(zeile.processName())
        .as("ProcessName bleibt in der Antwort — er steht in der Oberflaeche im Tooltip")
        .isEqualTo("40000_AMG_LAB_VDA");
  }

  /**
   * L14 weist den Ablaufnamen als durchgaengig gepflegt aus, und trotzdem bleibt die Spalte
   * nullable — die Produktion muss sich nicht daran halten, was die Testkopie zufaellig enthaelt.
   * Den Ersatztext waehlt die Oberflaeche (Regel Q4), nicht diese Schicht.
   */
  @Test
  @DisplayName("Ein fehlender Ablaufname bleibt null und wird nicht ersetzt")
  void fehlender_ablaufname_bleibt_null() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(List.of(zeile("m1", "FINISHED", null)));

    assertThat(service().liste(MANDANT, filter(null)).items().getFirst().sosName()).isNull();
  }

  @Test
  @DisplayName("Die Einordnung entsteht im Classifier, der Rohwert bleibt daneben stehen")
  void einordnung_und_rohwert() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(List.of(zeile("m1"), zeile("m2", "CHECKED", "Format Conversion"), zeile("m3")));

    List<NachrichtResponse> zeilen = service().liste(MANDANT, filter(null)).items();

    assertThat(zeilen.getFirst().statusKind()).isEqualTo("ABGESCHLOSSEN");
    assertThat(zeilen.getFirst().status()).isEqualTo("FINISHED");
    assertThat(zeilen.getFirst().bedeutungNichtVerifiziert()).isFalse();

    assertThat(zeilen.get(1).statusKind()).isEqualTo("UNGEKLAERT");
    assertThat(zeilen.get(1).status()).as("der Rohwert traegt die Plakette").isEqualTo("CHECKED");
    assertThat(zeilen.get(1).bedeutungNichtVerifiziert()).isTrue();
  }

  /**
   * Dieselbe Kette wie in {@code ZeitpunkteTest} und {@code tests/format.test.ts}: Aus 23:53:50
   * Wanduhrzeit wird in {@code Europe/Berlin} 22:53:50Z auf der Leitung — und in der Anzeige wieder
   * 23:53:50.
   */
  @Test
  @DisplayName("Die Wanduhrzeit wird mit der Zone der Anwendungsuhr nach UTC gerechnet")
  void wanduhrzeit_wird_nach_utc_gerechnet() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of(zeile("m1")));

    assertThat(service(ZoneId.of("Europe/Berlin")).liste(MANDANT, filter(null)).items().getFirst())
        .extracting(NachrichtResponse::zeitpunkt)
        .isEqualTo(Instant.parse("2025-12-29T22:53:50Z"));
  }

  /**
   * Die Zusatzzeile beantwortet „gibt es eine naechste Seite" ohne {@code COUNT} (Regel L2) — und
   * darf den Aufrufer nie erreichen.
   */
  @Test
  @DisplayName("Die Zusatzzeile wird abgeschnitten und beantwortet nur hasMore")
  void zusatzzeile_wird_abgeschnitten() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(
            List.of(
                zeile("m1"),
                zeile("m2"),
                zeile("m3"),
                zeile("m4"),
                zeile("m5"),
                zeile("m6"),
                zeile("m7"),
                zeile("m8"),
                zeile("m9"),
                zeile("m10"),
                zeile("m11")));

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter(null));

    assertThat(seite.items()).hasSize(10);
    assertThat(seite.items()).extracting(NachrichtResponse::messageId).doesNotContain("m11");
    assertThat(seite.hasMore()).isTrue();
    assertThat(seite.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("Eine leere Seite bleibt leer und traegt keinen Cursor")
  void leere_seite() {
    when(nachrichtenRepository.finde(any(), any())).thenReturn(List.of());

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter(null));

    assertThat(seite.items()).isEmpty();
    assertThat(seite.hasMore()).isFalse();
    assertThat(seite.nextCursor()).isNull();
  }

  /**
   * Trifft der Suchbegriff nichts in den Stammdaten, ist die Liste leer — <b>ohne dass {@code
   * Message} angefasst wird</b>. Das ist der Grund, warum die Aufloesung vorne steht.
   */
  @Test
  @DisplayName("Ein Suchbegriff ohne Treffer fasst Message gar nicht an")
  void suchbegriff_ohne_treffer_liest_keine_nachricht() {
    when(nachrichtenRepository.loeseSucheAuf(any(), any(), anyInt()))
        .thenReturn(new Suchtreffer(List.of(), List.of(), false));

    Seite<NachrichtResponse> seite = service().liste(MANDANT, filter("gibtesnicht"));

    assertThat(seite.items()).isEmpty();
    assertThat(seite.hasMore()).isFalse();
    verify(nachrichtenRepository, never()).finde(any(), any());
  }

  // ─── Die Merkmale des Bestands (Nachbesserung zu Schritt 4, Aufgabe 5) ──────────

  /**
   * Das Statement liest im schlimmsten Fall den ganzen Bestand eines Mandanten (L15, 331 ms). Es
   * darf deshalb nicht bei jedem Aufruf laufen — der Wert beschreibt den Gesamtbestand und aendert
   * sich, wenn ueberhaupt, genau einmal.
   */
  @Test
  @DisplayName("Die Merkmale werden je Mandant ein einziges Mal ermittelt")
  void merkmale_werden_zwischengespeichert() {
    when(nachrichtenRepository.hatZwischenschritte(MANDANT)).thenReturn(true);
    NachrichtenService service = service();

    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();
    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();
    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();

    verify(nachrichtenRepository, times(1)).hatZwischenschritte(MANDANT);
  }

  /**
   * Der Zwischenspeicher haengt am Mandanten. Waere er es nicht, saehe ein Nutzer die Auskunft
   * ueber einen fremden Bestand — bei einem Werkzeug, dessen Kernversprechen die Trennung ist,
   * waere das der peinlichste denkbare Fehler.
   */
  @Test
  @DisplayName("Jeder Mandant bekommt seine eigene Auskunft")
  void merkmale_haengen_am_mandanten() {
    MandantContext ohne = new MandantContext("IBIS");
    when(nachrichtenRepository.hatZwischenschritte(MANDANT)).thenReturn(true);
    when(nachrichtenRepository.hatZwischenschritte(ohne)).thenReturn(false);
    NachrichtenService service = service();

    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();
    assertThat(service.merkmale(ohne).zwischenschritteVorhanden()).isFalse();
    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();

    verify(nachrichtenRepository, times(1)).hatZwischenschritte(MANDANT);
    verify(nachrichtenRepository, times(1)).hatZwischenschritte(ohne);
  }

  /**
   * Die Haltbarkeit rechnet gegen die <b>System</b>uhr. Gegen die Anwendungsuhr gerechnet liefe sie
   * im Profil {@code dev} nie ab — sie steht dort Monate zurueck.
   */
  @Test
  @DisplayName("Nach Ablauf der Haltbarkeit wird neu ermittelt")
  void merkmale_laufen_ab() {
    when(nachrichtenRepository.hatZwischenschritte(MANDANT)).thenReturn(false, true);
    StellbareUhr uhr = new StellbareUhr(SYSTEMZEIT);
    NachrichtenService service = service(ZoneOffset.UTC, uhr);

    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isFalse();
    uhr.weiter(Duration.ofMinutes(59));
    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden())
        .as("innerhalb der Haltbarkeit steht der alte Wert")
        .isFalse();
    verify(nachrichtenRepository, times(1)).hatZwischenschritte(MANDANT);

    uhr.weiter(Duration.ofMinutes(2));
    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden())
        .as("danach wird neu gefragt — der erste Zwischenschritt eines Mandanten wird sichtbar")
        .isTrue();
    verify(nachrichtenRepository, times(2)).hatZwischenschritte(MANDANT);
  }

  /** Eine Uhr, die sich stellen laesst — {@link Clock#fixed} steht, und Ablauf braucht Bewegung. */
  private static final class StellbareUhr extends Clock {
    private Instant jetzt;

    private StellbareUhr(Instant start) {
      this.jetzt = start;
    }

    private void weiter(Duration dauer) {
      jetzt = jetzt.plus(dauer);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return jetzt;
    }
  }

  /**
   * Ein Fehler beim Ermitteln eines <b>Anzeigehinweises</b> darf die Liste nicht mitreissen.
   * Gezeigt wird dann der Schalter — der Zustand, den die Liste bis zur Nachbesserung fuer alle
   * hatte, also keine Verschlechterung.
   */
  @Test
  @DisplayName("Scheitert die Ermittlung, bleibt der Schalter sichtbar")
  void merkmale_fallen_sicher_zurueck() {
    when(nachrichtenRepository.hatZwischenschritte(MANDANT))
        .thenThrow(new DataAccessException("Zeitgrenze"));
    NachrichtenService service = service();

    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();

    // Auch der Rueckfall wird gehalten: Sonst waere der Schutzmechanismus die Last.
    assertThat(service.merkmale(MANDANT).zwischenschritteVorhanden()).isTrue();
    verify(nachrichtenRepository, times(1)).hatZwischenschritte(MANDANT);
  }
}
