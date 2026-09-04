package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
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
 *
 * <p>Die Tests zum Zwischenspeicher der Merkmale sind mit Schritt 6, Teil 2a entfallen: Der
 * Endpunkt {@code /api/nachrichten/merkmale} existiert nicht mehr, und mit ihm nicht der Speicher,
 * sein Rueckfall und die zweite Uhr, gegen die seine Haltbarkeit rechnete (docs/nachrichtenliste.md
 * §5).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NachrichtenServiceTest {

  private static final MandantContext MANDANT = new MandantContext("NEXANS");
  private static final LocalDateTime ZEITPUNKT = LocalDateTime.parse("2025-12-29T23:53:50");

  /** Der einzige Schritt, auf dem in der Testkopie eine offene Nachricht steht (M13). */
  private static final String SCHRITT = "Send Message to Pool";

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
        10,
        Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), ZoneOffset.UTC));
  }

  private static NachrichtZeile zeile(String id) {
    return zeile(id, "FINISHED", "Versand Einzel IDOC aus Split");
  }

  private static NachrichtZeile zeile(String id, String status, String sosName) {
    return zeile(id, status, sosName, SCHRITT);
  }

  /**
   * Der Schritt kommt aus der Quelle <b>ungefiltert</b> — auch bei abgeschlossenen Nachrichten
   * steht dort ein Wert. Ob er in die Antwort gehoert, entscheidet der Service.
   */
  private static NachrichtZeile zeile(String id, String status, String sosName, String schritt) {
    return new NachrichtZeile(
        id,
        ZEITPUNKT,
        status,
        "prozess-1",
        "40000_AMG_LAB_VDA",
        "300_KundenEingehend",
        sosName,
        schritt);
  }

  /**
   * Die Fensterverengung <b>abgeschaltet</b>. Dieser Test prueft die Uebersetzung des Service,
   * nicht die Verengung; abgeschaltet reicht sie die Abfrage unveraendert durch und fasst keine
   * Datenbank an. Was die Verengung selbst tut, pruefen {@code FensterverengungMerkmaleTest},
   * {@code FensterverengungGrenzenTest} und {@code FensterverengungDbIT}.
   */
  private static Fensterverengung verengungAus() {
    return new Fensterverengung(null, new NachrichtenlisteEigenschaften(false));
  }

  /** Die Zone der Anwendungsuhr ist die eine Stelle, an der die Wanduhrzeit nach UTC kommt. */
  private NachrichtenService service(ZoneId zone) {
    return new NachrichtenService(
        nachrichtenRepository,
        statusClassifier,
        verengungAus(),
        Clock.fixed(Instant.parse("2025-12-30T04:09:47Z"), zone));
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

  // ─── Der aktuelle Schritt (Nachbesserung zu Schritt 4, Aufgabe 6) ──────────────

  /**
   * Der Kern der Entscheidung: {@code SOSActionID} ist auf <b>jeder</b> Zeile gesetzt (M13), auch
   * auf abgeschlossenen — dort benennt sie den letzten Schritt und nicht den aktuellen. Als Feld
   * „aktueller Schritt" waere sie auf 99 Prozent der Zeilen eine falsche Auskunft.
   */
  @Test
  @DisplayName("Der Schritt kommt nur bei offenen Nachrichten mit")
  void schritt_nur_bei_offenen_nachrichten() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(
            List.of(
                zeile("wartend", "SUSPENDED", "Merge files and wait"),
                zeile("laeuft", "RUNNING", "Format Conversion"),
                zeile("fertig", "FINISHED", "Send Message to Partner"),
                zeile("quittiert", "EERP_RECEIVED", "Send Message to Partner"),
                zeile("fehler", "ERROR_DUPLICATE", "Format Conversion"),
                zeile("zwischen", "SPLITTED", "Split Multiple IDOC"),
                zeile("ungeklaert", "CHECKED", "Format Conversion")));

    List<NachrichtResponse> zeilen = service().liste(MANDANT, filter(null)).items();

    assertThat(zeilen)
        .filteredOn(z -> z.schritt() != null)
        .extracting(NachrichtResponse::messageId)
        .as("nur WARTEND und LAEUFT")
        .containsExactly("wartend", "laeuft");
    assertThat(zeilen.getFirst().schritt()).isEqualTo(SCHRITT);
  }

  /**
   * {@code UNGEKLAERT} ist der Fall, der die ausdrueckliche Aufzaehlung rechtfertigt: {@code
   * istEndstatus} liefert dort {@code true} — in der Ueberfaelligkeitsrechnung die vorsichtige
   * Antwort, in einem Sichtbarkeitsfilter die unvorsichtige ({@code message-status.md}). Der
   * Schritt erscheint dort nicht, weil ueber diese Zeilen nichts bekannt ist.
   */
  @Test
  @DisplayName("Ein ungeklaerter Status bekommt keinen Schritt")
  void ungeklaerter_status_bekommt_keinen_schritt() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(List.of(zeile("m1", "COMMIT_SENT", "Format Conversion")));

    assertThat(service().liste(MANDANT, filter(null)).items().getFirst().schritt()).isNull();
  }

  /**
   * 43,9 Prozent aller Verweise laufen ins Leere (M13). Bei den offenen Status tut das in der
   * Testkopie keiner — die Produktion muss sich daran aber nicht halten.
   */
  @Test
  @DisplayName("Auch bei einer offenen Nachricht darf der Schritt fehlen")
  void offene_nachricht_ohne_aufloesbaren_schritt() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(List.of(zeile("m1", "SUSPENDED", "Merge files and wait", null)));

    NachrichtResponse zeile = service().liste(MANDANT, filter(null)).items().getFirst();

    assertThat(zeile.statusKind()).isEqualTo("WARTEND");
    assertThat(zeile.schritt()).isNull();
  }

  // ─── Die beiden Statuswerte, die ZWISCHENSCHRITT abgeloest haben (Schritt 6, Teil 2a) ──

  /**
   * Aus einem Wert wurden zwei, und sie sind nicht dasselbe: <i>aus eins wurde viel</i> gegen
   * <i>aus viel wurde eins</i>. Die Antwort traegt die Einordnung, an der die Oberflaeche ihre
   * Beschriftung festmacht — ein gemeinsamer Eimer verschluckte genau diesen Unterschied.
   */
  @Test
  @DisplayName("SPLITTED und MERGED kommen als zwei verschiedene Einordnungen heraus")
  void aufgeteilt_und_zusammengefuehrt_sind_verschieden() {
    when(nachrichtenRepository.finde(any(), any()))
        .thenReturn(
            List.of(
                zeile("split", "SPLITTED", "Split Multiple IDOC"),
                zeile("merge", "MERGED", "Merge files and wait")));

    List<NachrichtResponse> zeilen = service().liste(MANDANT, filter(null)).items();

    assertThat(zeilen.getFirst().statusKind()).isEqualTo("AUFGETEILT");
    assertThat(zeilen.getFirst().status()).isEqualTo("SPLITTED");
    assertThat(zeilen.get(1).statusKind()).isEqualTo("ZUSAMMENGEFUEHRT");
    assertThat(zeilen.get(1).status()).isEqualTo("MERGED");
    assertThat(zeilen)
        .as("Keiner der beiden ist ungeklaert — die Einordnung ist gesichert, nur geteilt")
        .noneMatch(NachrichtResponse::bedeutungNichtVerifiziert);
  }
}
