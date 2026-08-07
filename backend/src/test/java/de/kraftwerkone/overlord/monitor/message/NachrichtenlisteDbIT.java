package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Der Listen-Endpunkt gegen die Testkopie.
 *
 * <p><b>Das Zeitfenster ist absolut und liegt im dichten Bestand</b> (29.12.2025). Ein relatives
 * Fenster haenge an der Dev-Uhr und damit am Datenstand der Testkopie; ein Test, der bei der
 * naechsten Neubefuellung rot wird, ohne dass sich etwas verschlechtert hat, ist keiner. Die beiden
 * Faelle, die das relative Fenster wirklich pruefen (Vorgabe von 24 Stunden), verlangen deshalb
 * keine bestimmte Zeilenzahl.
 *
 * <p>Die Mandantentrennung selbst prueft {@code NachrichtenIsolationDbIT} — hier geht es um
 * Zeitfenster, Filter, Sortierung und das Blaettern.
 */
class NachrichtenlisteDbIT extends SicherheitsTestbasis {

  private static final String NUTZER = PRAEFIX + "liste-nexans";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Der Mandant mit dem groessten Bestand — hier geht es um Menge, nicht um Trennung. */
  private static final String MANDANT = "NEXANS";

  /** Ein Tag im dichten Bestand: 6.249 Zeilen ueber alle Mandanten (Messung M9). */
  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-29T00:00:00");

  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-30T00:00:00");

  @Autowired private Clock anwendungsuhr;

  /** Nur zum Auffinden einer geeigneten Zeile — der Test soll keine Kennung fest verdrahten. */
  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung sitzung;

  @BeforeEach
  void anmelden() throws IOException, InterruptedException {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  /**
   * Wanduhrzeit der Quelle → UTC-Zeitpunkt der API. Ueber die Zone der Anwendungsuhr, damit der
   * Test dasselbe rechnet wie der Endpunkt und nicht eine feste Zeitzone unterstellt.
   */
  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String fensterAbfrage(String zusatz) {
    return "/api/nachrichten?von="
        + URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8)
        + zusatz;
  }

