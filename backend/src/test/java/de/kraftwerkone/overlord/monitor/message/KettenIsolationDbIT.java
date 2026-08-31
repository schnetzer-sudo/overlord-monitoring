package de.kraftwerkone.overlord.monitor.message;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import de.kraftwerkone.overlord.monitor.security.Zugriffszaehler;
import de.kraftwerkone.overlord.monitor.security.Zugriffszaehlung;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jooq.ExecuteListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * <b>Die beiden Pflicht-Isolationstests der Verkettung</b> (Regel M4) — einer je Endpunkt. Ohne sie
 * wird nicht gemergt.
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
 * <p><b>Regel M5 hat hier eine zweite Seite.</b> Bei Liste und Detail geht es um die eine
 * angefragte Zeile; bei der Kette geht es zusaetzlich um jedes <i>Glied</i> — und um die
 * <b>Zaehlung</b>. Eine ungefilterte Zahl neben gefilterten Zeilen waere selbst eine Auskunft ueber
 * fremden Bestand.
 *
 * <p><b>Das Zeitfenster ist absolut</b> (29.12.2025). Ausser {@code NEXANS} endet jeder Mandant am
 * 30.12.2025 (M3); in einem relativen Fenster saehe {@code SUTTONS} je nach Datenstand null Zeilen
 * — und der Test bewiese nur, dass leer leer ist.
 *
 * <h2>Und die dritte Seite wird seit dem 31.08.2026 an der Ursache geprueft, nicht an der Uhr</h2>
 *
 * <p>Die Ununterscheidbarkeit hat eine Seite, die kein Rumpfvergleich erreicht: Eine
 * <b>nachgelagerte Existenzpruefung</b> kostet einen zusaetzlichen Datenbankzugriff und waere ueber
 * genug Anfragen ein messbarer Kanal, auch wenn beide Antworten Zeichen fuer Zeichen gleich sind.
 *
 * <p><b>Bis zum 31.08.2026 stand dafuer eine Wanduhrmessung</b> — zwei {@code System.nanoTime} um
 * zwei HTTP-Aufrufe, verglichen gegen eine Faktor-10-Schranke. Sie war <b>zeichengleich</b> zu der
 * Fassung, die {@code BamIsolationDbIT} am selben Tag abgeloest hat; nur der Pfad war ein anderer.
 * Sie hat einen Zugriff von einer halben Millisekunde (M30‑1) ueber HTTP auf einem Testrechner
 * geschuetzt. <b>Der Test war nicht ungenau, er hat die falsche Groesse gemessen</b> — das ist der
 * Befund aus {@code docs/testfestigkeit.md} §7, offener Punkt <b>T-4</b>.
 *
 * <p>Gemessen wird stattdessen die Groesse, um die es geht: <b>die Zahl der abgesetzten
 * Statements</b> auf dem Lese-Kontext. Der Zaehler ist ein zweiter jOOQ-{@link ExecuteListener} auf
 * {@code glassfishDsl} ({@link Zugriffszaehler}, angebracht ueber {@link Zugriffszaehlung}), er
 * lebt ausschliesslich in {@code src/test} und aendert am Anwendungscode nichts. <b>Es ist genau
 * die Bauform aus {@code BamIsolationDbIT}, und zwar dieselbe Klasse und keine Abschrift.</b>
 *
 * <p><b>Was die Zaehlung nicht abdeckt</b>, steht als offener Punkt T-1 in {@code
 * docs/testfestigkeit.md} §6: Zwei gleich viele Zugriffe koennten verschieden lange dauern.
 */
