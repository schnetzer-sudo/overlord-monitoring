package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Der Pflicht-Isolationstest des Listen-Endpunkts</b> (Regel M4). Kopiert das Muster aus {@code
 * MandantenIsolationDbIT} und tauscht Aufruf und Kennung.
 *
 * <p><b>Warum {@code NEXANS} gegen {@code SUTTONS}:</b> Ueber den Gesamtbestand liegt {@code
 * SUTTONS} mit 197.158 Zeilen vor {@code VOTG} mit 145.840 (Messung M3). {@code NXHBE} und {@code
 * IBISGUS} scheiden aus — zwei Mandanten desselben Hauses sind ein schlechter Beweis fuer eine
 * Trennung, die zwischen Firmen greifen soll; {@code NXHBE} hat ohnehin neun Nachrichten.
 *
 * <p><b>Warum das Zeitfenster absolut ist:</b> Ausser {@code NEXANS} endet jeder Mandant am
 * 30.12.2025 (M3, „Zeitspanne je Mandant"). In einem relativen Fenster saehe {@code SUTTONS} je
 * nach Datenstand null Zeilen — und der Test bewiese nur, dass leer leer ist. Deshalb ein Fenster
 * um den 29.12.2025, in dem beide Mandanten Daten haben; der erste Test haelt genau das fest, damit
 * ein spaeterer Datenstand die Aussagekraft nicht stillschweigend verliert.
 *
 * <p><b>Ohne Statusfilter</b>, seit dem Wegfall des Ausblende-Schalters (11.08.2026): Die Liste
 * zeigt jede Zeile des Fensters. Bis dahin stand hier ein {@code zwischenschritte=true}, damit die
 * Zeilenzahl nicht an der Statusverteilung des Fensters haengt und der Test etwas anderes misst als
 * die Trennung. Das ist jetzt der Normalzustand.
 */
class NachrichtenIsolationDbIT extends SicherheitsTestbasis {

  private static final String MANDANT_NEXANS = "NEXANS";
  private static final String MANDANT_SUTTONS = "SUTTONS";

  private static final String NUTZER_A = PRAEFIX + "isolation-nexans";
  private static final String NUTZER_B = PRAEFIX + "isolation-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine ProcessID, die es garantiert nicht gibt — die Gegenprobe zum fremden, echten Prozess. */
  private static final String ERFUNDEN = "GIBTESGARANTIERTNICHT";

  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  private Sitzung aufNexans;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_SUTTONS)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_SUTTONS);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String abfrage(String zusatz) {
    return "/api/nachrichten?limit=200&von="
        + URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8)
        + zusatz;
  }

  /**
   * Die Voraussetzung, ohne die alles Folgende wertlos waere: In diesem Fenster haben <b>beide</b>
   * Mandanten Daten. Ein Isolationstest gegen einen leeren Ausschnitt beweist nichts.
   */
  @Test
  @DisplayName("Beide Mandanten haben Daten im gewaehlten Fenster")
  void beide_mandanten_haben_daten() throws Exception {
    assertThat(aufNexans.hole(abfrage("")).<List<String>>json("$.items[*].messageId")).isNotEmpty();
    assertThat(aufSuttons.hole(abfrage("")).<List<String>>json("$.items[*].messageId"))
        .isNotEmpty();
  }

  @Test
  @DisplayName("Der Nutzer auf NEXANS bekommt keine einzige Zeile von SUTTONS")
  void keine_fremde_zeile_in_der_liste() throws Exception {
    Antwort vonSuttons = aufSuttons.hole(abfrage(""));
    List<String> fremdeNachrichten = vonSuttons.json("$.items[*].messageId");
    List<String> fremdeProzesse = vonSuttons.json("$.items[*].processId");

    Antwort vonNexans = aufNexans.hole(abfrage(""));

    assertThat(vonNexans.<List<String>>json("$.items[*].messageId"))
        .isNotEmpty()
        .doesNotContainAnyElementsOf(fremdeNachrichten);
    assertThat(vonNexans.<List<String>>json("$.items[*].processId"))
        .doesNotContainAnyElementsOf(fremdeProzesse);
    for (String fremd : fremdeNachrichten) {
      assertThat(vonNexans.rumpf()).doesNotContain(fremd);
    }
  }

  /**
   * Der Kern des Musters: Ein fremder, <b>existierender</b> Prozess und eine <b>erfundene</b>
   * Kennung muessen eine ununterscheidbare Antwort liefern. Waeren sie zu unterscheiden, liesse
   * sich ueber den Filter die Prozesslandschaft fremder Mandanten abfragen.
   */
  @Test
  @DisplayName("Ein fremder Prozess ist von einer erfundenen Kennung nicht zu unterscheiden")
  void fremder_prozess_und_erfundene_kennung_sind_ununterscheidbar() throws Exception {
    String fremderProzess =
        aufSuttons.hole(abfrage("")).<List<String>>json("$.items[*].processId").getFirst();

    Antwort fremdAberEcht =
        aufNexans.hole(
            abfrage("&prozess=" + URLEncoder.encode(fremderProzess, StandardCharsets.UTF_8)));
    Antwort erfunden = aufNexans.hole(abfrage("&prozess=" + ERFUNDEN));

    assertThat(fremdAberEcht.status()).as("Niemals 403 — das verriete die Existenz").isEqualTo(200);
    assertThat(fremdAberEcht.<List<String>>json("$.items[*].messageId")).isEmpty();
    assertThat(erfunden.status()).isEqualTo(200);
    assertThat(fremdAberEcht.rumpfOhneTraceId())
        .as(
            "Unterschieden sich die beiden Antworten, liesse sich ueber den Prozessfilter die"
                + " Prozesslandschaft fremder Mandanten abfragen")
        .isEqualTo(erfunden.rumpfOhneTraceId());
  }

  /**
   * Die Mandantentrennung gilt auch quer (Regel M5): Der Freitextfilter loest Prozess-, Projekt-
   * und Ablaufnamen zu Kennungen auf — auch diese Vorfilterung traegt den Mandantenfilter.
   */
  @Test
  @DisplayName("Der Freitextfilter findet keine fremden Prozesse")
  void freitext_findet_keine_fremden_prozesse() throws Exception {
    String fremderProzessname =
        aufSuttons.hole(abfrage("")).<List<String>>json("$.items[*].processName").getFirst();

    Antwort antwort =
        aufNexans.hole(
            abfrage("&suche=" + URLEncoder.encode(fremderProzessname, StandardCharsets.UTF_8)));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].messageId")).isEmpty();
    assertThat(antwort.rumpf()).doesNotContain(fremderProzessname);
  }

  /**
   * Der Cursor traegt keine Berechtigung. Wer den Cursor eines fremden Mandanten einsetzt,
   * blaettert damit in <b>seinem eigenen</b> Ausschnitt weiter — die fremde Position ist nur ein
   * Zeitpunkt und eine Kennung, kein Zugang.
   */
  @Test
  @DisplayName("Ein fremder Cursor oeffnet keinen fremden Ausschnitt")
  void fremder_cursor_oeffnet_nichts() throws Exception {
    Antwort seiteVonSuttons = aufSuttons.hole(abfrage("&limit=5"));
    String fremderCursor = seiteVonSuttons.json("$.nextCursor");
    List<String> fremdeNachrichten = seiteVonSuttons.json("$.items[*].messageId");

    Antwort antwort =
        aufNexans.hole(
            abfrage("&cursor=" + URLEncoder.encode(fremderCursor, StandardCharsets.UTF_8)));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].messageId"))
        .doesNotContainAnyElementsOf(fremdeNachrichten);
  }

  /**
   * Und die Gegenrichtung — sonst bewiese der Test nur, dass {@code NEXANS} alles sieht: Der Nutzer
   * auf {@code SUTTONS} bekommt ebenso wenig eine Zeile von {@code NEXANS}.
   */
  @Test
  @DisplayName("Die Trennung gilt in beide Richtungen")
  void trennung_gilt_in_beide_richtungen() throws Exception {
    List<String> nexansProzesse = aufNexans.hole(abfrage("")).json("$.items[*].processId");

    Antwort vonSuttons = aufSuttons.hole(abfrage(""));

    assertThat(vonSuttons.<List<String>>json("$.items[*].processId"))
        .isNotEmpty()
        .doesNotContainAnyElementsOf(nexansProzesse);
  }

  /*
   * Hier standen bis zum 11.08.2026 die zwei Isolationstests von `GET /api/nachrichten/merkmale` —
   * der Endpunkt, dessen ganze Aufgabe war zu entscheiden, ob der Ausblende-Schalter erscheint. Er
   * ist mit dem Schalter entfallen (docs/nachrichtenliste.md §5), und mit ihm sein Pflichttest nach
   * Regel M4. **Kein anderer Isolationstest ist dabei angefasst worden**; die Liste behaelt ihren
   * vollstaendig, einschliesslich der Gegenprobe mit der erfundenen Kennung.
   */

  /**
   * <b>Der Isolationstest des Parameters {@code ueberfaellig}</b> (E-j, Schritt 10b-1). Regel M4
   * verlangt ihn fuer jeden Endpunktzustand, also auch fuer einen neuen Parameter am bestehenden
   * Endpunkt — er erzeugt eine <b>zweite Abfrageform</b> mit eigenem Plan ({@code
   * docs/nachrichtenliste.md} §5b), und ein zweiter Plan ist ein zweiter Ort, an dem der
   * Mandantenfilter fehlen kann.
   *
   * <p><b>Dieser Test ist schaerfer als der gewoehnliche, und der Grund liegt in den Daten:</b> Auf
   * der Testkopie sind <b>alle</b> ueberfaelligen Zeilen des Gesamtbestands {@code NEXANS}-Zeilen —
   * 538 Stueck, alle mit {@code SUSPENDED} und einer Frist von 1.800 Sekunden (M97). {@code
   * SUTTONS} hat keine einzige, in keinem Fenster. Faellt der Mandantenfilter aus dieser
   * Abfrageform heraus, sieht {@code SUTTONS} deshalb nicht ein paar fremde Zeilen, sondern
   * <b>genau die 538 von {@code NEXANS}</b>. Die erwartete Null ist hier also keine schwache
   * Zusage, sondern die schaerfste, die dieser Bestand hergibt.
   *
   * <p>Das Fenster ist deshalb der ganze Dezember 2025 und nicht der eine Tag der uebrigen Tests:
   * Im Tagesfenster traegt {@code NEXANS} genau <b>eine</b> ueberfaellige Zeile, im Dezember alle
   * 538. Ein Leck faellt bei 538 auf, bei einer nicht unbedingt.
   */
  @Test
  @DisplayName("ueberfaellig: SUTTONS sieht keine der 538 ueberfaelligen Zeilen von NEXANS")
  void ueberfaellig_zeigt_keine_fremden_zeilen() throws Exception {
    Antwort vonNexans = aufNexans.hole(dezemberAbfrage("&ueberfaellig=true"));
    List<String> ueberfaelligeVonNexans = vonNexans.json("$.items[*].messageId");

    assertThat(ueberfaelligeVonNexans)
        .as("Ohne Zeilen bei NEXANS bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();

    Antwort vonSuttons = aufSuttons.hole(dezemberAbfrage("&ueberfaellig=true"));

    assertThat(vonSuttons.status()).as("Niemals 403").isEqualTo(200);
    assertThat(vonSuttons.<List<String>>json("$.items[*].messageId")).isEmpty();
    for (String fremd : ueberfaelligeVonNexans) {
      assertThat(vonSuttons.rumpf()).doesNotContain(fremd);
    }
  }

  /**
   * Die Gegenprobe des Musters, auf die zweite Abfrageform uebertragen: Ein fremder,
   * <b>existierender</b> Prozess und eine <b>erfundene</b> Kennung muessen auch mit {@code
   * ueberfaellig=true} eine ununterscheidbare Antwort liefern.
   */
  @Test
  @DisplayName("ueberfaellig: fremder Prozess und erfundene Kennung sind ununterscheidbar")
  void ueberfaellig_fremder_prozess_und_erfundene_kennung() throws Exception {
    String fremderProzess =
        aufSuttons.hole(abfrage("")).<List<String>>json("$.items[*].processId").getFirst();

    Antwort fremdAberEcht =
        aufNexans.hole(
            dezemberAbfrage(
                "&ueberfaellig=true&prozess="
                    + URLEncoder.encode(fremderProzess, StandardCharsets.UTF_8)));
    Antwort erfunden = aufNexans.hole(dezemberAbfrage("&ueberfaellig=true&prozess=" + ERFUNDEN));

    assertThat(fremdAberEcht.status()).isEqualTo(200);
    assertThat(fremdAberEcht.<List<String>>json("$.items[*].messageId")).isEmpty();
    assertThat(fremdAberEcht.rumpfOhneTraceId()).isEqualTo(erfunden.rumpfOhneTraceId());
  }

  /**
   * Der Cursor der zweiten Abfrageform traegt so wenig Berechtigung wie der der ersten. Das ist
   * hier ausdruecklich zu pruefen, weil der Plan ihn <b>anders behandelt</b>: Er ist dort kein
   * Indexbereich mehr, sondern eine nachgelagerte Bedingung ({@code key_len} bleibt 123 statt auf
   * 151 zu steigen, M97). Was sich am Plan aendert, darf sich an der Trennung nicht aendern.
   */
  @Test
  @DisplayName("ueberfaellig: ein fremder Cursor oeffnet keinen fremden Ausschnitt")
  void ueberfaellig_fremder_cursor_oeffnet_nichts() throws Exception {
    Antwort seiteVonNexans = aufNexans.hole(dezemberAbfrage("&ueberfaellig=true&limit=5"));
    String cursorVonNexans = seiteVonNexans.json("$.nextCursor");
    List<String> zeilenVonNexans = seiteVonNexans.json("$.items[*].messageId");

    assertThat(cursorVonNexans).as("NEXANS muss hier mehr als fuenf Zeilen haben").isNotNull();

    Antwort antwort =
        aufSuttons.hole(
            dezemberAbfrage(
                "&ueberfaellig=true&cursor="
                    + URLEncoder.encode(cursorVonNexans, StandardCharsets.UTF_8)));

    assertThat(antwort.status()).isEqualTo(200);
    List<String> zeilenVonSuttons = antwort.json("$.items[*].messageId");
    assertThat(zeilenVonSuttons).doesNotContainAnyElementsOf(zeilenVonNexans).isEmpty();
  }

  /**
   * Das Fenster fuer die Ueberfaelligkeit: der ganze Dezember 2025. Es ist absolut und aus
   * demselben Grund wie {@link #FENSTER_VON} — ein relatives Fenster haenge am Datenstand.
   */
  private String dezemberAbfrage(String zusatz) {
    return "/api/nachrichten?limit=200&von="
        + URLEncoder.encode(iso(LocalDateTime.parse("2025-12-01T00:00:00")), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(LocalDateTime.parse("2025-12-31T00:00:00")), StandardCharsets.UTF_8)
        + zusatz;
  }
}
