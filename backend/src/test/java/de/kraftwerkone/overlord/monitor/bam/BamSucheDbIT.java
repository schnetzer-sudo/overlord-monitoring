package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.BAM_SOLLAENGE;
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
 * Die BAM-Suche gegen echte Daten — {@code @Tag("db")} über {@link SicherheitsTestbasis}.
 *
 * <p><b>Kein Prüfwert steht in dieser Datei</b> (Regel G1). Jeder wird zur Laufzeit aus der
 * Kuratierung und dem Bestand <i>hergeleitet</i>, nach seiner <b>Gestalt</b>: „ein Wert eines
 * kuratierten Paares, der auf der Sollänge liegt und mit einer Null beginnt". Damit prüft der Test
 * die Eigenschaft und nicht den Datenstand — und er sagt es, wenn die Herleitung nichts findet,
 * statt still grün zu bleiben.
 *
 * <p><b>Das Zeitfenster ist absolut</b> (Fenster B, 30.11. bis 30.12.2025). Außer {@code NEXANS}
 * endet jeder Mandant am 30.12.2025 (M3); in einem relativen Fenster hinge der Test am Datenstand.
 */
class BamSucheDbIT extends SicherheitsTestbasis {

  private static final String MANDANT_NEXANS = "NEXANS";
  private static final String NUTZER = PRAEFIX + "bamsuche-nexans";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Fenster B aus {@code messungen-schritt7.md} §0 — dieselbe Spanne wie die Vorgabe: 30 Tage. */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-11-30T00:00:00");

  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung aufNexans;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(MANDANT_NEXANS))
        .as("Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code")
        .isTrue();
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT_NEXANS);
    aufNexans = anmelden(NUTZER, PASSWORT);
  }

  // ─── Herleitung der Pruefwerte ────────────────────────────────────────────────

  /** Ein Wert aus dem Bestand samt seiner Nachricht — nach Gestalt gefunden, nicht eingetragen. */
  private record Pruefwert(short typ, String wert, String messageId) {

    /** Derselbe Wert ohne seine führenden Nullen — das, was der Nutzer vom Beleg abtippt. */
    String ohneFuehrendeNullen() {
      int i = 0;
      while (i < wert.length() - 1 && wert.charAt(i) == '0') {
        i++;
      }
      return wert.substring(i);
    }
  }

  /**
   * Der erste Wert eines <b>wirksamen</b> kuratierten Paares: einer, der auf der Sollänge liegt und
   * mit einer Null beginnt.
   *
   * <p>Durchsucht werden die kuratierten Paare des Mandanten in ihrer Reihenfolge — nicht ein
   * namentlich genannter Typ. Zwei der vierzehn Einträge können nachweislich nichts finden ({@code
   * IBIS}/1 und {@code SUTTONS}/2000, M46‑1c); bei {@code NEXANS} sind alle wirksam, und der Test
   * nimmt trotzdem den ersten, der <i>tatsächlich</i> etwas liefert.
   */
  private Pruefwert wirksamesPaar() {
    var kuratiert =
        glassfishDsl
            .select(BAM_SOLLAENGE.MESSAGE_BAM_TYPE, BAM_SOLLAENGE.SOLLAENGE)
            .from(BAM_SOLLAENGE)
            .where(BAM_SOLLAENGE.MANDANT_ID.eq(MANDANT_NEXANS))
            .and(BAM_SOLLAENGE.SOLLAENGE.isNotNull())
            .orderBy(BAM_SOLLAENGE.MESSAGE_BAM_TYPE)
            .fetch();

    assertThat(kuratiert)
        .as("Ohne kuratierte Sollaenge fuer %s prueft dieser Test nichts", MANDANT_NEXANS)
        .isNotEmpty();

    for (var zeile : kuratiert) {
      short typ = zeile.get(BAM_SOLLAENGE.MESSAGE_BAM_TYPE);
      int sollaenge = zeile.get(BAM_SOLLAENGE.SOLLAENGE).intValue();
      Record fund =
          glassfishDsl
              .resultQuery(
                  """
                  select b.MessageBAMValue as wert, b.MessageID as kennung
                  from MessageBAM b
                  join Message m         on m.MessageID  = b.MessageID
                  join Process p         on p.ProcessID  = m.ProcessID
                  join ProjectMandant pm on pm.ProjectID = p.ProjectID
                  where b.MessageBAMType   = ?
                    and pm.MandantID       = ?
                    and b.MessageBAMValue like '0%'
                    and char_length(b.MessageBAMValue) = ?
                    and m.MessageLastUpdate >= ?
                    and m.MessageLastUpdate <  ?
                  order by b.MessageBAMValue, b.MessageID
                  limit 1
                  """,
                  typ, MANDANT_NEXANS, sollaenge, FENSTER_VON, FENSTER_BIS)
              .fetchOne();
      if (fund != null) {
        return new Pruefwert(
            typ, fund.get("wert", String.class), fund.get("kennung", String.class));
      }
    }
    throw new AssertionError(
        "Kein kuratiertes Paar von "
            + MANDANT_NEXANS
            + " traegt im Fenster einen Wert mit fuehrender Null auf seiner Sollaenge."
            + " Dann hat sich der Bestand geaendert — siehe docs/bam-sollaengen.md §5.");
  }

  // ─── Aufrufe ──────────────────────────────────────────────────────────────────

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String suche(String... begriffe) {
    StringBuilder pfad = new StringBuilder("/api/bam/suche?von=");
    pfad.append(URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8));
    pfad.append("&bis=").append(URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8));
    for (String begriff : begriffe) {
      pfad.append("&begriff=").append(URLEncoder.encode(begriff, StandardCharsets.UTF_8));
    }
    return pfad.toString();
  }

  // ─── Die Normalisierung wirkt ─────────────────────────────────────────────────

  /**
   * <b>Das Abnahmekriterium des Teils.</b> Eine Suche <i>ohne</i> führende Null bei einem wirksamen
   * Paar findet die Nachricht, und die Antwort nennt die aufgefüllte Variante.
   *
   * <p>M43‑4 misst, warum das nötig ist: Bei sechs von acht geprüften Typen findet die rohe Fassung
   * <b>null</b> Treffer und die aufgefüllte die richtigen.
   */
  @Test
  @DisplayName("Eine Suche ohne fuehrende Null findet die Nachricht — und die Antwort sagt warum")
  void ohne_fuehrende_null_wird_gefunden() throws Exception {
    Pruefwert pruefwert = wirksamesPaar();
    assertThat(pruefwert.ohneFuehrendeNullen())
        .as("die Herleitung hat einen Wert MIT fuehrender Null gefunden")
        .isNotEqualTo(pruefwert.wert());

    Antwort antwort =
        aufNexans.hole(suche(pruefwert.typ() + ":" + pruefwert.ohneFuehrendeNullen()));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId"))
        .as("die Nachricht, auf der der Wert steht, wird gefunden")
        .contains(pruefwert.messageId());
    assertThat(antwort.<List<String>>json("$.begriffe[0].varianten"))
        .as("keine stille Korrektur — die aufgefuellte Fassung steht in der Antwort")
        .containsExactly(pruefwert.ohneFuehrendeNullen(), pruefwert.wert());
    assertThat(antwort.<List<String>>json("$.nachrichten[*].treffer[*].wert"))
        .as("der Treffer nennt den Wert, wie er im Bestand steht")
        .contains(pruefwert.wert());
  }

  /**
   * <b>Die Trefferzeile trägt die Felder der Nachrichtenliste, die Rollen und den Treffer.</b> Die
   * Rollen kosten keinen zusätzlichen Zugriff (E4) und sind der Unterschied zwischen „ich habe die
   * Nachricht" und „ich habe das Bündel".
   */
  @Test
  @DisplayName("Die Trefferzeile traegt Listenfelder, rollen und treffer")
  void die_trefferzeile_ist_vollstaendig() throws Exception {
    Pruefwert pruefwert = wirksamesPaar();

    Antwort antwort = aufNexans.hole(suche(":" + pruefwert.wert()));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId")).isNotEmpty();
    for (String feld :
        List.of(
            "messageId", "zeitpunkt", "status", "statusKind", "processId", "rollen", "treffer")) {
      assertThat(antwort.hatFeld("$.nachrichten[0]." + feld))
          .as("Feld %s fehlt in der Trefferzeile", feld)
          .isTrue();
    }
    assertThat(antwort.<List<String>>json("$.nachrichten[*].zeitpunkt"))
        .allSatisfy(zeitpunkt -> assertThat(zeitpunkt).endsWith("Z"));
    assertThat(antwort.<Integer>json("$.nachrichten[0].treffer.length()")).isPositive();
  }

  /**
   * <b>Das verwendete Fenster steht in der Antwort</b> — es verändert die Antwort, nicht nur den
   * Preis.
   */
  @Test
  @DisplayName("Die Antwort nennt das verwendete Zeitfenster, auch wenn es die Vorgabe war")
  void die_antwort_nennt_das_fenster() throws Exception {
    Pruefwert pruefwert = wirksamesPaar();

    Antwort mitFenster = aufNexans.hole(suche(":" + pruefwert.wert()));
    assertThat(mitFenster.<String>json("$.von")).isEqualTo(iso(FENSTER_VON));
    assertThat(mitFenster.<String>json("$.bis")).isEqualTo(iso(FENSTER_BIS));

    Antwort ohneFenster =
        aufNexans.hole(
            "/api/bam/suche?begriff="
                + URLEncoder.encode(":" + pruefwert.wert(), StandardCharsets.UTF_8));
    assertThat(ohneFenster.status()).isEqualTo(200);
    assertThat(ohneFenster.hatFeld("$.von")).as("auch die Vorgabe wird genannt").isTrue();
    assertThat(ohneFenster.hatFeld("$.bis")).isTrue();
  }

  // ─── Die Verundung ────────────────────────────────────────────────────────────

  /**
   * <b>Zwei Begriffe liefern nur Nachrichten, auf denen beide stehen.</b> Geprüft wird die
   * Eigenschaft und nicht eine Zeilenzahl: Jede Trefferzeile muss beide gesuchten Werte unter ihren
   * {@code treffer} führen.
   */
  @Test
  @DisplayName("Zwei Begriffe liefern nur Nachrichten, auf denen beide stehen")
  void zwei_begriffe_werden_verundet() throws Exception {
    Pruefwert pruefwert = wirksamesPaar();

    // Ein zweiter Wert von DERSELBEN Nachricht — sonst misst der Test den billigen Fall
    // „leeres Ergebnis" (dieselbe Ueberlegung wie bei der Auswahl der Pruefwerte in M42-0).
    String zweiter =
        glassfishDsl
            .resultQuery(
                """
                select b.MessageBAMValue
                from MessageBAM b
                where b.MessageID = ?
                  and b.MessageBAMValue <> ?
                order by b.MessageBAMValue
                limit 1
                """,
                pruefwert.messageId(),
                pruefwert.wert())
            .fetchOne(0, String.class);
    assertThat(zweiter)
        .as("Die Bezugsnachricht traegt nur einen einzigen BAM-Wert — dann misst der Test nichts")
        .isNotNull();

    Antwort antwort = aufNexans.hole(suche(":" + pruefwert.wert(), ":" + zweiter));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId"))
        .contains(pruefwert.messageId());

    int anzahl = antwort.<List<String>>json("$.nachrichten[*].messageId").size();
    for (int i = 0; i < anzahl; i++) {
      assertThat(antwort.<List<String>>json("$.nachrichten[" + i + "].treffer[*].wert"))
          .as("jede Trefferzeile traegt BEIDE gesuchten Werte")
          .contains(pruefwert.wert(), zweiter);
    }
  }

  /**
   * <b>Ein Wert, den es nicht gibt, ist eine leere Liste und kein Fehler.</b> Genau den tippt ein
   * Nutzer, der sich vertut — und M42 misst diesen Fall ausdrücklich <b>nicht</b> (offener Punkt).
   */
  @Test
  @DisplayName("Ein Wert ohne Treffer ist eine leere Liste mit 200")
  void ohne_treffer_leere_liste() throws Exception {
    Antwort antwort = aufNexans.hole(suche(":kein-solcher-bam-wert-4711"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId")).isEmpty();
    assertThat(antwort.<Boolean>json("$.abgeschnitten")).isFalse();
    assertThat(antwort.<List<String>>json("$.begriffe[*].eingabe"))
        .containsExactly("kein-solcher-bam-wert-4711");
  }

  // ─── Die Parametergrenzen am laufenden Endpunkt ───────────────────────────────

  @Test
  @DisplayName("Ein Begriff ohne Doppelpunkt ist 400 mit eigenem Problemtyp")
  void ohne_trenner_ist_400() throws Exception {
    Antwort antwort =
        aufNexans.hole(
            "/api/bam/suche?begriff=" + URLEncoder.encode("4711815", StandardCharsets.UTF_8));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/suchbegriff-ohne-typtrenner");
  }

  @Test
  @DisplayName("Neun Begriffe sind 400")
  void neun_begriffe_sind_400() throws Exception {
    StringBuilder pfad = new StringBuilder("/api/bam/suche?begriff=%3Aa");
    for (int i = 1; i <= BamSuchfilter.HOECHSTENS_BEGRIFFE; i++) {
      pfad.append("&begriff=%3Awert").append(i);
    }

    Antwort antwort = aufNexans.hole(pfad.toString());

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/zu-viele-suchbegriffe");
  }

  @Test
  @DisplayName("Ohne Begriff ist die Suche 400 und keine leere Antwort")
  void ohne_begriff_ist_400() throws Exception {
    Antwort antwort = aufNexans.hole("/api/bam/suche");

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/suchbegriff-fehlt");
  }

  /** Das Fenstermaximum ist das aus Regel L1 und wird nicht neu erfunden. */
  @Test
  @DisplayName("Ein Fenster ueber einem Jahr ist 400 — dieselbe Grenze wie in der Liste")
  void fenstermaximum_ist_ein_jahr() throws Exception {
    Antwort antwort =
        aufNexans.hole(
            "/api/bam/suche?begriff=%3A4711815&von="
                + URLEncoder.encode(iso(FENSTER_BIS.minusYears(2)), StandardCharsets.UTF_8)
                + "&bis="
                + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/zeitfenster-zu-gross");
  }
}
