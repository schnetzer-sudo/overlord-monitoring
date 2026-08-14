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

  /** Sollänge, ab der ein Prüfwert für den Präfixmodus taugt — siehe die Herleitung unten. */
  private static final int MINDESTE_SOLLAENGE = 8;

  /** Und wie viele Zeichen sein Kern mindestens haben muss, damit der Präfix selektiv bleibt. */
  private static final int MINDESTER_KERN = 7;

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
    return sucheMitModus(null, begriffe);
  }

  private String sucheMitModus(String modus, String... begriffe) {
    StringBuilder pfad = new StringBuilder("/api/bam/suche?von=");
    pfad.append(URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8));
    pfad.append("&bis=").append(URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8));
    for (String begriff : begriffe) {
      pfad.append("&begriff=").append(URLEncoder.encode(begriff, StandardCharsets.UTF_8));
    }
    if (modus != null) {
      pfad.append("&modus=").append(URLEncoder.encode(modus, StandardCharsets.UTF_8));
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

  // ─── Der Praefixmodus (Teil 4) ────────────────────────────────────────────────

  /**
   * <b>Der Präfixfall braucht einen <i>langen</i> Prüfwert, und das ist keine Bequemlichkeit.</b>
   *
   * <p>Ein Präfix ist umso weniger selektiv, je kürzer er ist — M49‑2a misst für vier Zeichen
   * schlimmstenfalls 1.332.180 Zeilen. Eine kurze Eingabe liefe hier ins harte Limit von 50, und
   * der Test schlüge fehl, weil die gesuchte Nachricht <i>zu weit hinten</i> steht und nicht, weil
   * der Präfixmodus kaputt ist. Verlangt werden deshalb <b>Sollänge ≥ 8</b> und ein <b>Kern ≥ 7
   * Zeichen</b>; gesucht wird der <i>größte</i> Wert mit führender Null, weil der die wenigsten
   * Nullen und damit den längsten Kern trägt.
   *
   * <p>Findet die Herleitung nichts, wird der Test rot und sagt es — statt still grün zu bleiben.
   */
  private Pruefwert langerWertMitFuehrenderNull() {
    var kuratiert =
        glassfishDsl
            .select(BAM_SOLLAENGE.MESSAGE_BAM_TYPE, BAM_SOLLAENGE.SOLLAENGE)
            .from(BAM_SOLLAENGE)
            .where(BAM_SOLLAENGE.MANDANT_ID.eq(MANDANT_NEXANS))
            .and(BAM_SOLLAENGE.SOLLAENGE.isNotNull())
            .orderBy(BAM_SOLLAENGE.MESSAGE_BAM_TYPE)
            .fetch();

    for (var zeile : kuratiert) {
      short typ = zeile.get(BAM_SOLLAENGE.MESSAGE_BAM_TYPE);
      int sollaenge = zeile.get(BAM_SOLLAENGE.SOLLAENGE).intValue();
      if (sollaenge < MINDESTE_SOLLAENGE) {
        continue;
      }
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
                  order by b.MessageBAMValue desc, b.MessageID
                  limit 1
                  """,
                  typ, MANDANT_NEXANS, sollaenge, FENSTER_VON, FENSTER_BIS)
              .fetchOne();
      if (fund == null) {
        continue;
      }
      Pruefwert pruefwert =
          new Pruefwert(typ, fund.get("wert", String.class), fund.get("kennung", String.class));
      if (pruefwert.ohneFuehrendeNullen().length() >= MINDESTER_KERN) {
        return pruefwert;
      }
    }
    throw new AssertionError(
        "Kein kuratiertes Paar von "
            + MANDANT_NEXANS
            + " traegt im Fenster einen hinreichend langen Wert mit fuehrender Null auf seiner"
            + " Sollaenge (Sollaenge >= "
            + MINDESTE_SOLLAENGE
            + ", Kern >= "
            + MINDESTER_KERN
            + "). Dann hat sich der Bestand geaendert — siehe docs/bam-sollaengen.md §5.");
  }

  /**
   * <b>Das Abnahmekriterium von Teil 4.</b> Eine verkürzte Eingabe findet im Präfixmodus die
   * Nachricht, die die exakte Suche nicht findet — und zwar <i>über die führenden Nullen
   * hinweg</i>.
   *
   * <p>Das ist der Fall, den M49‑1 als unlösbar mit der alten Variantenbildung gemessen hat: Die
   * auf die Sollänge aufgefüllte Fassung ankert vorn und trifft als Präfix nichts. Getroffen wird
   * über eine der Nullen-im-Muster-Fassungen — und die entstehen nur, weil der Begriff einen
   * <b>Typ</b> trägt.
   */
  @Test
  @DisplayName("Praefix findet mit verkuerzter Eingabe, was exakt nicht findet")
  void praefix_findet_was_exakt_nicht_findet() throws Exception {
    Pruefwert pruefwert = langerWertMitFuehrenderNull();
    String kern = pruefwert.ohneFuehrendeNullen();
    String eingabe = kern.substring(0, kern.length() - 1);

    Antwort exakt = aufNexans.hole(suche(pruefwert.typ() + ":" + eingabe));
    assertThat(exakt.status()).isEqualTo(200);
    assertThat(exakt.<List<String>>json("$.nachrichten[*].messageId"))
        .as("exakt kann eine verkuerzte Nummer nicht finden — dafuer gibt es diesen Teil")
        .doesNotContain(pruefwert.messageId());

    Antwort praefix = aufNexans.hole(sucheMitModus("praefix", pruefwert.typ() + ":" + eingabe));
    assertThat(praefix.status()).isEqualTo(200);
    assertThat(praefix.<Boolean>json("$.abgeschnitten"))
        .as("waere die Antwort gedeckelt, sagte ein fehlender Treffer nichts ueber den Modus")
        .isFalse();
    assertThat(praefix.<List<String>>json("$.nachrichten[*].messageId"))
        .as("ueber die fuehrenden Nullen hinweg gefunden (M49-1)")
        .contains(pruefwert.messageId());
    assertThat(praefix.<String>json("$.modus")).isEqualTo("PRAEFIX");
    assertThat(praefix.<List<String>>json("$.begriffe[0].varianten"))
        .as("die gemeldeten Fassungen sind lesbare Werte, keine Muster")
        .hasSizeGreaterThan(1)
        .noneMatch(fassung -> fassung.endsWith("%"))
        .allSatisfy(fassung -> assertThat(fassung).endsWith(eingabe));
  }

  /**
   * <b>Ein Begriff ohne Typ erzeugt genau eine Fassung</b> — und findet trotzdem, weil der rohe
   * Anfang genügt, wenn die Nullen mitgetippt sind.
   */
  @Test
  @DisplayName("Ohne Typ erzeugt der Praefixmodus genau eine Fassung")
  void ohne_typ_genau_eine_fassung() throws Exception {
    Pruefwert pruefwert = langerWertMitFuehrenderNull();
    String anfang = pruefwert.wert().substring(0, pruefwert.wert().length() - 1);

    Antwort antwort = aufNexans.hole(sucheMitModus("praefix", ":" + anfang));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.begriffe[0].varianten"))
        .as("ohne Typ gibt es keine Sollaenge, die man bedienen koennte, ohne alle zu bedienen")
        .containsExactly(anfang);
    assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId"))
        .as("der rohe Anfang findet die Nachricht — die Nullen sind ja mitgetippt")
        .contains(pruefwert.messageId());
  }

  /**
   * <b>Ein {@code %} oder {@code _} in der Eingabe wird literal gesucht und nicht als
   * Platzhalter.</b>
   *
   * <p>Die Prüfung ist ein <b>Paar</b>, weil eine leere Antwort allein nichts belegte: Dieselbe
   * Eingabe <i>mit</i> dem echten Zeichen findet die Nachricht, mit dem Sonderzeichen an derselben
   * Stelle nicht. <b>Ohne {@code ESCAPE} fände die zweite sie ebenfalls</b> — das ist die Falle aus
   * Regel Q1, und M49‑4 hat gezählt, dass 2.696 Werte des Bestands ein {@code _} tragen.
   */
  @Test
  @DisplayName("Prozent und Unterstrich der Eingabe werden literal gesucht")
  void sonderzeichen_werden_literal_gesucht() throws Exception {
    Pruefwert pruefwert = langerWertMitFuehrenderNull();
    String anfang = pruefwert.wert().substring(0, pruefwert.wert().length() - 1);
    String stumpf = anfang.substring(0, anfang.length() - 1);

    assertThat(
            aufNexans
                .hole(sucheMitModus("praefix", ":" + anfang))
                .<List<String>>json("$.nachrichten[*].messageId"))
        .as("die Gegenprobe: mit dem echten Zeichen an dieser Stelle wird gefunden")
        .contains(pruefwert.messageId());

    for (String sonderzeichen : List.of("_", "%")) {
      String eingabe = stumpf + sonderzeichen;
      Antwort antwort = aufNexans.hole(sucheMitModus("praefix", ":" + eingabe));

      assertThat(antwort.status()).isEqualTo(200);
      assertThat(antwort.<List<String>>json("$.nachrichten[*].messageId"))
          .as("%s wird als Zeichen gesucht, nicht als Platzhalter", sonderzeichen)
          .doesNotContain(pruefwert.messageId());
      assertThat(antwort.<List<String>>json("$.begriffe[*].eingabe"))
          .as("das Zitat bleibt ein Zitat — auch mit Sonderzeichen")
          .containsExactly(eingabe);
    }
  }

  /**
   * <b>Die Vorgabe ist exakt, und sie ändert nichts.</b> Ohne {@code modus}-Parameter liefert der
   * Endpunkt Byte für Byte dieselbe Antwort wie mit {@code modus=exakt} — die Zusage, unter der
   * Teil 4 gebaut ist.
   */
  @Test
  @DisplayName("Ohne modus-Parameter ist die Antwort die von modus=exakt")
  void ohne_modus_ist_die_antwort_die_von_exakt() throws Exception {
    Pruefwert pruefwert = wirksamesPaar();

    Antwort ohne = aufNexans.hole(suche(":" + pruefwert.wert()));
    Antwort mitExakt = aufNexans.hole(sucheMitModus("exakt", ":" + pruefwert.wert()));

    assertThat(ohne.status()).isEqualTo(200);
    assertThat(ohne.rumpf()).isEqualTo(mitExakt.rumpf());
    assertThat(ohne.<String>json("$.modus")).isEqualTo("EXAKT");
  }

  /**
   * <b>Zweig B aus M50.</b> Über ein Jahr ist der schlimmste bekannte Präfix des Bestands an der
   * 60‑Sekunden-Grenze abgebrochen; über 30 Tage kostet er 3,851 s. Der Präfixmodus ist deshalb auf
   * 30 Tage gedeckelt — die exakte Suche behält ihr Jahresmaximum.
   */
  @Test
  @DisplayName("Ein Fenster ueber 30 Tagen ist im Praefixmodus 400, im exakten nicht")
  void praefix_ist_auf_dreissig_tage_gedeckelt() throws Exception {
    String halbesJahr =
        "/api/bam/suche?begriff=%3A4711815&von="
            + URLEncoder.encode(iso(FENSTER_BIS.minusMonths(6)), StandardCharsets.UTF_8)
            + "&bis="
            + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8);

    Antwort mitPraefix = aufNexans.hole(halbesJahr + "&modus=praefix");
    assertThat(mitPraefix.status()).isEqualTo(400);
    assertThat(mitPraefix.<String>json("$.type")).endsWith("/praefixsuche-fenster-zu-gross");
    assertThat(mitPraefix.<Integer>json("$.grenzeTage"))
        .as("die Grenze steht im Rumpf, damit die Oberflaeche sie nicht ein zweites Mal kennt")
        .isEqualTo(30);
    assertThat(mitPraefix.<Integer>json("$.angefragtTage")).isGreaterThan(30);

    assertThat(aufNexans.hole(halbesJahr).status())
        .as("dieselbe Anfrage exakt bleibt gueltig — der Deckel gilt nur dem neuen Pfad")
        .isEqualTo(200);
  }

  @Test
  @DisplayName("Ein unbekannter Modus ist 400 mit eigenem Problemtyp")
  void unbekannter_modus_ist_400() throws Exception {
    Antwort antwort = aufNexans.hole(sucheMitModus("prefix", ":4711815"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/suchmodus-ungueltig");
  }

  // ─── Das Komma im Wert (behoben am 14.08.2026) ────────────────────────────────

  /**
   * Ein {@code NEXANS}-Wert des Fensters, der ein <b>Komma</b> trägt — und zwar nicht an letzter
   * Stelle.
   *
   * <p><b>Warum das Komma nicht hinten stehen darf.</b> Der Präfixfall unten schneidet das letzte
   * Zeichen ab; säße dort das Komma, prüfte er genau das nicht mehr, worum es geht. Die Bedingung
   * steht deshalb in der Abfrage ({@code LIKE '%,_%'}) und nicht als Hoffnung daneben.
   *
   * <p><b>Genommen wird der längste Kandidat</b>, aus demselben Grund wie in {@link
   * #langerWertMitFuehrenderNull()}: Je länger der Wert, desto selektiver der Präfix und desto
   * sicherer bleibt die Antwort unter dem harten Limit.
   *
   * <p>Findet die Herleitung nichts, wird der Test rot und sagt es — statt still grün zu bleiben.
   */
  private Pruefwert wertMitKomma() {
    Record fund =
        glassfishDsl
            .resultQuery(
                """
                select b.MessageBAMType  as typ,
                       b.MessageBAMValue as wert,
                       b.MessageID       as kennung
                from Message m
                join MessageBAM b      on b.MessageID  = m.MessageID
                join Process p         on p.ProcessID  = m.ProcessID
                join ProjectMandant pm on pm.ProjectID = p.ProjectID
                where pm.MandantID = ?
                  and m.MessageLastUpdate >= ?
                  and m.MessageLastUpdate <  ?
                  and b.MessageBAMValue like '%,_%'
                order by char_length(b.MessageBAMValue) desc, b.MessageBAMValue, b.MessageID
                limit 1
                """,
                MANDANT_NEXANS, FENSTER_VON, FENSTER_BIS)
            .fetchOne();

    assertThat(fund)
        .as(
            "Kein Wert von %s traegt im Fenster ein Komma vor dem letzten Zeichen. Dann hat sich"
                + " der Bestand geaendert — M51 zaehlt 55.989 solcher Werte ueber den Bestand,"
                + " davon 54.096 unter Typ 9003.",
            MANDANT_NEXANS)
        .isNotNull();

    return new Pruefwert(
        fund.get("typ", Short.class),
        fund.get("wert", String.class),
        fund.get("kennung", String.class));
  }

  /**
   * <b>Das Abnahmekriterium dieses Auftrags.</b> Ein Wert mit Komma wird gefunden — exakt
   * <i>und</i> über den Anfang.
   *
   * <p><b>Dieser Test hat vorher das Gegenteil behauptet.</b> Er hieß {@code
   * ein_komma_im_wert_ist_heute_400} und hielt fest, dass Spring den wiederholbaren Parameter am
   * Komma zerlegt: Der zweite Teil trug dann keinen Pflichttrenner mehr, und die Antwort war {@code
   * 400 suchbegriff-ohne-typtrenner}. Der Defekt stammte aus der Parameterform von Teil 2b, betraf
   * <b>beide</b> Modi und ist am 14.08.2026 behoben worden ({@code
   * BamSucheController#einParameterIstEinBegriff}). Der alte Name durfte nicht stehen bleiben — er
   * behauptete den Ist-Zustand als Sollzustand.
   *
   * <p><b>Der Präfixfall schneidet ein Zeichen ab</b>, damit er nicht derselbe Vergleich in anderer
   * Schreibweise ist. Geprüft wird zusätzlich, dass die Antwort nicht gedeckelt ist: Wäre sie es,
   * sagte ein fehlender Treffer nichts über die Bindung.
   */
  @Test
  @DisplayName("Ein Komma im Wert wird gefunden — exakt und im Praefixmodus")
  void ein_komma_im_wert_wird_gefunden() throws Exception {
    Pruefwert pruefwert = wertMitKomma();
    assertThat(pruefwert.wert())
        .as("die Herleitung hat einen Wert MIT Komma gefunden")
        .contains(",");

    Antwort exakt = aufNexans.hole(suche(pruefwert.typ() + ":" + pruefwert.wert()));
    assertThat(exakt.status())
        .as("frueher war das 400 suchbegriff-ohne-typtrenner: %s", exakt.rumpf())
        .isEqualTo(200);
    assertThat(exakt.<List<String>>json("$.begriffe[*].eingabe"))
        .as("ein Parameter, ein Begriff — das Komma ist Teil des Werts")
        .containsExactly(pruefwert.wert());
    assertThat(exakt.<List<String>>json("$.nachrichten[*].messageId"))
        .contains(pruefwert.messageId());

    String anfang = pruefwert.wert().substring(0, pruefwert.wert().length() - 1);
    assertThat(anfang).as("der Praefix traegt das Komma noch").contains(",");

    Antwort praefix = aufNexans.hole(sucheMitModus("praefix", pruefwert.typ() + ":" + anfang));
    assertThat(praefix.status()).isEqualTo(200);
    assertThat(praefix.<Boolean>json("$.abgeschnitten"))
        .as("waere die Antwort gedeckelt, sagte ein fehlender Treffer nichts ueber die Bindung")
        .isFalse();
    assertThat(praefix.<List<String>>json("$.begriffe[*].eingabe")).containsExactly(anfang);
    assertThat(praefix.<List<String>>json("$.nachrichten[*].messageId"))
        .as("der Defekt lag vor dem Modus und ist deshalb in beiden behoben")
        .contains(pruefwert.messageId());
  }

  /**
   * <b>Zwei getrennte Parameter bleiben zwei Begriffe — auch wenn einer ein Komma trägt.</b> Das
   * ist die Gegenprobe zur Behebung: Sie hat die Zerlegung <i>innerhalb</i> eines Werts abgestellt
   * und nicht die Mehrfachsetzung des Parameters.
   *
   * <p>Der zweite Begriff ist ausgedacht und findet nichts; das ist Absicht. Geprüft wird die
   * <b>Zerlegung</b>, und die steht in {@code begriffe} — nicht in der Trefferliste.
   */
  @Test
  @DisplayName("Ein Begriff mit Komma und einer ohne ergeben zwei Begriffe")
  void komma_und_zweiter_begriff_ergeben_zwei_begriffe() throws Exception {
    Pruefwert pruefwert = wertMitKomma();

    Antwort antwort = aufNexans.hole(suche(":" + pruefwert.wert(), ":kein-solcher-bam-wert-4711"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.begriffe[*].eingabe"))
        .as("zwei Parameter, zwei Begriffe — und das Komma hat keinen dritten erzeugt")
        .containsExactly(pruefwert.wert(), "kein-solcher-bam-wert-4711");
  }

  /**
   * <b>Die Änderung greift nur an diesem Endpunkt</b> — hier am laufenden System nachgewiesen.
   *
   * <p>Die Nachrichtenliste bindet {@code status} und {@code prozess} weiterhin so, wie {@code
   * begriff} es bis zum 14.08.2026 tat: Ein einzeln gesetzter Parameter wird am Komma zerlegt. Zwei
   * gültige Einordnungen in <i>einem</i> Parameter ergeben dort deshalb {@code 200} und nicht
   * {@code 400 status-unbekannt} — genau das misst dieser Test.
   *
   * <p><b>Das ist kein Mangel, sondern der Schnitt des Auftrags.</b> Ob dort überhaupt kommahaltige
   * Werte vorkommen können, steht in {@code docs/bam-suche.md} §9: Einordnungen sind
   * Aufzählungsnamen, {@code prozess} ist eine {@code ProcessID}. <b>Fällt dieser Test eines Tages
   * um, ist die Bindung dort geändert worden</b> — dann gehört der Abschnitt fortgeschrieben und
   * nicht der Test gelöscht.
   */
  @Test
  @DisplayName("Die Nachrichtenliste trennt weiterhin am Komma — die Aenderung ist eng geschnitten")
  void die_nachrichtenliste_trennt_weiterhin_am_komma() throws Exception {
    Antwort antwort = aufNexans.hole("/api/nachrichten?zeitraum=24h&status=FEHLER,ABGESCHLOSSEN");

    assertThat(antwort.status())
        .as(
            "getrennt am Komma sind das zwei gueltige Einordnungen; ungetrennt waere es eine"
                + " unbekannte und damit 400 status-unbekannt. Rumpf: %s",
            antwort.rumpf())
        .isEqualTo(200);
  }
}