  @Test
  @DisplayName("Ein Nutzer auf NEXANS bekommt echte Zeilen mit Prozess und Projekt")
  void liefert_echte_zeilen() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=5"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].messageId")).hasSize(5);
    assertThat(antwort.<List<String>>json("$.items[*].processId")).hasSize(5);
    assertThat(antwort.<List<String>>json("$.items[*].status")).isNotEmpty();
    assertThat(antwort.<List<String>>json("$.items[*].statusKind")).isNotEmpty();
    assertThat(antwort.<Boolean>json("$.hasMore")).isTrue();
    assertThat(antwort.<String>json("$.nextCursor")).isNotBlank();
  }

  @Test
  @DisplayName("Der Zeitpunkt geht als ISO 8601 in UTC ueber die Leitung")
  void zeitpunkt_ist_iso_utc() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=1"));

    String zeitpunkt = antwort.<List<String>>json("$.items[*].zeitpunkt").getFirst();
    assertThat(zeitpunkt)
        .as("Kein Epoch-Zahlenwert, keine lokale Zeit — Richtlinie §5.3")
        .endsWith("Z")
        .startsWith("2025-12-2");
  }

  /**
   * <b>Die Zeitkette gegen echte Daten</b> (Aufgabe 11): Wanduhrzeit in {@code GlassfishDB} → die
   * Zone, die das Backend selbst als Anzeigezone nennt → UTC in der Antwort.
   *
   * <p>Der Test unterstellt <b>keine</b> Zeitzone, sondern liest sie aus der Selbstauskunft — genau
   * den Wert, mit dem das Frontend anschliessend zurueckrechnet. Waeren die beiden verschieden,
   * stuende in der Anzeige eine andere Uhrzeit als in der Datenbank, und keine Seite fuer sich
   * saehe falsch aus. Die Rueckrichtung (UTC → Anzeige) belegt {@code
   * frontend/tests/format.test.ts}, die reine Rechnung {@code ZeitpunkteTest}.
   */
  @Test
  @DisplayName("Der Zeitpunkt der Antwort ist die Wanduhrzeit der Datenbank in der Anzeigezone")
  void zeitpunkt_entspricht_der_wanduhrzeit_der_quelle() throws Exception {
    String anzeigezone = sitzung.hole("/api/auth/me").json("$.anzeigezone");
    assertThat(anzeigezone)
        .as("Ohne Anzeigezone kann das Frontend nicht zurueckrechnen")
        .isNotBlank();

    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=1"));
    String messageId = antwort.<List<String>>json("$.items[*].messageId").getFirst();
    String ausDerAntwort = antwort.<List<String>>json("$.items[*].zeitpunkt").getFirst();

    LocalDateTime inDerDatenbank =
        glassfishDsl
            .select(MESSAGE.MESSAGELASTUPDATE)
            .from(MESSAGE)
            .where(MESSAGE.MESSAGEID.eq(messageId))
            .fetchOne(MESSAGE.MESSAGELASTUPDATE);

    assertThat(ausDerAntwort)
        .isEqualTo(
            DateTimeFormatter.ISO_INSTANT.format(
                inDerDatenbank.atZone(java.time.ZoneId.of(anzeigezone)).toInstant()));
  }

  @Test
  @DisplayName("Ohne Zeitfenster greift die Vorgabe von 24 Stunden")
  void ohne_zeitfenster_greift_die_vorgabe() throws Exception {
    Antwort antwort = sitzung.hole("/api/nachrichten?limit=1");

    // Keine Zeilenzahl behauptet: Wie viel im 24-Stunden-Fenster der Dev-Uhr liegt, haengt am
    // Datenstand. Geprueft wird, dass ueberhaupt ohne Zeitangabe gelesen werden darf.
    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.hatFeld("$.items")).isTrue();
  }

  @Test
  @DisplayName("Ein Zeitfenster ueber einem Jahr ist 400")
  void zu_grosses_zeitfenster_ist_400() throws Exception {
    Antwort antwort =
        sitzung.hole(
            "/api/nachrichten?von="
                + URLEncoder.encode(
                    iso(FENSTER_BIS.minusYears(1).minusDays(1)), StandardCharsets.UTF_8)
                + "&bis="
                + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/zeitfenster-zu-gross");
  }

  @Test
  @DisplayName("Zeitraum und Zeitpunkte zugleich sind 400 — keine stille Vorrangregel")
  void beide_zeitmodi_sind_400() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&zeitraum=24h"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/zeitfenster-mehrdeutig");
  }

  @Test
  @DisplayName("Ein unlesbarer Cursor ist 400 und nicht stillschweigend die erste Seite")
  void unlesbarer_cursor_ist_400() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&cursor=nicht-lesbar"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/cursor-ungueltig");
  }

  @Test
  @DisplayName("Ein zu kurzer Suchbegriff ist 400")
  void zu_kurzer_suchbegriff_ist_400() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&suche=ab"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/suchbegriff-zu-kurz");
  }

  @Test
  @DisplayName("Die zweite Seite setzt lueckenlos fort und wiederholt keine Zeile")
  void blaettern_ueberspringt_und_wiederholt_nichts() throws Exception {
    Antwort seite1 = sitzung.hole(fensterAbfrage("&limit=10"));
    List<String> ersteIds = seite1.json("$.items[*].messageId");
    List<String> ersteZeitpunkte = seite1.json("$.items[*].zeitpunkt");

    Antwort seite2 =
        sitzung.hole(
            fensterAbfrage(
                "&limit=10&cursor="
                    + URLEncoder.encode(seite1.json("$.nextCursor"), StandardCharsets.UTF_8)));
    List<String> zweiteIds = seite2.json("$.items[*].messageId");

    assertThat(seite2.status()).isEqualTo(200);
    assertThat(zweiteIds).hasSize(10).doesNotContainAnyElementsOf(ersteIds);
    assertThat(ersteZeitpunkte).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(seite2.<List<String>>json("$.items[*].zeitpunkt").getFirst())
        .as("Die zweite Seite beginnt nicht vor dem Ende der ersten")
        .isLessThanOrEqualTo(ersteZeitpunkte.getLast());
  }

  @Test
  @DisplayName("Aufsteigend sortiert liefert die aeltesten Zeilen zuerst")
  void aufsteigende_sortierung() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=10&sortierung=aelteste"));

    assertThat(antwort.<List<String>>json("$.items[*].zeitpunkt"))
        .isSortedAccordingTo(java.util.Comparator.naturalOrder());
  }

  /**
   * Der Nachweis, dass die Uebersetzung Einordnung → SQL und die Einordnung in der Anzeige aus
   * derselben Quelle stammen: Jede gelieferte Zeile traegt die Einordnung, nach der gefiltert
   * wurde.
   */
  @Test
  @DisplayName("Der Statusfilter liefert ausschliesslich Zeilen dieser Einordnung")
  void statusfilter_ist_deckungsgleich_mit_der_anzeige() throws Exception {
    for (String einordnung : List.of("FEHLER", "ABGESCHLOSSEN", "QUITTIERT", "ZWISCHENSCHRITT")) {
      Antwort antwort =
          sitzung.hole(fensterAbfrage("&limit=50&zwischenschritte=true&status=" + einordnung));

      assertThat(antwort.status()).as("Status %s", einordnung).isEqualTo(200);
      assertThat(antwort.<List<String>>json("$.items[*].statusKind"))
          .as("Status %s", einordnung)
          .allMatch(einordnung::equals);
    }
  }

  @Test
  @DisplayName("Zwischenschritte bleiben ohne ausdrueckliche Anforderung draussen")
  void zwischenschritte_sind_ausgeblendet() throws Exception {
    Antwort ohne = sitzung.hole(fensterAbfrage("&limit=200"));
    assertThat(ohne.<List<String>>json("$.items[*].statusKind"))
        .isNotEmpty()
        .doesNotContain("ZWISCHENSCHRITT");

    Antwort mit = sitzung.hole(fensterAbfrage("&limit=200&zwischenschritte=true"));
    assertThat(mit.<List<String>>json("$.items[*].statusKind"))
        .as("Im dichten Bestand gibt es SPLITTED und MERGED — sonst pruefte der Test nichts")
        .contains("ZWISCHENSCHRITT");
  }

  @Test
  @DisplayName("Der Freitextfilter wirkt ueber die Prozessnamen des Mandanten")
  void freitextfilter_wirkt() throws Exception {
    // Ein Prozessname aus dem Fenster selbst — nicht geraten, sondern gelesen.
    String prozessname =
        sitzung
            .hole(fensterAbfrage("&limit=1"))
            .<List<String>>json("$.items[*].processName")
            .getFirst();
    String begriff = prozessname.substring(0, Math.min(prozessname.length(), 8));

    Antwort antwort =
        sitzung.hole(
            fensterAbfrage(
                "&limit=50&suche=" + URLEncoder.encode(begriff, StandardCharsets.UTF_8)));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].processName"))
        .isNotEmpty()
        .allMatch(name -> name.contains(begriff));
  }

  /**
   * Die Fenstergrenze der Suche (Messung L13). Sie haengt an der <b>Spanne</b>, nicht am Modus, und
   * greift nur bei gesetztem Suchbegriff: Ohne ihn kostet ein langes Fenster nichts (L1 bis L3).
   *
   * <p>Ein Fenster ueber der Grenze wird abgewiesen, <b>bevor</b> {@code Message} angefasst wird —
   * der Test kostet deshalb keine Sekunde Datenbankzeit, obwohl er den teuersten Fall des Endpunkts
   * beschreibt. Aus demselben Grund sucht der Fall, der <i>durchgeht</i>, nach einem Begriff ohne
   * jeden Treffer: Geprueft wird, ob die Anfrage angenommen wird, nicht wie schnell ein bestimmter
   * Begriff antwortet — und ein Begriff, der die Stammdaten trifft, laesst hier einen
   * mehrsekuendigen Durchlauf auf der geteilten Testkopie stehen.
   */
  private static final String BEGRIFF_OHNE_TREFFER = "zzzgibtesganzsichernicht";

  private String langesFenster(long tage, String zusatz) {
    return "/api/nachrichten?limit=1&von="
        + URLEncoder.encode(iso(FENSTER_BIS.minusDays(tage)), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8)
        + zusatz;
  }

  @Test
  @DisplayName("Ein Suchfenster ueber 30 Tagen ist 400 und nennt beide Zahlen")
  void suchfenster_ueber_der_grenze_ist_400() throws Exception {
    Antwort antwort = sitzung.hole(langesFenster(60, "&suche=" + BEGRIFF_OHNE_TREFFER));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/suche-fenster-zu-gross");
    assertThat(antwort.<Integer>json("$.grenzeTage"))
        .as("Ohne diese Zahl muesste die Oberflaeche die Grenze ein zweites Mal kennen")
        .isEqualTo(30);
    assertThat(antwort.<Integer>json("$.angefragtTage")).isEqualTo(60);
  }

  @Test
  @DisplayName("Dasselbe Fenster geht mit langeSuche durch")
  void langesuche_hebt_die_grenze_auf() throws Exception {
    Antwort antwort =
        sitzung.hole(langesFenster(60, "&suche=" + BEGRIFF_OHNE_TREFFER + "&langeSuche=true"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.hatFeld("$.items")).isTrue();
  }

  @Test
  @DisplayName("Ueber der aeusseren Grenze ist es auch mit langeSuche 400")
  void ueber_der_aeusseren_grenze_bleibt_es_400() throws Exception {
    Antwort antwort =
        sitzung.hole(langesFenster(120, "&suche=" + BEGRIFF_OHNE_TREFFER + "&langeSuche=true"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/suche-fenster-zu-gross");
    assertThat(antwort.<Integer>json("$.grenzeTage"))
        .as("Mit langeSuche gilt die groessere Grenze — genannt wird die, die tatsaechlich gilt")
        .isEqualTo(90);
  }

  @Test
  @DisplayName("Ohne Suchbegriff greift die Fenstergrenze der Suche nicht")
  void ohne_suchbegriff_bleibt_das_lange_fenster_erlaubt() throws Exception {
    Antwort antwort = sitzung.hole(langesFenster(120, ""));

    assertThat(antwort.status()).isEqualTo(200);
  }

  @Test
  @DisplayName("Ein Suchbegriff ohne Treffer liefert eine leere Liste, keinen Fehler")
  void suche_ohne_treffer_ist_leer() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&suche=gibtesganzsicherniemals"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].messageId")).isEmpty();
    assertThat(antwort.<Boolean>json("$.hasMore")).isFalse();
  }

  // ─── Der Ablaufname (Nachbesserung zu Schritt 4, Aufgabe 4) ─────────────────────

  /**
   * Die Spalte, die die beiden BAM-Spalten abgeloest hat. Der Anspruch an sie ist genau der, an dem
   * jene gescheitert sind: <b>Sie traegt auf jeder Zeile etwas.</b> Messung L14 weist das fuer alle
   * 180.251 Nachrichten des dichten Monats nach; dieser Test haelt es fuer das Fenster fest, damit
   * ein spaeterer Datenstand die Aussage nicht stillschweigend verliert.
   */
  @Test
  @DisplayName("Jede Zeile traegt einen Ablaufnamen in Klartext")
  void jede_zeile_traegt_einen_ablaufnamen() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=50"));

    List<String> namen = antwort.json("$.items[*].sosName");
    assertThat(namen).isNotEmpty().doesNotContainNull();
    assertThat(namen)
        .as("Klartext, keine Kennung: mindestens sechs Zeichen und nicht nur Ziffern (L14)")
        .allMatch(name -> name.length() >= 6 && !name.matches("\\d+"));
  }

  /**
   * {@code ProcessName} bleibt in der Antwort, obwohl er keine eigene Spalte mehr hat — die
   * Oberflaeche zeigt ihn im Tooltip des Ablaufs. Er ist nur zufaellig lesbar, und genau deshalb
   * traegt die Spalte jetzt {@code SOSName} (Projektbeschreibung §3.2).
   */
  @Test
  @DisplayName("Ablauf- und Prozessname stehen beide in der Antwort, und sie sind verschieden")
  void ablauf_und_prozessname_stehen_beide_da() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=50"));

    assertThat(antwort.<List<String>>json("$.items[*].processName")).isNotEmpty();
    assertThat(antwort.<List<String>>json("$.items[*].projectName")).isNotEmpty();
  }

  /**
   * Die BAM-Werte sind aus der Liste verschwunden, und zwar vollstaendig — kein leeres Feld, das
   * spaeter jemand wieder zu fuellen versucht. {@code bam_spalte} und {@code
   * common/BamSpaltenRegel} bleiben davon unberuehrt; Schritt 7 baut die BAM-Suche darauf.
   */
  @Test
  @DisplayName("Die Antwort traegt kein Feld bamWerte mehr")
  void keine_bam_werte_mehr_in_der_antwort() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=5"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.rumpf()).doesNotContain("bamWerte");
  }
}
