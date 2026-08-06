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
 * <p><b>Warum {@code zwischenschritte=true}:</b> Sonst haengt die Zeilenzahl an der
 * Statusverteilung des Fensters, und der Test misst etwas anderes als die Trennung.
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
    return "/api/nachrichten?zwischenschritte=true&limit=200&von="
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
}
