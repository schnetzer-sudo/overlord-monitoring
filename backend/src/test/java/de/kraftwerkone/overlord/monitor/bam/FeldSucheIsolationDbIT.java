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
 * <b>Der Pflicht-Isolationstest der Property-Suche</b> — der Parameter {@code feld} an {@code GET
 * /api/bam/suche} (Regel M4). Ohne ihn wird nicht gemergt.
 *
 * <p>Er kopiert das Muster aus {@code BamSucheIsolationDbIT}: Paarung <b>{@code NEXANS} gegen
 * {@code SUTTONS}</b>, absolutes Fenster B, und die Gegenprobe ohne {@code 404} — eine Suche, die
 * nichts findet, ist {@code 200} mit leerer Liste, und die Ununterscheidbarkeit liegt im Rumpf.
 *
 * <h2>Zwei Zugriffsarten, zwei Nachweise</h2>
 *
 * <p>Die Property-Suche hat zwei Pfade (E‑101), und beide brauchen den Nachweis: der
 * <b>EAV-Zugriff</b> ueber Name und Wert in {@code MessageProperty} und das <b>Spaltenpraedikat</b>
 * auf {@code Message}. Der zweite ist der schaerfere Fall — {@code feld=Message.MessageID:<fremde
 * Kennung>} ist ein Zugriff ueber den Primaerschluessel, und wenn die Mandantenkette irgendwo nicht
 * im Statement stuende, dann faende er die Zeile.
 *
 * <h2>Die Herleitung der Pruefwerte</h2>
 *
 * <p><b>Kein Pruefwert steht in dieser Datei</b> (Regeln G1 und T2). Ein Eigenschaftswert eines
 * Mandanten wird nach Gestalt gefunden: die erste Nachricht des Mandanten am dichtesten Tag, davon
 * die erste Eigenschaft, deren Wert <b>selten</b> ist — hoechstens {@link
 * BamSucheRepository#HOECHSTENS_TREFFER} Nachrichten tragen ihn —, damit „findet die eigene
 * Nachricht" nicht an der Deckelung scheitert. Welcher Name das ist, entscheidet der Bestand und
 * nicht dieser Test.
 */
class FeldSucheIsolationDbIT extends SicherheitsTestbasis {

  private static final String NEXANS = "NEXANS";

  private static final String NUTZER_A = PRAEFIX + "feldsuche-nexans";
  private static final String NUTZER_B = PRAEFIX + "feldsuche-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Fenster B — dieselbe Spanne wie die Vorgabe des Endpunkts. */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-11-30T00:00:00");

  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  /** Fenster A — der dichteste Tag, fuer die Herleitung und die Abschneidung. */
  private static final LocalDateTime TAG_VON = LocalDateTime.parse("2025-12-29T00:00:00");

  private static final LocalDateTime TAG_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung aufNexans;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  // ─── Herleitung ───────────────────────────────────────────────────────────────

  /** Eine Eigenschaft eines Mandanten samt Nachricht und Prozess — nach Gestalt gefunden. */
  record Eigenschaft(String name, String wert, String messageId, String processId) {}

  private Record ersteNachrichtVon(String mandant) {
    Record fund =
        glassfishDsl
            .resultQuery(
                """
                select m.MessageID as kennung, m.ProcessID as prozess
                from Message m
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                where pm.MandantID = ?
                  and m.MessageLastUpdate >= ?
                  and m.MessageLastUpdate <  ?
                order by m.MessageID
                limit 1
                """,
                mandant,
                TAG_VON,
                TAG_BIS)
            .fetchOne();
    assertThat(fund)
        .as(
            "%s hat am dichtesten Tag keine Nachricht — dann hat sich die Testkopie geaendert",
            mandant)
        .isNotNull();
    return fund;
  }

  /**
   * Die erste seltene Eigenschaft der ersten Nachricht: hoechstens {@link
   * BamSucheRepository#HOECHSTENS_TREFFER} Nachrichten tragen denselben Wert unter demselben Namen.
   * Die Zaehlung laeuft ueber {@code MessagePropertyNameValueIDX} und bricht bei einem haeufigen
   * Wert nicht ab, sondern zaehlt ihn — deshalb werden lange Werte bevorzugt, sie sind seltener.
   */
  private Eigenschaft eineSelteneEigenschaftVon(String mandant) {
    Record nachricht = ersteNachrichtVon(mandant);
    String messageId = nachricht.get("kennung", String.class);
    String processId = nachricht.get("prozess", String.class);

    List<Record> kandidaten =
        glassfishDsl.fetch(
            """
            select MessagePropertyName as name, MessagePropertyValue as wert
            from MessageProperty
            where MessageID = ?
              and char_length(MessagePropertyValue) between 8 and 50
            order by char_length(MessagePropertyValue) desc, MessagePropertyName
            """,
            messageId);
    for (Record kandidat : kandidaten) {
      String name = kandidat.get("name", String.class);
      String wert = kandidat.get("wert", String.class);
      Integer traeger =
          glassfishDsl
              .resultQuery(
                  """
                  select count(distinct MessageID)
                  from MessageProperty
                  where MessagePropertyName = ? and MessagePropertyValue = ?
                  """,
                  name,
                  wert)
              .fetchOne(0, Integer.class);
      if (traeger != null && traeger <= BamSucheRepository.HOECHSTENS_TREFFER) {
        return new Eigenschaft(name, wert, messageId, processId);
      }
    }
    throw new AssertionError(
        "Keine seltene Eigenschaft auf der ersten Nachricht von "
            + mandant
            + " — dann hat sich der Bestand geaendert, und ein anderer Anker gehoert hierher.");
  }

  /** Der Prozess, der bei {@code NEXANS} am dichtesten Tag die meisten Nachrichten traegt. */
  private String haeufigsterProzessAmTag() {
    Record fund =
        glassfishDsl
            .resultQuery(
                """
                select m.ProcessID as prozess, count(*) as nachrichten
                from Message m
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                where pm.MandantID = ?
                  and m.MessageLastUpdate >= ?
                  and m.MessageLastUpdate <  ?
                group by m.ProcessID
                order by nachrichten desc, m.ProcessID
                limit 1
                """,
                NEXANS,
                TAG_VON,
                TAG_BIS)
            .fetchOne();
    assertThat(fund).isNotNull();
    assertThat(fund.get("nachrichten", Long.class))
        .as("Ohne einen Prozess ueber dem Limit prueft die Abschneidung nichts")
        .isGreaterThan(BamSucheRepository.HOECHSTENS_TREFFER);
    return fund.get("prozess", String.class);
  }

  private int nachrichtenVon(String mandant, String processId) {
    Integer zahl =
        glassfishDsl
            .resultQuery(
                """
                select count(*)
                from Message m
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                where pm.MandantID = ? and m.ProcessID = ?
                  and m.MessageLastUpdate >= ? and m.MessageLastUpdate < ?
                """,
                mandant,
                processId,
                TAG_VON,
                TAG_BIS)
            .fetchOne(0, Integer.class);
    return zahl == null ? 0 : zahl;
  }

  // ─── Aufrufe ──────────────────────────────────────────────────────────────────

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String feldsuche(LocalDateTime von, LocalDateTime bis, String name, String wert) {
    return "/api/bam/suche?von="
        + URLEncoder.encode(iso(von), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(bis), StandardCharsets.UTF_8)
        + "&feld="
        + URLEncoder.encode(name + ":" + wert, StandardCharsets.UTF_8);
  }

  private String feldsuche(String name, String wert) {
    return feldsuche(FENSTER_VON, FENSTER_BIS, name, wert);
  }

  /** Der Rumpf ohne {@code traceId} und ohne das Zitat des Werts. */
  private static String vergleichbar(Antwort antwort, String wert) {
    return antwort.rumpfOhneTraceId().replace(wert, "-");
  }

  /** Ein Wert derselben Laenge, den es im Bestand nicht geben kann. */
  private static String erfunden(String vorbild) {
    return "X".repeat(vorbild.length());
  }

  // ─── Die Voraussetzung ────────────────────────────────────────────────────────

  /** Ohne diese Zusicherung waere alles Folgende wertlos. */
  @Test
  @DisplayName("Beide Mandanten finden ihre eigene Nachricht ueber eine Eigenschaft")
  void beide_finden_ihre_eigenen_eigenschaften() throws Exception {
    Eigenschaft vonNexans = eineSelteneEigenschaftVon(NEXANS);
    Eigenschaft vonSuttons = eineSelteneEigenschaftVon(MANDANT_B);

    Antwort nexans = aufNexans.hole(feldsuche(vonNexans.name(), vonNexans.wert()));
    Antwort suttons = aufSuttons.hole(feldsuche(vonSuttons.name(), vonSuttons.wert()));

    assertThat(nexans.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(nexans.<List<String>>json("$.nachrichten[*].messageId"))
        .contains(vonNexans.messageId());
    assertThat(suttons.<List<String>>json("$.nachrichten[*].messageId"))
        .contains(vonSuttons.messageId());
  }

  // ─── Der EAV-Zugriff ──────────────────────────────────────────────────────────

  /**
   * <b>Eine fremde Eigenschaft findet nichts</b> — und zwar so, dass es von einem erfundenen Wert
   * nicht zu unterscheiden ist. Der Name ist echt und bleibt; der Wert ist das, was fremd ist.
   */
  @Test
  @DisplayName("Eine fremde Eigenschaft ist ununterscheidbar von einer erfundenen")
  void fremde_eigenschaft_ist_ununterscheidbar_von_erfundener() throws Exception {
    Eigenschaft fremd = eineSelteneEigenschaftVon(MANDANT_B);
    String erfunden = erfunden(fremd.wert());

    Antwort fremdAberEcht = aufNexans.hole(feldsuche(fremd.name(), fremd.wert()));
    Antwort ohneVorbild = aufNexans.hole(feldsuche(fremd.name(), erfunden));

    assertThat(fremdAberEcht.status())
        .as("kein 403, kein 404 — 200 mit leerer Liste")
        .isEqualTo(200);
    assertThat(ohneVorbild.status()).isEqualTo(200);
    assertThat(fremdAberEcht.<List<String>>json("$.nachrichten[*].messageId"))
        .as("die fremde Nachricht ist unerreichbar")
        .isEmpty();
    assertThat(vergleichbar(fremdAberEcht, fremd.wert()))
        .as("jeder Unterschied ausser der Frage selbst waere eine Auskunft ueber fremden Bestand")
        .isEqualTo(vergleichbar(ohneVorbild, erfunden));
    // Das Zitat bleibt ein Zitat: Der Wert steht im Rumpf, weil er die Frage ist — und sonst
    // nirgends. Bei Message.GUID IST der Wert die Kennung der Nachricht (eine Zeile je Nachricht,
    // M157); deshalb wird die Kennung im Rumpf OHNE das Zitat gesucht.
    assertThat(fremdAberEcht.<List<String>>json("$.felder[*].wert")).containsExactly(fremd.wert());
    assertThat(vergleichbar(fremdAberEcht, fremd.wert()))
        .doesNotContain(fremd.messageId())
        .doesNotContain(fremd.processId());
  }

  /** Sonst bewiese der Test nur, dass {@code SUTTONS} nichts sieht. */
  @Test
  @DisplayName("Die Trennung gilt in beide Richtungen")
  void trennung_gilt_in_beide_richtungen() throws Exception {
    Eigenschaft vonNexans = eineSelteneEigenschaftVon(NEXANS);

    Antwort antwort = aufSuttons.hole(feldsuche(vonNexans.name(), vonNexans.wert()));
    Antwort erfunden = aufSuttons.hole(feldsuche(vonNexans.name(), erfunden(vonNexans.wert())));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(vergleichbar(antwort, vonNexans.wert()))
        .as("ausser dem Zitat nichts aus dem fremden Bestand")
        .doesNotContain(vonNexans.messageId())
        .doesNotContain(vonNexans.processId());
    assertThat(vergleichbar(antwort, vonNexans.wert()))
        .isEqualTo(vergleichbar(erfunden, erfunden(vonNexans.wert())));
  }

  // ─── Das Spaltenpraedikat ─────────────────────────────────────────────────────

  /**
   * <b>Der schaerfste Fall: die fremde Kennung ueber den Primaerschluessel.</b> {@code
   * Message.MessageID} ist ein Spaltenpraedikat auf {@code Message} — stuende die Mandantenkette
   * nicht im selben Statement, faende dieser Zugriff die Zeile in einem Schritt.
   */
  @Test
  @DisplayName("Eine fremde MessageID als Spaltenfeld ist ununterscheidbar von einer erfundenen")
  void fremde_kennung_als_spalte_ist_ununterscheidbar() throws Exception {
    Record fremd = ersteNachrichtVon(MANDANT_B);
    String kennung = fremd.get("kennung", String.class);
    String erfunden = erfunden(kennung);

    Antwort fremdAberEcht = aufNexans.hole(feldsuche("Message.MessageID", kennung));
    Antwort ohneVorbild = aufNexans.hole(feldsuche("Message.MessageID", erfunden));

    assertThat(fremdAberEcht.status()).isEqualTo(200);
    assertThat(fremdAberEcht.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(fremdAberEcht.<List<Boolean>>json("$.felder[*].spalte"))
        .as("der Pfad, der geprueft wird, ist das Spaltenpraedikat")
        .containsExactly(true);
    assertThat(vergleichbar(fremdAberEcht, kennung)).isEqualTo(vergleichbar(ohneVorbild, erfunden));
    assertThat(fremdAberEcht.rumpf()).doesNotContain(fremd.get("prozess", String.class));

    // Und dieselbe Kennung ist beim eigenen Mandanten erreichbar — sonst pruefte das nichts.
    assertThat(
            aufSuttons
                .hole(feldsuche("Message.MessageID", kennung))
                .<List<String>>json("$.nachrichten[*].messageId"))
        .containsExactly(kennung);
  }

  /**
   * Auch der Join-Pfad ({@code Process}) traegt die Kette — ueber den Prozess des fremden
   * Mandanten.
   */
  @Test
  @DisplayName("Ein fremder Prozess als Spaltenfeld findet nichts — auch ueber den Namen")
  void fremder_prozess_als_spalte_findet_nichts() throws Exception {
    Record fremd = ersteNachrichtVon(MANDANT_B);
    String processId = fremd.get("prozess", String.class);
    String processName =
        glassfishDsl
            .resultQuery("select ProcessName from Process where ProcessID = ?", processId)
            .fetchOne(0, String.class);
    assertThat(processName).isNotBlank();

    assertThat(
            aufNexans
                .hole(feldsuche("Message.ProcessID", processId))
                .<List<String>>json("$.nachrichten[*].messageId"))
        .isEmpty();
    assertThat(
            aufNexans
                .hole(feldsuche("Message.ProcessName", processName))
                .<List<String>>json("$.nachrichten[*].messageId"))
        .isEmpty();
    assertThat(
            aufSuttons
                .hole(feldsuche("Message.ProcessID", processId))
                .<List<String>>json("$.nachrichten[*].messageId"))
        .as("beim eigenen Mandanten ist der Prozess erreichbar — sonst pruefte das nichts")
        .isNotEmpty();
  }

  // ─── Die Abschneidung ─────────────────────────────────────────────────────────

  /**
   * <b>Die Abschneidung wird nach dem Mandantenfilter gezaehlt</b> — dieselbe Suche, zwei
   * Mandanten, zwei verschiedene Zahlen. Der haeufigste Prozess von {@code NEXANS} an einem Tag
   * liegt ueber dem Limit; bei {@code SUTTONS} traegt er null Nachrichten.
   */
  @Test
  @DisplayName("Die Abschneidung wird nach dem Mandantenfilter gezaehlt")
  void abschneidung_wird_nach_dem_mandantenfilter_gezaehlt() throws Exception {
    String prozess = haeufigsterProzessAmTag();
    assertThat(nachrichtenVon(MANDANT_B, prozess))
        .as("traege SUTTONS denselben Prozess, verglichen die Zahlen nicht dieselbe Frage")
        .isZero();

    Antwort nexans = aufNexans.hole(feldsuche(TAG_VON, TAG_BIS, "Message.ProcessID", prozess));
    Antwort suttons = aufSuttons.hole(feldsuche(TAG_VON, TAG_BIS, "Message.ProcessID", prozess));

    assertThat(nexans.<Boolean>json("$.abgeschnitten")).isTrue();
    assertThat(nexans.<List<String>>json("$.nachrichten[*].messageId"))
        .hasSize(BamSucheRepository.HOECHSTENS_TREFFER);
    assertThat(suttons.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(suttons.<Boolean>json("$.abgeschnitten"))
        .as("eine Abschneidung aus den Rohtreffern verriete die Datenmenge des anderen Mandanten")
        .isFalse();
  }

  // ─── Rolle und Kontext ────────────────────────────────────────────────────────

  /** Ohne aktiven Mandanten gibt es keinen Zugriff — auch nicht fuer ADMIN (Regeln M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antwortet der Endpunkt 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "feldsuche-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort antwort = alsAdmin.hole(feldsuche("Message.MessageID", "4711"));

    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
  }

  /**
   * <b>Die Gegenprobe ueber den Mandantenwechsel</b>: als ADMIN zu {@code SUTTONS}, dort eine
   * Eigenschaft holen und nachweisen, dass sie gefunden wird, zurueckwechseln, dieselbe suchen —
   * leer, ununterscheidbar von der erfundenen Eingabe.
   */
  @Test
  @DisplayName("Auch nach einem Mandantenwechsel bleibt der fremde Bestand unerreichbar")
  void gegenprobe_ueber_den_mandantenwechsel() throws Exception {
    String admin = PRAEFIX + "feldsuche-admin";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_B + "\"}").status())
        .isEqualTo(200);
    Eigenschaft fremd = eineSelteneEigenschaftVon(MANDANT_B);
    assertThat(
            alsAdmin
                .hole(feldsuche(fremd.name(), fremd.wert()))
                .<List<String>>json("$.nachrichten[*].messageId"))
        .as("als SUTTONS ist die Eigenschaft erreichbar — sonst prueft die Gegenprobe nichts")
        .contains(fremd.messageId());

    assertThat(alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}").status())
        .isEqualTo(200);

    Antwort nachDemWechsel = alsAdmin.hole(feldsuche(fremd.name(), fremd.wert()));
    Antwort erfunden = alsAdmin.hole(feldsuche(fremd.name(), erfunden(fremd.wert())));

    assertThat(nachDemWechsel.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(vergleichbar(nachDemWechsel, fremd.wert()))
        .isEqualTo(vergleichbar(erfunden, erfunden(fremd.wert())));
  }
}
