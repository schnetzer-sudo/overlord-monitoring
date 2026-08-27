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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Der Parameter {@code ueberfaellig} gegen die Testkopie (E-j, Schritt 10b-1).
 *
 * <p><b>Das Fenster ist absolut und umfasst den ganzen Dezember 2025</b>, aus demselben Grund wie
 * in {@code NachrichtenlisteDbIT}: Ein relatives Fenster haengt an der Dev-Uhr und damit am
 * Datenstand. Im Dezember traegt {@code NEXANS} alle {@value #UEBERFAELLIG_IM_DEZEMBER}
 * ueberfaelligen Zeilen des Gesamtbestands (M97) — genug fuer zwei volle Seiten, worauf der
 * Blaettertest ruht.
 *
 * <p><b>Der Stichtag ist die Anwendungsuhr</b>, im Profil {@code dev} also der Anker {@code
 * 2025-12-30 04:09:47}. Mit der Systemuhr waere hier jede offene Zeile ueberfaellig und der
 * Parameter ohne Aussage; der erste Test haelt das fest, damit ein Uhrenwechsel nicht
 * stillschweigend die Grundlage aller uebrigen entzieht.
 *
 * <p>Die Mandantentrennung prueft {@code NachrichtenIsolationDbIT}, den Plan {@code
 * NachrichtenPlanDbIT} — hier geht es um Zaehlung, Sortierung und das Blaettern ueber zwei Seiten.
 */
class NachrichtenUeberfaelligDbIT extends SicherheitsTestbasis {

  private static final String NUTZER = PRAEFIX + "ueberfaellig-nexans";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Der einzige Mandant mit ueberfaelligen Zeilen (M97, Vorprobe). */
  private static final String MANDANT = "NEXANS";

  /**
   * Alle ueberfaelligen Zeilen des Gesamtbestands liegen im Dezember 2025 und gehoeren {@code
   * NEXANS} — 538, alle {@code SUSPENDED}, alle mit einer Frist von 1.800 Sekunden (M97).
   *
   * <p><b>Weicht die Zahl ab, ist die Testkopie neu befuellt und nicht der Code kaputt</b> —
   * dieselbe Bauform wie der Bestandsanfang in {@code RollupDbIT}.
   */
  static final int UEBERFAELLIG_IM_DEZEMBER = 538;

  private static final LocalDateTime FENSTER_VON = LocalDateTime.parse("2025-12-01T00:00:00");
  private static final LocalDateTime FENSTER_BIS = LocalDateTime.parse("2025-12-31T00:00:00");

  @Autowired private Clock anwendungsuhr;

  private Sitzung sitzung;

  @BeforeEach
  void anmelden() throws IOException, InterruptedException {
    legeNutzerAn(NUTZER, PASSWORT, Rolle.MANDANT, MANDANT);
    sitzung = anmelden(NUTZER, PASSWORT);
  }

  private String iso(LocalDateTime wanduhrzeit) {
    return DateTimeFormatter.ISO_INSTANT.format(
        wanduhrzeit.atZone(anwendungsuhr.getZone()).toInstant());
  }

  private String abfrage(String zusatz) {
    return "/api/nachrichten?von="
        + URLEncoder.encode(iso(FENSTER_VON), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(FENSTER_BIS), StandardCharsets.UTF_8)
        + zusatz;
  }

  /** Der 29.12.2025 — ein Tag im dichten Bestand mit genau einer ueberfaelligen Zeile (M97). */
  private String tagesAbfrage(String zusatz) {
    return "/api/nachrichten?von="
        + URLEncoder.encode(iso(LocalDateTime.parse("2025-12-29T00:00:00")), StandardCharsets.UTF_8)
        + "&bis="
        + URLEncoder.encode(iso(LocalDateTime.parse("2025-12-30T00:00:00")), StandardCharsets.UTF_8)
        + zusatz;
  }

  /**
   * Die Voraussetzung aller uebrigen Tests: Die Anwendungsuhr steht am Anker der Testkopie und
   * nicht auf der Systemzeit. Stuende sie auf heute, waere jede offene Zeile ueberfaellig.
   */
  @Test
  @DisplayName("Die Anwendungsuhr steht am Anker der Testkopie")
  void anwendungsuhr_steht_am_anker() {
    assertThat(LocalDateTime.now(anwendungsuhr))
        .as("Ohne den zurueckversetzten Anker misst dieser Test etwas anderes")
        .isBetween(FENSTER_BIS.minusDays(1), FENSTER_BIS.plusDays(2));
  }

  /**
   * Ein Tagesfenster statt des Dezembers, und der Grund gehoert dazu: {@link
   * NachrichtenFilter#LIMIT_MAXIMUM} deckelt eine Seite bei 200. Ueber den Dezember lieferten
   * <b>beide</b> Abfragen 200 Zeilen, und der Test vergliche zwei Deckel statt zweier Mengen. Am
   * 29.12.2025 traegt {@code NEXANS} genau <b>eine</b> ueberfaellige Zeile (M97, Vorprobe) bei
   * mehreren tausend insgesamt — der Unterschied ist dort nicht zu uebersehen.
   */
  @Test
  @DisplayName("Der Parameter grenzt die Liste ein und laesst sie nicht unveraendert")
  void parameter_grenzt_ein() throws Exception {
    List<String> ohne = sitzung.hole(tagesAbfrage("&limit=200")).json("$.items[*].messageId");
    List<String> mit =
        sitzung.hole(tagesAbfrage("&limit=200&ueberfaellig=true")).json("$.items[*].messageId");

    assertThat(ohne).isNotEmpty();
    assertThat(mit)
        .as("Am 29.12.2025 traegt NEXANS genau eine ueberfaellige Zeile (M97)")
        .hasSize(1);
    // Kein containsAll: Die ungefilterte Seite ist bei 200 gedeckelt und traegt die 200
    // juengsten Zeilen des Tages; die eine ueberfaellige liegt aelter und faellt heraus.
    // Genau das ist der Punkt des Parameters — sie waere ohne ihn nicht zu finden.
  }

  /**
   * Jede gelieferte Zeile traegt einen offenen Status. Das ist die fachliche Zusage des Parameters
   * und zugleich die Gegenprobe zur SQL-Uebersetzung: Waere die Statusmenge im Statement eine
   * andere als die, die {@code istEndstatus} offen laesst, stuende hier eine abgeschlossene Zeile.
   */
  @Test
  @DisplayName("Jede ueberfaellige Zeile steht in einem offenen Status")
  void jede_zeile_ist_offen() throws Exception {
    Antwort antwort = sitzung.hole(abfrage("&limit=200&ueberfaellig=true"));

    assertThat(antwort.<List<String>>json("$.items[*].statusKind"))
        .isNotEmpty()
        .allSatisfy(statusKind -> assertThat(statusKind).isIn("WARTEND", "LAEUFT"));
    assertThat(antwort.<List<String>>json("$.items[*].status"))
        .allSatisfy(status -> assertThat(status).isIn("SUSPENDED", "RUNNING"));
  }

  /**
   * <b>Der Nachweis, den der Auftrag verlangt: Der Cursor blaettert ueber zwei Seiten korrekt</b> —
   * obwohl der Plan ihn in dieser Abfrageform <b>nicht</b> mehr als Indexbereich nutzt ({@code
   * key_len} bleibt 123 statt auf 151 zu steigen, M97). Er wirkt dann als nachgelagerte Bedingung,
   * und genau deshalb ist dieser Test noetig: Was der Optimierer anders macht, darf am Ergebnis
   * nichts aendern.
   *
   * <p>Geprueft wird dreierlei: Die zweite Seite ist voll, sie ueberschneidet sich mit der ersten
   * <b>in keiner einzigen Kennung</b>, und sie schliesst in der Sortierfolge lueckenlos an.
   */
  @Test
  @DisplayName("Der Cursor blaettert ueber zwei Seiten, ohne zu ueberlappen oder zu ueberspringen")
  void cursor_blaettert_ueber_zwei_seiten() throws Exception {
    Antwort seite1 = sitzung.hole(abfrage("&limit=50&ueberfaellig=true"));
    List<String> ids1 = seite1.json("$.items[*].messageId");
    List<String> zeiten1 = seite1.json("$.items[*].zeitpunkt");
    String cursor = seite1.json("$.nextCursor");

    assertThat(ids1).hasSize(50);
    assertThat(seite1.<Boolean>json("$.hasMore")).isTrue();
    assertThat(cursor).isNotNull();

    Antwort seite2 =
        sitzung.hole(
            abfrage(
                "&limit=50&ueberfaellig=true&cursor="
                    + URLEncoder.encode(cursor, StandardCharsets.UTF_8)));
    List<String> ids2 = seite2.json("$.items[*].messageId");
    List<String> zeiten2 = seite2.json("$.items[*].zeitpunkt");

    assertThat(ids2).hasSize(50).doesNotContainAnyElementsOf(ids1);
    assertThat(zeiten1).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(zeiten2).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(zeiten2.getFirst())
        .as("Die zweite Seite schliesst an die erste an, sie beginnt nicht davor")
        .isLessThanOrEqualTo(zeiten1.getLast());
  }

  /**
   * Und dasselbe bis zum Ende durchgeblaettert: Die Summe aller Seiten ist genau die Menge, die ein
   * einziger Aufruf mit grossem {@code limit} liefert — und sie ist {@value
   * #UEBERFAELLIG_IM_DEZEMBER}.
   *
   * <p><b>Das ist der eigentliche Beweis</b>, dass der entwertete Cursor nichts verliert und nichts
   * doppelt: Ein Blaettern, das eine Zeile ueberspringt, faellt bei zwei Seiten nicht auf, bei elf
   * schon.
   */
  @Test
  @DisplayName("Ueber alle Seiten geblaettert kommen genau die 538 Zeilen heraus, jede einmal")
  void alle_seiten_ergeben_die_ganze_menge() throws Exception {
    List<String> geblaettert = new ArrayList<>();
    List<String> zeitpunkte = new ArrayList<>();
    String cursor = null;
    int seiten = 0;
    do {
      Antwort seite =
          sitzung.hole(
              abfrage(
                  "&limit=50&ueberfaellig=true"
                      + (cursor == null
                          ? ""
                          : "&cursor=" + URLEncoder.encode(cursor, StandardCharsets.UTF_8))));
      geblaettert.addAll(seite.<List<String>>json("$.items[*].messageId"));
      zeitpunkte.addAll(seite.<List<String>>json("$.items[*].zeitpunkt"));
      cursor =
          Boolean.TRUE.equals(seite.<Boolean>json("$.hasMore")) ? seite.json("$.nextCursor") : null;
      seiten++;
      assertThat(seiten).as("Endlosschleife beim Blaettern").isLessThan(30);
    } while (cursor != null);

    assertThat(geblaettert)
        .as("Weicht die Zahl ab, ist die Testkopie neu befuellt — nicht der Code kaputt")
        .hasSize(UEBERFAELLIG_IM_DEZEMBER)
        .doesNotHaveDuplicates();
    assertThat(seiten).isEqualTo(11);
    assertThat(zeitpunkte)
        .as("Ueber die Seitengrenzen hinweg bleibt die Sortierfolge lueckenlos absteigend")
        .isSortedAccordingTo(java.util.Comparator.reverseOrder());
  }

  /**
   * Die unvereinbare Kombination am laufenden Endpunkt: {@code ueberfaellig=true} zusammen mit
   * einem Statusfilter, der keinen offenen Status enthaelt, ist {@code 400} — und keine leere
   * Liste. Eine leere Liste hiesse „in diesem Fenster gibt es nichts", und das waere eine falsche
   * Auskunft ueber den Bestand.
   */
  @Test
  @DisplayName("ueberfaellig mit einem Statusfilter ohne offenen Status ist 400")
  void unvereinbare_kombination_ist_400() throws Exception {
    Antwort antwort = sitzung.hole(abfrage("&ueberfaellig=true&status=ABGESCHLOSSEN"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.rumpf()).contains("ueberfaellig-und-status-unvereinbar");

    // Und die Gegenprobe: mit einem offenen Status geht dieselbe Anfrage durch.
    assertThat(sitzung.hole(abfrage("&ueberfaellig=true&status=WARTEND")).status()).isEqualTo(200);
  }
}
