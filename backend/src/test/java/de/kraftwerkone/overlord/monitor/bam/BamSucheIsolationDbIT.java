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
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <b>Der Pflicht-Isolationstest der BAM-Suche</b> (Regel M4). Ohne ihn wird nicht gemergt.
 *
 * <p>Er kopiert das Muster aus {@code MandantenIsolationDbIT} und tauscht Aufruf und Eingabe. Die
 * Paarung ist die von {@code NachrichtenIsolationDbIT}: <b>{@code NEXANS} gegen {@code SUTTONS}</b>
 * — zwei Mandanten aus <b>verschiedenen Häusern</b> und die beiden mit dem größten Bestand, also
 * die Paarung, bei der ein Leck am ehesten sichtbar würde. {@code NXHBE} und {@code IBISGUS}
 * scheiden aus; zwei Mandanten desselben Konzerns sind ein schlechter Beweis für eine Trennung, die
 * zwischen Firmen greifen soll.
 *
 * <h2>Wo die Gegenprobe hier anders aussieht als bei den Pfad-Endpunkten</h2>
 *
 * <p>Bei Detail, Kette und Belegdaten steht die Kennung <i>im Pfad</i>, und die Gegenprobe
 * vergleicht zwei {@code 404}-Rümpfe. <b>Hier gibt es kein {@code 404}</b>: Eine Suche, die nichts
 * findet, ist {@code 200} mit leerer Liste — genau wie eine Suche nach einem Wert, den es nicht
 * gibt. Die Ununterscheidbarkeit verschiebt sich damit vom Statuscode auf den <b>Rumpf</b>.
 *
 * <p><b>Und der Rumpf zitiert die Frage</b>, weil er die Begriffe samt ihrer Varianten nennt (keine
 * stille Korrektur). Das ist dieselbe Lage wie beim Feld {@code instance} nach RFC 9457: eine
 * Spiegelung der Eingabe und keine Auskunft über den Bestand. Der Test behandelt sie genauso — er
 * normalisiert das Zitat <b>und</b> weist zusätzlich nach, dass es genau die gesendete Eingabe ist.
 *
 * <p><b>Die Varianten hängen ausschließlich am Mandanten der Sitzung</b>, nie am Mandanten des
 * gesuchten Werts: Die Kuratierung wird für den aktiven Mandanten gelesen. Damit ein Unterschied in
 * der <i>Zahl</i> der Varianten den Vergleich nicht stört, ist die erfundene Eingabe <b>genauso
 * lang</b> wie die fremde.
 *
 * <p><b>Das Zeitfenster ist absolut</b>. Außer {@code NEXANS} endet jeder Mandant am 30.12.2025
 * (M3); in einem relativen Fenster sähe {@code SUTTONS} je nach Datenstand null Zeilen, und der
 * Test bewiese nur, dass leer leer ist.
 */
class BamSucheIsolationDbIT extends SicherheitsTestbasis {

  /** Mandant A — der mit dem größten BAM-Bestand. */
  private static final String NEXANS = "NEXANS";

