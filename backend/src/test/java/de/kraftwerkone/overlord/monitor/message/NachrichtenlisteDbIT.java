package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGE;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.jooq.Record2;
import org.jooq.impl.DSL;
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

  @Test
  @DisplayName("Ein Suchbegriff ohne Treffer liefert eine leere Liste, keinen Fehler")
  void suche_ohne_treffer_ist_leer() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&suche=gibtesganzsicherniemals"));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].messageId")).isEmpty();
    assertThat(antwort.<Boolean>json("$.hasMore")).isFalse();
  }

  // ─── BAM-Werte (Aufgabe 7) ──────────────────────────────────────────────────────────────────

  /**
   * {@code NEXANS} ist der Mandant, fuer den die Kuratierung angelegt wurde: Der Sortierindex
   * zeigte auf „Abladestelle" (millionenfach derselbe Wert), kuratiert sind Lieferschein-Nr. und
   * Abrufnummer. Geprueft wird die <b>Reihenfolge</b> und dass jede Zeile beide Spalten traegt —
   * auch die, die dazu nichts zu sagen hat.
   */
  @Test
  @DisplayName("Jede Zeile traegt beide BAM-Spalten des Mandanten, in kuratierter Reihenfolge")
  void bam_spalten_stehen_an_jeder_zeile() throws Exception {
    Antwort antwort = sitzung.hole(fensterAbfrage("&limit=20"));

    assertThat(antwort.<List<List<Object>>>json("$.items[*].bamWerte"))
        .isNotEmpty()
        .allMatch(werte -> werte.size() == 2);
    assertThat(antwort.<List<Integer>>json("$.items[0].bamWerte[*].typ"))
        .containsExactly(9006, 9001);
    assertThat(antwort.<List<String>>json("$.items[*].bamWerte[*].beschreibung"))
        .allMatch(beschreibung -> beschreibung != null && !beschreibung.isBlank());
  }

  @Test
  @DisplayName("Mehr als drei Werte eines Typs werden gekuerzt und die Kuerzung wird benannt")
  void mehr_als_drei_werte_werden_gekuerzt() throws Exception {
    Record2<String, LocalDateTime> vielwertig =
        glassfishDsl
            .select(MESSAGEBAM.MESSAGEID, MESSAGE.MESSAGELASTUPDATE)
            .from(MESSAGEBAM)
            .join(MESSAGE)
            .on(MESSAGE.MESSAGEID.eq(MESSAGEBAM.MESSAGEID))
            .where(MESSAGE.MESSAGELASTUPDATE.between(FENSTER_VON, FENSTER_BIS))
            .and(MESSAGEBAM.MESSAGEBAMTYPE.eq((short) 9006))
            .groupBy(MESSAGEBAM.MESSAGEID, MESSAGE.MESSAGELASTUPDATE)
            .having(DSL.count().gt(NachrichtenService.BAM_WERTE_JE_SPALTE))
            .limit(1)
            .fetchOne();
    // Ohne eine solche Nachricht prueft der Test nichts — dann ist er uebersprungen und nicht
    // gruen.
    assumeTrue(vielwertig != null, "Keine Nachricht mit mehr als drei Werten im Fenster");

    int werteInDerQuelle =
        glassfishDsl.fetchCount(
            MESSAGEBAM,
            MESSAGEBAM
                .MESSAGEID
                .eq(vielwertig.value1())
                .and(MESSAGEBAM.MESSAGEBAMTYPE.eq((short) 9006)));

    // Ein Fenster von genau einer Sekunde um diese Nachricht — damit sie auf der ersten Seite
    // steht.
    Antwort antwort =
        sitzung.hole(
            "/api/nachrichten?limit=200&zwischenschritte=true&von="
                + URLEncoder.encode(iso(vielwertig.value2()), StandardCharsets.UTF_8)
                + "&bis="
                + URLEncoder.encode(iso(vielwertig.value2()), StandardCharsets.UTF_8));

    List<String> ids = antwort.json("$.items[*].messageId");
    int index = ids.indexOf(vielwertig.value1());
    assertThat(index)
        .as("Die Nachricht muss im Fenster ihrer eigenen Sekunde liegen")
        .isNotNegative();

    assertThat(antwort.<List<String>>json("$.items[" + index + "].bamWerte[0].werte"))
        .hasSize(NachrichtenService.BAM_WERTE_JE_SPALTE);
    assertThat(antwort.<Integer>json("$.items[" + index + "].bamWerte[0].weitere"))
        .as("Die Kuerzung wird gezaehlt und nicht verschwiegen")
        .isPositive()
        .isLessThanOrEqualTo(werteInDerQuelle - NachrichtenService.BAM_WERTE_JE_SPALTE);
  }

  /**
   * {@code WOC} („Without Contract") hat Nachrichten, aber keine Zeile in {@code
   * MessageBAMMandant}. Keine leere Spalte, kein Platzhalter — eine Spalte ohne Inhalt behauptet,
   * es gaebe dort etwas zu sehen.
   */
  @Test
  @DisplayName("Ein Mandant ohne BAM-Konfiguration bekommt keine Spalte")
  void mandant_ohne_bam_konfiguration_bekommt_keine_spalte() throws Exception {
    String nutzerOhneBam = PRAEFIX + "liste-woc";
    legeNutzerAn(nutzerOhneBam, PASSWORT, Rolle.MANDANT, "WOC");
    Sitzung woc = anmelden(nutzerOhneBam, PASSWORT);

    Antwort antwort =
        woc.hole(
            "/api/nachrichten?limit=5&zwischenschritte=true&von="
                + URLEncoder.encode(
                    iso(LocalDateTime.parse("2025-12-01T00:00:00")), StandardCharsets.UTF_8)
                + "&bis="
                + URLEncoder.encode(
                    iso(LocalDateTime.parse("2025-12-30T00:00:00")), StandardCharsets.UTF_8));

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<List<String>>json("$.items[*].messageId")).isNotEmpty();
    assertThat(antwort.<List<List<Object>>>json("$.items[*].bamWerte")).allMatch(List::isEmpty);
  }
}