@Import(Zugriffszaehlung.class)
class KettenIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "kette-votg";
  private static final String NUTZER_B = PRAEFIX + "kette-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine MessageID, die es garantiert nicht gibt — die Gegenprobe zur fremden, echten. */
  private static final String ERFUNDEN = "00000000-0000-0000-0000-gibtesnicht";

  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-29T00:00:00");
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;
  @Autowired private Zugriffszaehler zugriffe;

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
   * Mandanten Daten, und beide erreichen ihre <b>eigene</b> Kette.
   */
  @Test
  @DisplayName("Beide Mandanten haben Daten im Fenster und sehen ihre eigene Kette")
  void beide_mandanten_sehen_ihre_eigene_kette() throws Exception {
    String eigeneVonVotg = eineNachrichtVon(aufVotg).messageId();
    String eigeneVonSuttons = eineNachrichtVon(aufSuttons).messageId();

    assertThat(aufVotg.hole("/api/nachrichten/" + eigeneVonVotg + "/kette").status())
        .isEqualTo(200);
    assertThat(aufSuttons.hole("/api/nachrichten/" + eigeneVonSuttons + "/kette").status())
        .isEqualTo(200);
    assertThat(aufVotg.hole("/api/nachrichten/" + eigeneVonVotg + "/kette/abwaerts").status())
        .isEqualTo(200);
    assertThat(aufSuttons.hole("/api/nachrichten/" + eigeneVonSuttons + "/kette/abwaerts").status())
        .isEqualTo(200);
  }

  @Test
  @DisplayName("instance spiegelt ausschliesslich den angefragten Pfad")
  void instanz_spiegelt_nur_die_anfrage() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    assertThat(
            aufVotg
                .hole("/api/nachrichten/" + fremd.messageId() + "/kette")
                .<String>json("$.instance"))
        .isEqualTo("/api/nachrichten/" + fremd.messageId() + "/kette");
    assertThat(aufVotg.hole("/api/nachrichten/" + ERFUNDEN + "/kette").<String>json("$.instance"))
        .isEqualTo("/api/nachrichten/" + ERFUNDEN + "/kette");
  }

  // ─── Endpunkt 1: GET /api/nachrichten/{messageId}/kette ─────────────────────────

  @Test
  @DisplayName(
      "Kette: eine fremde Nachricht ist 404 und von einer erfundenen Kennung nicht zu"
          + " unterscheiden")
  void kette_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    Antwort fremdAberEcht = aufVotg.hole("/api/nachrichten/" + fremd.messageId() + "/kette");
    Antwort erfunden = aufVotg.hole("/api/nachrichten/" + ERFUNDEN + "/kette");

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
  @DisplayName("Kette: die Trennung gilt in beide Richtungen")
  void kette_trennung_gilt_in_beide_richtungen() throws Exception {
    Fremd vonVotg = eineNachrichtVon(aufVotg);

    Antwort antwort = aufSuttons.hole("/api/nachrichten/" + vonVotg.messageId() + "/kette");

    assertThat(antwort.status())
        .as("sonst bewiese der Test nur, dass VOTG nichts sieht")
        .isEqualTo(404);
    ohneFremdeAngaben(antwort, vonVotg);
  }

  // ─── Endpunkt 2: GET /api/nachrichten/{messageId}/kette/abwaerts ────────────────

  @Test
  @DisplayName("Abwaerts: eine fremde Nachricht ist 404 — und keine leere Seite")
  void abwaerts_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    Fremd fremd = eineNachrichtVon(aufSuttons);

    Antwort fremdAberEcht =
        aufVotg.hole("/api/nachrichten/" + fremd.messageId() + "/kette/abwaerts");
    Antwort erfunden = aufVotg.hole("/api/nachrichten/" + ERFUNDEN + "/kette/abwaerts");

    assertThat(fremdAberEcht.status())
        .as(
            "Eine leere Seite waere eine andere Auskunft als „gibt es nicht\" — und damit eine"
                + " Auskunft ueber den Bestand")
        .isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht)).isEqualTo(vergleichbar(erfunden));
    ohneFremdeAngaben(fremdAberEcht, fremd);
  }

  @Test
  @DisplayName("Abwaerts: die Trennung gilt in beide Richtungen")
  void abwaerts_trennung_gilt_in_beide_richtungen() throws Exception {
    Fremd vonVotg = eineNachrichtVon(aufVotg);

    Antwort antwort =
        aufSuttons.hole("/api/nachrichten/" + vonVotg.messageId() + "/kette/abwaerts");

    assertThat(antwort.status()).isEqualTo(404);
    ohneFremdeAngaben(antwort, vonVotg);
  }

  // ─── Die Gegenprobe mit einer echten fremden Kennung ────────────────────────────

  /**
   * <b>Die Gegenprobe, die den eigentlichen Kern ausmacht</b> — mit einer <i>echten</i> fremden
   * Kennung, nicht nur mit einer erfundenen: als ADMIN den Mandanten wechseln, dort eine {@code
   * MessageID} holen, zurueckwechseln, dieselbe Kennung anfragen.
   *
   * <p><b>Verglichen wird auch die Zahl der Datenbankzugriffe.</b> Der Rumpfvergleich prueft die
   * <i>Wirkung</i>, nicht die <i>Ursache</i>: Ein Code, der erst die Existenz nachschluege und dann
   * denselben festen Text ausgaebe, bestuende ihn — und waere trotzdem unterscheidbar, weil „gibt
   * es nicht" einen Zugriff kostet und „gehoert einem anderen" zwei. Ueber genug Anfragen ist das
   * ein messbarer Kanal.
   *
   * <p><b>Gezaehlt und nicht gestoppt</b> <i>(seit 31.08.2026, Regel T1, offener Punkt T-4)</i>.
   * Bis dahin stand hier ein Vergleich zweier Wanduhrzeiten gegen eine Faktor-10-Schranke — die
   * <b>zeichengleiche</b> Fassung dessen, was {@code BamIsolationDbIT} am selben Tag abgeloest hat.
   * Sie hat eine halbe Millisekunde (M30‑1) ueber HTTP geschuetzt und musste deshalb gelegentlich
   * grundlos fallen. Die Zahl der abgesetzten Statements ist dieselbe Aussage ohne das Rauschen:
   * Sie ist genau das, was eine nachgelagerte Existenzpruefung veraendern wuerde.
   *
   * <p>Verglichen wird nicht nur die <b>Zahl</b>, sondern die <b>Folge der Statements</b>. Der Text
   * traegt Platzhalter statt Bindewerte; zwei Anfragen, die dasselbe Statement mit verschiedenen
   * Kennungen absetzen, sind darin Zeichen fuer Zeichen gleich. Ein zusaetzlicher Zugriff faellt
   * damit mit seinem Wortlaut auf und nicht nur als um eins hoehere Zahl.
   *
   * <p><b>Beide Endpunkte, nicht nur einer.</b> Die alte Fassung hat ausschliesslich {@code
   * …/kette} gemessen; {@code …/kette/abwaerts} ist ein eigener Weg mit eigener Zaehlung und
   * eigener Vorpruefung. Die Zaehlung ist billig genug, um ihn mitzunehmen — und teuer waere nur
   * die Luecke.
   *
   * <p>Der <b>Aufwaermlauf bleibt</b>, und aus einem anderen Grund als zuvor: Er ist keine
   * Beruhigung einer Messung mehr, sondern sorgt dafuer, dass ein einmaliger Zugriff beim ersten
   * Aufruf — Metadaten, Sitzungsaufbau — nicht in genau einer der beiden Folgen landet.
   */
  @Test
  @DisplayName("Eine echte fremde Kennung antwortet wie eine erfundene — auch in den Zugriffen")
  void gegenprobe_mit_echter_fremder_kennung() throws Exception {
    String admin = PRAEFIX + "kette-admin";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_B + "\"}").status())
        .isEqualTo(200);
    String fremdeKennung = eineNachrichtVon(alsAdmin).messageId();

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_A + "\"}").status())
        .isEqualTo(200);

    zugriffeSindUnunterscheidbar(alsAdmin, "/api/nachrichten/%s/kette", fremdeKennung);
    zugriffeSindUnunterscheidbar(alsAdmin, "/api/nachrichten/%s/kette/abwaerts", fremdeKennung);
  }

  /**
   * Fragt denselben Pfad einmal mit einer echten fremden und einmal mit einer erfundenen Kennung an
   * und haelt beide Antworten samt der <b>Folge der abgesetzten Statements</b> gegeneinander.
   *
   * @param vorlage der Pfad mit genau einem {@code %s} fuer die Kennung
   */
  private void zugriffeSindUnunterscheidbar(Sitzung sitzung, String vorlage, String fremdeKennung)
      throws IOException, InterruptedException {
    String echterPfad = vorlage.formatted(fremdeKennung);
    String erfundenerPfad = vorlage.formatted(ERFUNDEN);

    // Ein Aufwaermlauf, bevor gezaehlt wird — sonst traegt der erste der beiden Aufrufe einen
    // Zugriff, den es nur beim ersten Mal gibt, und der Vergleich beschriebe den Aufwaermeffekt.
    sitzung.hole(erfundenerPfad);
    sitzung.hole(echterPfad);

    zugriffe.zuruecksetzen();
    Antwort fremdAberEcht = sitzung.hole(echterPfad);
    List<String> beiEchter = zugriffe.abgesetzt();

    zugriffe.zuruecksetzen();
    Antwort erfunden = sitzung.hole(erfundenerPfad);
    List<String> beiErfundener = zugriffe.abgesetzt();

    assertThat(fremdAberEcht.status()).as("%s", echterPfad).isEqualTo(404);
    assertThat(erfunden.status()).as("%s", erfundenerPfad).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht))
        .as(
            "eine echte fremde Kennung ist von einer erfundenen nicht zu unterscheiden (%s)",
            vorlage)
        .isEqualTo(vergleichbar(erfunden));
    assertThat(fremdAberEcht.<String>json("$.instance"))
        .as(
            "die einzige Stelle, an der die Kennung vorkommen darf, ist das Zitat der Frage —"
                + " und dort steht sie zwangslaeufig, weil sie im Pfad steht (RFC 9457)")
        .isEqualTo(echterPfad);

    assertThat(beiEchter)
        .as(
            "Ohne einen einzigen Zugriff waere nichts gezaehlt worden und der Vergleich unten"
                + " bewiese, dass leer gleich leer ist (%s)",
            vorlage)
        .isNotEmpty();
    assertThat(beiEchter)
        .as(
            "eine nachgelagerte Existenzpruefung kostete einen zusaetzlichen Zugriff und waere"
                + " hier sichtbar — %s, echt: %s, erfunden: %s",
            vorlage, beiEchter, beiErfundener)
        .hasSameSizeAs(beiErfundener)
        .isEqualTo(beiErfundener);
  }

  /**
   * Der Fehlerrumpf traegt keinen internen Hinweis darauf, <i>warum</i> nichts gefunden wurde. Und
   * es entsteht <b>kein neuer Problemtyp</b>: {@code nicht-gefunden} ist der bestehende, den auch
   * ein unbekannter Pfad bekommt.
   */
  @Test
  @DisplayName("Die Fehlerantwort verraet nichts ueber die Ursache und traegt keinen neuen Typ")
  void fehlerantwort_verraet_nichts() throws Exception {
    Antwort antwort = aufVotg.hole("/api/nachrichten/" + ERFUNDEN + "/kette");

    assertThat(antwort.<String>json("$.type")).endsWith("/nicht-gefunden");
    assertThat(antwort.rumpf())
        .doesNotContain("Mandant")
        .doesNotContain("sichtbar")
        .doesNotContain("Message")
        .doesNotContain("select");
  }

  /** Ohne aktiven Mandanten gibt es keinen Zugriff — auch nicht fuer ADMIN (Regel M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antworten beide Endpunkte 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "kette-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort kette = alsAdmin.hole("/api/nachrichten/" + ERFUNDEN + "/kette");
    Antwort abwaerts = alsAdmin.hole("/api/nachrichten/" + ERFUNDEN + "/kette/abwaerts");

    assertThat(kette.status()).isEqualTo(403);
    assertThat(kette.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
    assertThat(abwaerts.status()).isEqualTo(403);
  }
}