  private static final String NUTZER_A = PRAEFIX + "bamsuche-nexans";
  private static final String NUTZER_B = PRAEFIX + "bamsuche-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Fenster B — dieselbe Spanne wie die Vorgabe des Endpunkts. */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-11-30T00:00:00");

  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  /** Fenster A — ein Tag, für die Abschneidung. */
  private static final LocalDateTime TAG_VON = LocalDateTime.parse("2025-12-29T00:00:00");

  private static final LocalDateTime TAG_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung aufNexans;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  // ─── Herleitung ───────────────────────────────────────────────────────────────

  /**
   * Ein BAM-Wert eines Mandanten samt der Nachricht und ihres Prozesses — nach Gestalt gefunden.
   */
  private record Fremd(String wert, String messageId, String processId) {}

  private Fremd einWertVon(String mandant) {
    Record fund =
        glassfishDsl
            .resultQuery(
                """
                select b.MessageBAMValue as wert, m.MessageID as kennung, m.ProcessID as prozess
                from Message m
                join MessageBAM b      on b.MessageID  = m.MessageID
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                where pm.MandantID = ?
                  and m.MessageLastUpdate >= ?
                  and m.MessageLastUpdate <  ?
                order by m.MessageID, b.MessageBAMType, b.MessageBAMValue
                limit 1
                """,
                mandant,
                FENSTER_VON,
                FENSTER_BIS)
            .fetchOne();
    assertThat(fund)
        .as(
            "%s traegt im Fenster keinen einzigen BAM-Wert — dann bewiese der Test nur, dass leer"
                + " leer ist",
            mandant)
        .isNotNull();
    return new Fremd(
        fund.get("wert", String.class),
        fund.get("kennung", String.class),
        fund.get("prozess", String.class));
  }

  /**
   * Der Wert, der bei {@code NEXANS} an einem Tag auf den meisten Nachrichten steht — die Grundlage
   * für die Abschneidung. Gesucht wird über einen Tag und nicht über den Monat: Die Frage ist, ob
   * ein Wert das Limit sprengt, und dafür genügt der dichteste Ausschnitt.
   */
  private String haeufigsterWertAmTag() {
    Record fund =
        glassfishDsl
            .resultQuery(
                """
                select b.MessageBAMValue as wert, count(distinct b.MessageID) as nachrichten
                from Message m
                join MessageBAM b      on b.MessageID  = m.MessageID
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                where pm.MandantID = ?
                  and m.MessageLastUpdate >= ?
                  and m.MessageLastUpdate <  ?
                group by b.MessageBAMValue
                order by nachrichten desc, b.MessageBAMValue
                limit 1
                """,
                NEXANS,
                TAG_VON,
                TAG_BIS)
            .fetchOne();
    assertThat(fund).isNotNull();
    assertThat(fund.get("nachrichten", Long.class))
        .as("Ohne einen Wert ueber dem Limit prueft die Abschneidung nichts")
        .isGreaterThan(BamSucheRepository.HOECHSTENS_TREFFER);
    return fund.get("wert", String.class);
  }

  // ─── Aufrufe ──────────────────────────────────────────────────────────────────

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String suche(LocalDateTime von, LocalDateTime bis, String begriff) {
    return "/api/bam/suche?von="
        + URLEncoder.encode(iso(von), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(bis), StandardCharsets.UTF_8)
        + "&begriff="
        + URLEncoder.encode(":" + begriff, StandardCharsets.UTF_8);
  }

  private String suche(String begriff) {
    return suche(FENSTER_VON, FENSTER_BIS, begriff);
  }

  /**
   * Der Rumpf ohne die {@code traceId} und ohne das <b>Zitat der Eingabe</b>.
   *
   * <p>Der Antwortrumpf nennt die gesuchten Fassungen, damit es keine stille Korrektur gibt — er
   * spiegelt damit zwangsläufig die Frage. Dass die Spiegelung eine Spiegelung <i>bleibt</i>, prüft
   * {@link #das_zitat_bleibt_ein_zitat()} zusätzlich Zeichen für Zeichen.
   */
  private static String vergleichbar(Antwort antwort, String eingabe) {
    return antwort.rumpfOhneTraceId().replace(eingabe, "-");
  }

  /** Eine Eingabe derselben Länge, die es im Bestand nicht geben kann. */
  private static String erfunden(String vorbild) {
    return "X".repeat(vorbild.length());
  }

  // ─── Die Voraussetzung ────────────────────────────────────────────────────────

  /** Ohne diese Zusicherung wäre alles Folgende wertlos. */
  @Test
  @DisplayName("Beide Mandanten haben Werte im Fenster und finden ihre eigenen")
  void beide_finden_ihre_eigenen_werte() throws Exception {
    Fremd vonNexans = einWertVon(NEXANS);
    Fremd vonSuttons = einWertVon(MANDANT_B);

    Antwort nexans = aufNexans.hole(suche(vonNexans.wert()));
    Antwort suttons = aufSuttons.hole(suche(vonSuttons.wert()));

    assertThat(nexans.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(nexans.<List<String>>json("$.nachrichten[*].messageId"))
        .contains(vonNexans.messageId());
    assertThat(suttons.<List<String>>json("$.nachrichten[*].messageId"))
        .contains(vonSuttons.messageId());
  }

  // ─── Der Kern ─────────────────────────────────────────────────────────────────

  /**
   * <b>Ein fremder Wert findet nichts</b> — und zwar so, dass es von einem erfundenen Wert nicht zu
   * unterscheiden ist. Wäre es unterscheidbar, ließe sich mit einer Belegnummer aus einer E-Mail
   * feststellen, ob ein anderer Mandant sie führt.
   */
  @Test
  @DisplayName("Ein fremder Wert ist ununterscheidbar von einem erfundenen")
  void fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    Fremd fremd = einWertVon(MANDANT_B);
    String erfunden = erfunden(fremd.wert());

    Antwort fremdAberEcht = aufNexans.hole(suche(fremd.wert()));
    Antwort ohneVorbild = aufNexans.hole(suche(erfunden));

    assertThat(fremdAberEcht.status())
        .as("keine leere Liste mit 403 und kein 404 — eine Suche ohne Treffer ist 200")
        .isEqualTo(200);
    assertThat(ohneVorbild.status()).isEqualTo(200);
    assertThat(fremdAberEcht.<List<String>>json("$.nachrichten[*].messageId"))
        .as("die fremde Nachricht ist unerreichbar")
        .isEmpty();
    assertThat(vergleichbar(fremdAberEcht, fremd.wert()))
        .as(
            "Unterschieden sich die beiden Antworten in irgendetwas, das nicht die Frage selbst"
                + " ist, liesse sich ueber die Suche der fremde Bestand abfragen")
        .isEqualTo(vergleichbar(ohneVorbild, erfunden));
    assertThat(fremdAberEcht.rumpf())
        .as("keine Kennung und keine Prozesskennung des fremden Mandanten")
        .doesNotContain(fremd.messageId())
        .doesNotContain(fremd.processId());
  }

  /**
   * <b>Das Zitat bleibt ein Zitat.</b> Der Rumpf nennt die Eingabe und die daraus gebildeten
   * Fassungen — beide entstehen aus der Eingabe und der Kuratierung <i>des aktiven Mandanten</i>.
   * Käme darin je etwas vor, das aus dem Bestand stammt, wäre die Spiegelung eine Auskunft.
   */
  @Test
  @DisplayName("Die genannten Begriffe sind ausschliesslich die gesendete Eingabe")
  void das_zitat_bleibt_ein_zitat() throws Exception {
    Fremd fremd = einWertVon(MANDANT_B);

    Antwort antwort = aufNexans.hole(suche(fremd.wert()));

    assertThat(antwort.<List<String>>json("$.begriffe[*].eingabe"))
        .as("Zeichen fuer Zeichen die gesendete Eingabe")
        .containsExactly(fremd.wert());
    assertThat(antwort.<List<String>>json("$.begriffe[0].varianten"))
        .as("jede Fassung entsteht aus der Eingabe — nichts davon kommt aus dem Bestand")
        .allSatisfy(fassung -> assertThat(fassung).endsWith(fremd.wert()));
  }

  /** Sonst bewiese der Test nur, dass {@code SUTTONS} nichts sieht. */
  @Test
  @DisplayName("Die Trennung gilt in beide Richtungen")
  void trennung_gilt_in_beide_richtungen() throws Exception {
    Fremd vonNexans = einWertVon(NEXANS);

    Antwort antwort = aufSuttons.hole(suche(vonNexans.wert()));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(antwort.rumpf())
        .doesNotContain(vonNexans.messageId())
        .doesNotContain(vonNexans.processId());
  }

  // ─── Die Abschneidung ─────────────────────────────────────────────────────────

  /**
   * <b>Die Abschneidung wird nach dem Mandantenfilter gezählt</b> — dieselbe Suche, zwei Mandanten,
   * zwei verschiedene Zahlen.
   *
   * <p>Würde sie aus den Rohtreffern gezählt, meldete auch {@code SUTTONS} „es gibt mehr" — und
   * sagte damit etwas über die Datenmenge eines fremden Mandanten. Genau die Sorte Leck, gegen die
   * die 404-Regel beim Mandantenwechsel gebaut ist.
   */
  @Test
  @DisplayName("Die Abschneidung wird nach dem Mandantenfilter gezaehlt")
  void abschneidung_wird_nach_dem_mandantenfilter_gezaehlt() throws Exception {
    String haeufig = haeufigsterWertAmTag();

    Antwort nexans = aufNexans.hole(suche(TAG_VON, TAG_BIS, haeufig));
    Antwort suttons = aufSuttons.hole(suche(TAG_VON, TAG_BIS, haeufig));

    assertThat(nexans.<Boolean>json("$.abgeschnitten"))
        .as("fuer NEXANS gibt es mehr als das Limit")
        .isTrue();
    assertThat(nexans.<List<String>>json("$.nachrichten[*].messageId"))
        .hasSize(BamSucheRepository.HOECHSTENS_TREFFER);

    assertThat(suttons.<List<String>>json("$.nachrichten[*].messageId"))
        .as("derselbe Wert gehoert SUTTONS nicht")
        .isEmpty();
    assertThat(suttons.<Boolean>json("$.abgeschnitten"))
        .as("eine Abschneidung aus den Rohtreffern verriete die Datenmenge des anderen Mandanten")
        .isFalse();
  }

  // ─── Rolle und Kontext ────────────────────────────────────────────────────────

  /** Ohne aktiven Mandanten gibt es keinen Zugriff — auch nicht für ADMIN (Regeln M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antwortet der Endpunkt 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "bamsuche-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort antwort = alsAdmin.hole(suche("4711815"));

    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
  }

  /**
   * <b>Die Gegenprobe mit einer echten fremden Eingabe über den Mandantenwechsel</b> — als ADMIN
   * den Mandanten wechseln, dort einen Wert holen, zurückwechseln, denselben Wert suchen.
   *
   * <p>Sie ist die schärfere Fassung: Der Wert ist nachweislich echt, und die Antwort muss trotzdem
   * dieselbe sein wie für eine erfundene Eingabe.
   */
  @Test
  @DisplayName("Auch nach einem Mandantenwechsel bleibt der fremde Bestand unerreichbar")
  void gegenprobe_ueber_den_mandantenwechsel() throws Exception {
    String admin = PRAEFIX + "bamsuche-admin";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_B + "\"}").status())
        .isEqualTo(200);
    Fremd fremd = einWertVon(MANDANT_B);
    assertThat(alsAdmin.hole(suche(fremd.wert())).<List<String>>json("$.nachrichten[*].messageId"))
        .as("als SUTTONS ist der Wert erreichbar — sonst prueft die Gegenprobe nichts")
        .contains(fremd.messageId());

    assertThat(alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}").status())
        .isEqualTo(200);

    Antwort nachDemWechsel = alsAdmin.hole(suche(fremd.wert()));
    Antwort erfunden = alsAdmin.hole(suche(erfunden(fremd.wert())));

    assertThat(nachDemWechsel.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(vergleichbar(nachDemWechsel, fremd.wert()))
        .isEqualTo(vergleichbar(erfunden, erfunden(fremd.wert())));
  }
}
