package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Pflicht-Isolationstests des Prozess-Katalogs — <b>einer je Endpunkt</b> (Regel M4). Ohne sie
 * wird nicht gemergt.
 *
 * <p>Es sind fuenf Endpunkte, und drei davon <b>schreiben</b>. Das macht den Nachweis hier
 * schwerwiegender als bei einer Leseflaeche: Eine Luecke faende nicht nur fremde Daten, sie
 * ueberschriebe sie.
 *
 * <p><b>Zwei Rollengrenzen statt einer.</b> {@code /api/katalog/**} verlangt zusaetzlich die Rolle
 * {@code ADMIN}. Die Nutzer hier sind deshalb Administratoren, und weil ein Administrator fuer
 * <b>alle</b> Mandanten berechtigt ist, waehlt er seinen aktiven Mandanten ausdruecklich — genau
 * der Fall, in dem eine Verwechslung am teuersten waere.
 *
 * <p><b>Aufgeraeumt wird ueber {@code geaendert_von}.</b> Jede Zeile, die dieser Test schreibt,
 * traegt einen Benutzernamen mit dem Testpraefix; nichts anderes wird angefasst. Eine von Hand
 * kuratierte Zeile ueberlebt den Lauf.
 */
class ProzessKatalogIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "katalog-votg";
  private static final String NUTZER_B = PRAEFIX + "katalog-suttons";
  private static final String NUTZER_OHNE_ROLLE = PRAEFIX + "katalog-mandant";
  private static final String NUTZER_OHNE_WAHL = PRAEFIX + "katalog-ohnewahl";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Eine ProcessID, die es garantiert nicht gibt. */
  private static final String ERFUNDENER_PROZESS = "00000_GIBTESNICHT_XX_YY";

  /** Eine ProjectID, die es garantiert nicht gibt. */
  private static final String ERFUNDENES_PROJEKT = "000_GibtEsNicht";

  private Sitzung aufVotg;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_A)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.ADMIN, MANDANT_A);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.ADMIN, MANDANT_B);
    aufVotg = angemeldetAuf(NUTZER_A, MANDANT_A);
    aufSuttons = angemeldetAuf(NUTZER_B, MANDANT_B);
  }

  @AfterEach
  void raeumeKatalogzeilenWeg() {
    monitorDsl
        .deleteFrom(PROCESS_CATALOG)
        .where(PROCESS_CATALOG.GEAENDERT_VON.like(PRAEFIX + "%"))
        .execute();
  }

  private Sitzung angemeldetAuf(String nutzer, String mandantId)
      throws IOException, InterruptedException {
    Sitzung sitzung = anmelden(nutzer, PASSWORT);
    Antwort gewechselt =
        sitzung.sende(
            "/api/auth/mandant",
            """
            {"mandantId":"%s"}"""
                .formatted(mandantId));
    assertThat(gewechselt.status())
        .as("Ein Administrator ist fuer alle Mandanten berechtigt und waehlt deshalb ausdruecklich")
        .isEqualTo(200);
    return sitzung;
  }

  /** Der Rumpf ohne die beiden Angaben, die je Anfrage verschieden sind und sein muessen. */
  private static String vergleichbar(Antwort antwort) {
    return antwort
        .rumpfOhneTraceId()
        .replaceAll("\"instance\"\\s*:\\s*\"[^\"]*\"", "\"instance\":\"-\"");
  }

  private static String einProzessVon(Sitzung sitzung) throws IOException, InterruptedException {
    Antwort liste = sitzung.hole("/api/katalog/prozesse");
    assertThat(liste.status()).isEqualTo(200);
    List<String> kennungen = liste.json("$[*].processId");
    assertThat(kennungen).as("Ohne Prozesse bewiese der Test nur, dass leer leer ist").isNotEmpty();
    return kennungen.getFirst();
  }

  private static String einProjektVon(Sitzung sitzung) throws IOException, InterruptedException {
    Antwort liste = sitzung.hole("/api/katalog/prozesse");
    List<String> projekte = liste.json("$[*].projectId");
    assertThat(projekte).isNotEmpty();
    return projekte.getFirst();
  }

  private static String zuordnung(String partner, String richtung) {
    return """
        {"partner":%s,"richtung":%s}"""
        .formatted(alsJson(partner), alsJson(richtung));
  }

  private static String alsJson(String wert) {
    return wert == null ? "null" : "\"" + wert + "\"";
  }

  private static String massenzuordnung(String projectId, String feld, String wert, String modus) {
    return """
        {"projectId":%s,"feld":%s,"wert":%s,"modus":%s}"""
        .formatted(alsJson(projectId), alsJson(feld), alsJson(wert), alsJson(modus));
  }

  // ─── Voraussetzung ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("Beide Mandanten haben ueberhaupt Prozesse im Katalog")
  void beide_mandanten_haben_prozesse() throws Exception {
    assertThat(aufVotg.hole("/api/katalog/prozesse").<List<String>>json("$[*].processId"))
        .isNotEmpty();
    assertThat(aufSuttons.hole("/api/katalog/prozesse").<List<String>>json("$[*].processId"))
        .isNotEmpty();
  }

  // ─── GET /api/katalog/prozesse ───────────────────────────────────────────────

  @Test
  @DisplayName("Pflegeliste: kein einziger fremder Prozess, weder als Kennung noch als Name")
  void pflegeliste_zeigt_nichts_fremdes() throws Exception {
    Antwort vonSuttons = aufSuttons.hole("/api/katalog/prozesse");
    List<String> fremdeKennungen = vonSuttons.json("$[*].processId");
    List<String> fremdeProjekte = vonSuttons.json("$[*].projectId");

    Antwort vonVotg = aufVotg.hole("/api/katalog/prozesse");

    assertThat(vonVotg.<List<String>>json("$[*].processId"))
        .isNotEmpty()
        .doesNotContainAnyElementsOf(fremdeKennungen);
    // Die Gegenprobe ueber den ganzen Rumpf: Auch ein Projektname darf nicht durchsickern — ueber
    // ihn liesse sich die Prozesslandschaft eines fremden Mandanten ebenso ablesen.
    String rumpf = vonVotg.rumpf();
    for (String fremd : fremdeKennungen) {
      assertThat(rumpf).doesNotContain(fremd);
    }
    for (String fremd : fremdeProjekte) {
      assertThat(rumpf).doesNotContain(fremd);
    }
  }

  @Test
  @DisplayName("Pflegeliste: der Filter nurOffene aendert nichts an der Trennung")
  void pflegeliste_mit_filter_zeigt_nichts_fremdes() throws Exception {
    List<String> fremde =
        aufSuttons.hole("/api/katalog/prozesse?nurOffene=true").json("$[*].processId");
    Antwort eigene = aufVotg.hole("/api/katalog/prozesse?nurOffene=true");

    assertThat(eigene.status()).isEqualTo(200);
    assertThat(eigene.<List<String>>json("$[*].processId")).doesNotContainAnyElementsOf(fremde);
  }

  // ─── GET /api/katalog/partner ────────────────────────────────────────────────

  @Test
  @DisplayName("Partnerliste: ein bei SUTTONS gepflegter Partner erscheint bei VOTG nicht")
  void partnerliste_ist_mandantengebunden() throws Exception {
    String fremderProzess = einProzessVon(aufSuttons);
    String partner = "ERFUNDENERPARTNER";

    Antwort gesetzt =
        aufSuttons.aendere(
            "/api/katalog/prozesse/" + fremderProzess, zuordnung(partner, "EINGEHEND"));
    assertThat(gesetzt.status()).isEqualTo(200);

    assertThat(aufSuttons.hole("/api/katalog/partner").<List<String>>json("$"))
        .as("Sonst bewiese der Test nur, dass beide Listen leer sind")
        .contains(partner);
    assertThat(aufVotg.hole("/api/katalog/partner").<List<String>>json("$"))
        .as("BAYER bei VOTG und BAYER bei SUTTONS sind zwei Werte (E3)")
        .doesNotContain(partner);
  }

  // ─── PUT /api/katalog/prozesse/{processId} ───────────────────────────────────

  @Test
  @DisplayName(
      "Zuordnung: ein fremder Prozess ist 404 und von einem erfundenen nicht zu " + "unterscheiden")
  void zuordnung_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    String fremd = einProzessVon(aufSuttons);

    Antwort fremdAberEcht =
        aufVotg.aendere("/api/katalog/prozesse/" + fremd, zuordnung("ERFUNDEN", null));
    Antwort erfunden =
        aufVotg.aendere("/api/katalog/prozesse/" + ERFUNDENER_PROZESS, zuordnung("ERFUNDEN", null));

    assertThat(fremdAberEcht.status())
        .as("Niemals 403 — das verriete, dass es den Prozess gibt")
        .isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht)).isEqualTo(vergleichbar(erfunden));
  }

  @Test
  @DisplayName("Zuordnung: die Trennung gilt in beide Richtungen — und schreibt nichts")
  void zuordnung_trennung_gilt_in_beide_richtungen() throws Exception {
    String vonVotg = einProzessVon(aufVotg);

    Antwort abgewiesen =
        aufSuttons.aendere("/api/katalog/prozesse/" + vonVotg, zuordnung("ERFUNDEN", null));

    assertThat(abgewiesen.status())
        .as("sonst bewiese der Test nur, dass VOTG nichts sieht")
        .isEqualTo(404);
    // Und der abgewiesene Aufruf darf die Zeile auch nicht angelegt haben.
    Antwort eigene = aufVotg.hole("/api/katalog/prozesse");
    assertThat(eigene.rumpf())
        .as("Ein abgewiesener Schreibversuch hinterlaesst keine Zeile")
        .doesNotContain("ERFUNDEN");
  }

  // ─── POST /api/katalog/massenzuordnung ───────────────────────────────────────

  @Test
  @DisplayName(
      "Massenzuordnung: ein fremdes Projekt ist 404 und von einem erfundenen nicht zu "
          + "unterscheiden")
  void massenzuordnung_fremd_und_erfunden_sind_ununterscheidbar() throws Exception {
    String fremdesProjekt = einProjektVon(aufSuttons);

    Antwort fremdAberEcht =
        aufVotg.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(fremdesProjekt, "PARTNER", "ERFUNDEN", "VORSCHAU"));
    Antwort erfunden =
        aufVotg.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(ERFUNDENES_PROJEKT, "PARTNER", "ERFUNDEN", "VORSCHAU"));

    assertThat(fremdAberEcht.status()).isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(vergleichbar(fremdAberEcht))
        .as("Eine Vorschau mit null Zeilen waere eine andere Auskunft als „gibt es nicht\"")
        .isEqualTo(vergleichbar(erfunden));
  }

  @Test
  @DisplayName("Massenzuordnung: auch im Modus AUSFUEHREN ist ein fremdes Projekt 404")
  void massenzuordnung_ausfuehren_auf_fremdem_projekt() throws Exception {
    String fremdesProjekt = einProjektVon(aufSuttons);

    Antwort abgewiesen =
        aufVotg.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(fremdesProjekt, "PARTNER", "ERFUNDEN", "AUSFUEHREN"));

    assertThat(abgewiesen.status()).isEqualTo(404);
    assertThat(aufSuttons.hole("/api/katalog/prozesse").rumpf())
        .as("Der abgewiesene Aufruf darf beim Eigentuemer nichts veraendert haben")
        .doesNotContain("ERFUNDEN");
  }

  // ─── POST /api/katalog/vorschlagen ───────────────────────────────────────────

  @Test
  @DisplayName("Heuristik-Lauf: er laeuft fuer den aktiven Mandanten und nur fuer ihn")
  void heuristiklauf_laeuft_nur_fuer_den_aktiven_mandanten() throws Exception {
    int eigeneProzesse =
        aufSuttons.hole("/api/katalog/prozesse").<List<String>>json("$[*].processId").size();
    String votgVorher = aufVotg.hole("/api/katalog/prozesse").rumpf();

    Antwort lauf = aufSuttons.sende("/api/katalog/vorschlagen", "{}");

    assertThat(lauf.status()).isEqualTo(200);
    int angelegt = lauf.json("$.angelegt");
    int aufgefrischt = lauf.json("$.aufgefrischt");
    int unberuehrt = lauf.json("$.unberuehrt");
    assertThat(angelegt + aufgefrischt + unberuehrt)
        .as("Der Lauf sieht genau die Prozesse des aktiven Mandanten — nicht alle 1.503")
        .isEqualTo(eigeneProzesse);

    assertThat(aufVotg.hole("/api/katalog/prozesse").rumpf())
        .as("Beim anderen Mandanten darf sich keine einzige Zeile geaendert haben")
        .isEqualTo(votgVorher);
  }

  // ─── Die Rollengrenze ────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ein MANDANT-Nutzer bekommt auf jedem der fuenf Endpunkte 403")
  void mandantennutzer_kommt_nicht_an_den_katalog() throws Exception {
    legeNutzerAn(NUTZER_OHNE_ROLLE, PASSWORT, Rolle.MANDANT, MANDANT_A);
    Sitzung ohneRolle = anmelden(NUTZER_OHNE_ROLLE, PASSWORT);

    assertThat(ohneRolle.hole("/api/katalog/prozesse").status()).isEqualTo(403);
    assertThat(ohneRolle.hole("/api/katalog/partner").status()).isEqualTo(403);
    assertThat(
            ohneRolle
                .aendere("/api/katalog/prozesse/" + ERFUNDENER_PROZESS, zuordnung("X", null))
                .status())
        .isEqualTo(403);
    assertThat(
            ohneRolle
                .sende(
                    "/api/katalog/massenzuordnung",
                    massenzuordnung(ERFUNDENES_PROJEKT, "PARTNER", "X", "VORSCHAU"))
                .status())
        .isEqualTo(403);
    assertThat(ohneRolle.sende("/api/katalog/vorschlagen", "{}").status()).isEqualTo(403);

    // Und die Gegenprobe: Derselbe Nutzer erreicht seine eigene Nachrichtenflaeche sehr wohl.
    assertThat(ohneRolle.hole("/api/prozesse").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("Ein Administrator ohne gewaehlten Mandanten bekommt 403 statt aller Daten")
  void admin_ohne_mandantenwahl() throws Exception {
    legeNutzerAn(NUTZER_OHNE_WAHL, PASSWORT, Rolle.ADMIN, MANDANT_A);
    Sitzung ohneWahl = anmelden(NUTZER_OHNE_WAHL, PASSWORT);

    assertThat(ohneWahl.hole("/api/katalog/prozesse").status())
        .as("Ein Zugriff „einfach ohne Filter\" existiert nicht")
        .isEqualTo(403);
    assertThat(ohneWahl.sende("/api/katalog/vorschlagen", "{}").status()).isEqualTo(403);
  }

  // ─── Regel M1 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Kein Endpunkt nimmt eine Mandanten-ID entgegen (Regel M1)")
  void keine_mandanten_id() throws Exception {
    Antwort mitParameter = aufVotg.hole("/api/katalog/prozesse?mandant=" + MANDANT_B);
    Antwort ohneParameter = aufVotg.hole("/api/katalog/prozesse");

    assertThat(mitParameter.status()).isEqualTo(200);
    assertThat(mitParameter.rumpf())
        .as("Ein unbekannter Parameter aendert nichts — der Mandant kommt aus der Sitzung")
        .isEqualTo(ohneParameter.rumpf());

    Antwort partnerMitParameter = aufVotg.hole("/api/katalog/partner?mandant=" + MANDANT_B);
    assertThat(partnerMitParameter.rumpf()).isEqualTo(aufVotg.hole("/api/katalog/partner").rumpf());
  }

  @Test
  @DisplayName("Auch ein mandantId-Feld im Anfragekoerper bleibt wirkungslos")
  void mandanten_id_im_koerper_wird_ignoriert() throws Exception {
    String fremdesProjekt = einProjektVon(aufSuttons);

    Antwort mitFeld =
        aufVotg.sende(
            "/api/katalog/massenzuordnung",
            """
            {"projectId":"%s","feld":"PARTNER","wert":"ERFUNDEN","modus":"VORSCHAU",\
            "mandantId":"%s"}"""
                .formatted(fremdesProjekt, MANDANT_B));

    assertThat(mitFeld.status())
        .as("Ein untergeschobener Mandant darf den Zugriff nicht oeffnen")
        .isEqualTo(404);
  }
}
