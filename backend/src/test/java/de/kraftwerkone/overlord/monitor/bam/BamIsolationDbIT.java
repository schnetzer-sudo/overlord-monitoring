package de.kraftwerkone.overlord.monitor.bam;

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
 * <b>Der Pflicht-Isolationstest des BAM-Endpunkts</b> (Regel M4). Ohne ihn wird nicht gemergt.
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
 * Kennung aus einer geteilten URL der fremde Bestand abfragen.
 *
 * <p><b>Hier hat die Ununterscheidbarkeit eine zweite Seite.</b> 80,6 Prozent aller Nachrichten
 * tragen keinen BAM-Wert (M41) — eine leere Gruppenliste ist also der Normalfall. Gerade deshalb
 * darf eine fremde Nachricht <b>keine leere Liste</b> bekommen: Sonst waere „leer" die Auskunft
 * „gibt es, gehoert aber jemand anderem", und der Bestand liesse sich abfragen, ohne dass je ein
 * Wert herausgegeben wuerde.
 *
 * <p><b>Das Zeitfenster ist absolut</b> (29.12.2025). Ausser {@code NEXANS} endet jeder Mandant am
 * 30.12.2025 (M3); in einem relativen Fenster saehe {@code SUTTONS} je nach Datenstand null Zeilen
 * — und der Test bewiese nur, dass leer leer ist.
 */
class BamIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "bam-votg";
  private static final String NUTZER_B = PRAEFIX + "bam-suttons";
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
    return "/api/nachrichten?limit=5&von="
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

  private static String bam(String messageId) {
    return "/api/nachrichten/" + messageId + "/bam";
  }

  /**
   * Der Rumpf ohne die beiden Angaben, die je Anfrage verschieden sind — <b>und verschieden sein
   * muessen</b>. {@code traceId} ist die Korrelations-ID, {@code instance} nach RFC 9457 der
   * angefragte Pfad; weil die {@code MessageID} <i>im Pfad</i> steht, enthaelt {@code instance} sie
   * zwangslaeufig. Sie spiegelt die Frage zurueck und sagt nichts darueber, ob es die Nachricht
   * gibt.
   */
  private static String vergleichbar(Antwort antwort) {
    return antwort
        .rumpfOhneTraceId()
        .replaceAll("\"instance\"\\s*:\\s*\"[^\"]*\"", "\"instance\":\"-\"");
  }

  private static void ohneFremdeAngaben(Antwort antwort, Fremd fremd) {
    assertThat(antwort.rumpf())
        .as("Kein Anzeigename und keine Prozesskennung des fremden Mandanten")
        .doesNotContain(fremd.sosName())
        .doesNotContain(fremd.processId());
  }

  /**
   * Die Voraussetzung, ohne die alles Folgende wertlos waere: In diesem Fenster haben <b>beide</b>
   * Mandanten Daten, und beide erreichen ihre <b>eigenen</b> Belegdaten.
   */
  @Test
  @DisplayName("Beide Mandanten haben Daten im Fenster und sehen ihre eigenen Belegdaten")
  void beide_mandanten_sehen_ihre_eigenen_belegdaten() throws Exception {
    String eigeneVonVotg = eineNachrichtVon(aufVotg).messageId();
    String eigeneVonSuttons = eineNachrichtVon(aufSuttons).messageId();

    Antwort votg = aufVotg.hole(bam(eigeneVonVotg));
    Antwort suttons = aufSuttons.hole(bam(eigeneVonSuttons));

    assertThat(votg.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(votg.<String>json("$.messageId")).isEqualTo(eigeneVonVotg);
    assertThat(suttons.<String>json("$.messageId")).isEqualTo(eigeneVonSuttons);
    assertThat(votg.rumpf())
        .as("gruppen ist immer vorhanden, leer statt fehlend")
        .contains("\"gruppen\"");
  }

  @Test
  @DisplayName("instance spiegelt ausschliesslich den angefragten Pfad")
  void instanz_spiegelt_nur_die_anfrage() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    assertThat(aufVotg.hole(bam(fremd.messageId())).<String>json("$.instance"))
        .isEqualTo(bam(fremd.messageId()));
    assertThat(aufVotg.hole(bam(ERFUNDEN)).<String>json("$.instance")).isEqualTo(bam(ERFUNDEN));
  }

  @Test
  @DisplayName(
      "Eine fremde Nachricht ist 404 und von einer erfundenen Kennung nicht zu unterscheiden")
  void fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    Antwort fremdAberEcht = aufVotg.hole(bam(fremd.messageId()));
    Antwort erfunden = aufVotg.hole(bam(ERFUNDEN));

    assertThat(fremdAberEcht.status())
        .as(
            "Niemals 403 — das verriete, dass die Nachricht existiert. Und niemals 200 mit leerer"
                + " Gruppenliste: „leer\" ist hier der Normalfall und waere damit die Auskunft"
                + " „gibt es, gehoert aber jemand anderem\"")
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
  @DisplayName("Die Trennung gilt in beide Richtungen")
  void trennung_gilt_in_beide_richtungen() throws Exception {
    Fremd vonVotg = eineNachrichtVon(aufVotg);

    Antwort antwort = aufSuttons.hole(bam(vonVotg.messageId()));

    assertThat(antwort.status())
        .as("sonst bewiese der Test nur, dass VOTG nichts sieht")
        .isEqualTo(404);
    ohneFremdeAngaben(antwort, vonVotg);
  }

  /**
   * <b>Die Gegenprobe, die den eigentlichen Kern ausmacht</b> — mit einer <i>echten</i> fremden
   * Kennung, nicht nur mit einer erfundenen: als ADMIN den Mandanten wechseln, dort eine {@code
   * MessageID} holen, zurueckwechseln, dieselbe Kennung anfragen.
   *
   * <p><b>Verglichen wird auch die Laufzeit.</b> Der Rumpfvergleich prueft die <i>Wirkung</i>,
   * nicht die <i>Ursache</i>: Ein Code, der erst die Existenz nachschluege und dann denselben
   * festen Text ausgaebe, bestuende ihn — und waere trotzdem unterscheidbar, weil „gibt es nicht"
   * einen Zugriff kostet und „gehoert einem anderen" zwei. Ueber genug Anfragen ist das ein
   * messbarer Kanal.
   *
   * <p>Die Grenze ist bewusst grob (Faktor zehn): Gemessen ist ein Zugriff von 0,44 Millisekunden
   * ({@code docs/bam-werte.md} §4), waehrend HTTP, Sitzungspruefung und Zufallslast auf dem
   * Testrechner deutlich mehr streuen. Der Test soll einen zusaetzlichen <i>Datenbankzugriff</i>
   * auffallen lassen, nicht das Rauschen eines Testlaufs.
   */
  @Test
  @DisplayName("Eine echte fremde Kennung antwortet wie eine erfundene — auch in der Laufzeit")
  void gegenprobe_mit_echter_fremder_kennung() throws Exception {
    String admin = PRAEFIX + "bam-admin";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_B + "\"}").status())
        .isEqualTo(200);
    String fremdeKennung = eineNachrichtVon(alsAdmin).messageId();

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_A + "\"}").status())
        .isEqualTo(200);

    // Ein Aufwaermlauf, bevor gemessen wird — sonst traegt der erste der beiden Aufrufe die
    // Kosten des ersten Zugriffs auf Message und der Vergleich beschriebe den Aufwaermeffekt.
    alsAdmin.hole(bam(ERFUNDEN));
    alsAdmin.hole(bam(fremdeKennung));

    long vorEcht = System.nanoTime();
    Antwort fremdAberEcht = alsAdmin.hole(bam(fremdeKennung));
    long dauerEcht = System.nanoTime() - vorEcht;

    long vorErfunden = System.nanoTime();
    Antwort erfunden = alsAdmin.hole(bam(ERFUNDEN));
    long dauerErfunden = System.nanoTime() - vorErfunden;

    assertThat(fremdAberEcht.status()).isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht))
        .as("eine echte fremde Kennung ist von einer erfundenen nicht zu unterscheiden")
        .isEqualTo(vergleichbar(erfunden));
    assertThat(fremdAberEcht.<String>json("$.instance"))
        .as(
            "die einzige Stelle, an der die Kennung vorkommen darf, ist das Zitat der Frage —"
                + " und dort steht sie zwangslaeufig, weil sie im Pfad steht (RFC 9457)")
        .isEqualTo(bam(fremdeKennung));

    double verhaeltnis =
        (double) Math.max(dauerEcht, dauerErfunden) / Math.min(dauerEcht, dauerErfunden);
    assertThat(verhaeltnis)
        .as(
            "eine vorgelagerte Existenzpruefung kostete einen zusaetzlichen Zugriff und waere hier"
                + " sichtbar (echt: %d ns, erfunden: %d ns)",
            dauerEcht, dauerErfunden)
        .isLessThan(10.0);
  }

  /**
   * Der Fehlerrumpf traegt keinen internen Hinweis darauf, <i>warum</i> nichts gefunden wurde. Und
   * es entsteht <b>kein neuer Problemtyp</b>: {@code nicht-gefunden} ist der bestehende, den auch
   * ein unbekannter Pfad bekommt.
   */
  @Test
  @DisplayName("Die Fehlerantwort verraet nichts ueber die Ursache und traegt keinen neuen Typ")
  void fehlerantwort_verraet_nichts() throws Exception {
    Antwort antwort = aufVotg.hole(bam(ERFUNDEN));

    assertThat(antwort.<String>json("$.type")).endsWith("/nicht-gefunden");
    assertThat(antwort.rumpf())
        .doesNotContain("Mandant")
        .doesNotContain("sichtbar")
        .doesNotContain("MessageBAM")
        .doesNotContain("select");
  }

  /** Ohne aktiven Mandanten gibt es keinen Zugriff — auch nicht fuer ADMIN (Regeln M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antwortet der Endpunkt 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "bam-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort antwort = alsAdmin.hole(bam(ERFUNDEN));

    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
  }
}
