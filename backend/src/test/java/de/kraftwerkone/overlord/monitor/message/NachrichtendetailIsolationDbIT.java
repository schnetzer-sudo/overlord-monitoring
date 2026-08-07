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
 * <b>Die beiden Pflicht-Isolationstests des Nachrichtendetails</b> (Regel M4) — einer je Endpunkt.
 * Ohne sie wird nicht gemergt.
 *
 * <p>Er kopiert das Muster aus {@code MandantenIsolationDbIT} und tauscht Aufruf und Kennung. Die
 * Paarung ist die der Vorlage: <b>{@code VOTG} gegen {@code SUTTONS}</b> — zwei Mandanten aus
 * <b>verschiedenen Haeusern</b>. {@code NXHBE} und {@code IBISGUS} scheiden aus; zwei Mandanten
 * desselben Konzerns sind ein schlechter Beweis fuer eine Trennung, die zwischen Firmen greifen
 * soll.
 *
 * <p><b>Der Kern ist nicht der Statuscode, sondern die Ununterscheidbarkeit.</b> Eine fremde,
 * <i>existierende</i> {@code MessageID} und eine <i>erfundene</i> muessen dieselbe Antwort liefern
 * — gleicher Status <b>und</b> gleicher Rumpf. Waeren sie zu unterscheiden, liesse sich mit einer
 * Kennung aus einer geteilten URL der fremde Bestand abfragen. Ein {@code 404} allein genuegt
 * deshalb nicht.
 *
 * <p><b>Das Zeitfenster ist absolut</b> (29.12.2025). Ausser {@code NEXANS} endet jeder Mandant am
 * 30.12.2025 (M3); in einem relativen Fenster saehe {@code SUTTONS} je nach Datenstand null Zeilen
 * — und der Test bewiese nur, dass leer leer ist.
 */
class NachrichtendetailIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "detail-votg";
  private static final String NUTZER_B = PRAEFIX + "detail-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine MessageID, die es garantiert nicht gibt — die Gegenprobe zur fremden, echten. */
  private static final String ERFUNDEN = "00000000-0000-0000-0000-gibtesnicht";

  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  private Sitzung aufVotg;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_A)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_A);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    aufVotg = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String liste() {
    return "/api/nachrichten?zwischenschritte=true&limit=5&von="
        + URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8);
  }

  /** Eine fremde Nachricht samt der Angaben, die im Rumpf einer 404 nichts zu suchen haben. */
  private record Fremd(String messageId, String sosName, String processId) {}

  private Fremd eineNachrichtVon(Sitzung sitzung) throws IOException, InterruptedException {
    Antwort liste = sitzung.hole(liste());
    List<String> kennungen = liste.json("$.items[*].messageId");
    assertThat(kennungen)
        .as("Ohne Daten im Fenster bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();
    List<String> ablaeufe = liste.json("$.items[*].sosName");
    List<String> prozesse = liste.json("$.items[*].processId");
    return new Fremd(kennungen.getFirst(), ablaeufe.getFirst(), prozesse.getFirst());
  }

  /**
   * Der Rumpf ohne die beiden Angaben, die je Anfrage verschieden sind — <b>und verschieden sein
   * muessen</b>.
   *
   * <p>{@code traceId} ist die Korrelations-ID, {@code instance} nach RFC 9457 der angefragte Pfad.
   * Weil die {@code MessageID} bei diesen beiden Endpunkten <i>im Pfad</i> steht, enthaelt {@code
   * instance} sie zwangslaeufig — es <b>spiegelt die Eingabe des Aufrufers zurueck</b> und sagt
   * nichts darueber, ob es die Nachricht gibt. Genau das ist der Unterschied, auf den es ankommt:
   * Verglichen wird, ob irgendein Teil der Antwort von der <i>Existenz</i> abhaengt. Dass zwei
   * verschiedene Fragen verschieden zitiert werden, ist keine Auskunft ueber den Bestand.
   *
   * <p>Damit die Spiegelung eine Spiegelung bleibt und keine Nachschlage-Auskunft wird, prueft
   * {@link #instanzSpiegeltNurDieAnfrage} sie zusaetzlich Zeichen fuer Zeichen gegen den gesendeten
   * Pfad.
   */
  private static String vergleichbar(Antwort antwort) {
    return antwort
        .rumpfOhneTraceId()
        .replaceAll("\"instance\"\\s*:\\s*\"[^\"]*\"", "\"instance\":\"-\"");
  }

  /** Was von einer fremden Nachricht auf keinen Fall im Rumpf stehen darf. */
  private static void ohneFremdeAngaben(Antwort antwort, Fremd fremd) {
    assertThat(antwort.rumpf())
        .as("Kein Anzeigename und keine Prozesskennung des fremden Mandanten")
        .doesNotContain(fremd.sosName())
        .doesNotContain(fremd.processId());
  }

  /**
   * Die Voraussetzung, ohne die alles Folgende wertlos waere: In diesem Fenster haben <b>beide</b>
   * Mandanten Daten, und beide erreichen ihr <b>eigenes</b> Detail.
   */
  @Test
  @DisplayName("Beide Mandanten haben Daten im Fenster und sehen ihr eigenes Detail")
  void beide_mandanten_sehen_ihr_eigenes_detail() throws Exception {
    String eigeneVonVotg = eineNachrichtVon(aufVotg).messageId();
    String eigeneVonSuttons = eineNachrichtVon(aufSuttons).messageId();

    assertThat(aufVotg.hole("/api/nachrichten/" + eigeneVonVotg).status()).isEqualTo(200);
    assertThat(aufSuttons.hole("/api/nachrichten/" + eigeneVonSuttons).status()).isEqualTo(200);
    assertThat(aufVotg.hole("/api/nachrichten/" + eigeneVonVotg + "/eigenschaften").status())
        .isEqualTo(200);
    assertThat(aufSuttons.hole("/api/nachrichten/" + eigeneVonSuttons + "/eigenschaften").status())
        .isEqualTo(200);
  }

  /**
   * Die eine Stelle, an der sich die beiden {@code 404}-Antworten unterscheiden duerfen — und der
   * Nachweis, dass sie dort nur die Frage zitiert und nichts nachgeschlagen hat.
   */
  @Test
  @DisplayName("instance spiegelt ausschliesslich den angefragten Pfad")
  void instanzSpiegeltNurDieAnfrage() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    assertThat(aufVotg.hole("/api/nachrichten/" + fremd.messageId()).<String>json("$.instance"))
        .isEqualTo("/api/nachrichten/" + fremd.messageId());
    assertThat(aufVotg.hole("/api/nachrichten/" + ERFUNDEN).<String>json("$.instance"))
        .isEqualTo("/api/nachrichten/" + ERFUNDEN);
  }

  // ─── Endpunkt 1: GET /api/nachrichten/{messageId} ───────────────────────────────

  @Test
  @DisplayName(
      "Detail: eine fremde Nachricht ist 404 und von einer erfundenen Kennung nicht zu"
          + " unterscheiden")
  void detail_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    Antwort fremdAberEcht = aufVotg.hole("/api/nachrichten/" + fremd.messageId());
    Antwort erfunden = aufVotg.hole("/api/nachrichten/" + ERFUNDEN);

    assertThat(fremdAberEcht.status())
        .as("Niemals 403 — das verriete, dass die Nachricht existiert")
        .isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht))
        .as(
            "Unterschieden sich die beiden Antworten in irgendetwas, das nicht die Frage selbst"
                + " ist, liesse sich mit einer Kennung aus einer geteilten URL der fremde Bestand"
                + " abfragen")
        .isEqualTo(vergleichbar(erfunden));
    ohneFremdeAngaben(fremdAberEcht, fremd);
  }

  @Test
  @DisplayName("Detail: die Trennung gilt in beide Richtungen")
  void detail_trennung_gilt_in_beide_richtungen() throws Exception {
    Fremd vonVotg = eineNachrichtVon(aufVotg);

    Antwort antwort = aufSuttons.hole("/api/nachrichten/" + vonVotg.messageId());

    assertThat(antwort.status())
        .as("sonst bewiese der Test nur, dass VOTG nichts sieht")
        .isEqualTo(404);
    ohneFremdeAngaben(antwort, vonVotg);
  }

  // ─── Endpunkt 2: GET /api/nachrichten/{messageId}/eigenschaften ─────────────────

  @Test
  @DisplayName("Eigenschaften: eine fremde Nachricht ist 404 — und keine leere Liste")
  void eigenschaften_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    Antwort fremdAberEcht =
        aufVotg.hole("/api/nachrichten/" + fremd.messageId() + "/eigenschaften");
    Antwort erfunden = aufVotg.hole("/api/nachrichten/" + ERFUNDEN + "/eigenschaften");

    assertThat(fremdAberEcht.status())
        .as(
            "Eine leere Liste waere eine andere Auskunft als „gibt es nicht\" — und damit eine"
                + " Auskunft ueber den Bestand")
        .isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht)).isEqualTo(vergleichbar(erfunden));
    ohneFremdeAngaben(fremdAberEcht, fremd);
  }

  @Test
  @DisplayName("Eigenschaften: die Trennung gilt in beide Richtungen")
  void eigenschaften_trennung_gilt_in_beide_richtungen() throws Exception {
    Fremd vonVotg = eineNachrichtVon(aufVotg);

    Antwort antwort = aufSuttons.hole("/api/nachrichten/" + vonVotg.messageId() + "/eigenschaften");

    assertThat(antwort.status()).isEqualTo(404);
    ohneFremdeAngaben(antwort, vonVotg);
  }

  /**
   * Der Fehlerrumpf traegt keinen internen Hinweis darauf, <i>warum</i> nichts gefunden wurde. Die
   * uebergebene Ursache geht ausschliesslich ins Protokoll — stuende sie in der Antwort,
   * unterschiede sich „gibt es nicht" wieder von „gehoert jemand anderem".
   */
  @Test
  @DisplayName("Die Fehlerantwort verraet nichts ueber die Ursache")
  void fehlerantwort_verraet_nichts() throws Exception {
    Antwort antwort = aufVotg.hole("/api/nachrichten/" + ERFUNDEN);

    assertThat(antwort.<String>json("$.type")).endsWith("/nicht-gefunden");
    assertThat(antwort.rumpf())
        .doesNotContain("Mandant")
        .doesNotContain("sichtbar")
        .doesNotContain("Message")
        .doesNotContain("select");
  }
}
