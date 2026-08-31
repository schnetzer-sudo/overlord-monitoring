package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Pflicht-Isolationstests des Prozess-Katalogs — <b>einer je Endpunkt</b> (Regel M4). Ohne sie
 * wird nicht gemergt.
 *
 * <p>Es sind <b>sechs</b> Endpunkte, und <b>vier</b> davon <b>schreiben</b>. Das macht den Nachweis
 * hier schwerwiegender als bei einer Leseflaeche: Eine Luecke faende nicht nur fremde Daten, sie
 * ueberschriebe sie. Der sechste ist die Vorschlagsuebernahme (E22–E24, 26.08.2026); seine
 * Gegenprobe steht auf der <b>Wirkung</b> und nicht auf der Eingabe, weil er keine Kennung
 * entgegennimmt.
 *
 * <p><b>Zwei Rollengrenzen statt einer.</b> {@code /api/katalog/**} verlangt zusaetzlich die Rolle
 * {@code ADMIN}. Die Nutzer hier sind deshalb Administratoren, und weil ein Administrator fuer
 * <b>alle</b> Mandanten berechtigt ist, waehlt er seinen aktiven Mandanten ausdruecklich — genau
 * der Fall, in dem eine Verwechslung am teuersten waere.
 *
 * <p><b>Aufgeraeumt wird ueber {@code geaendert_von}.</b> Jede Zeile, die dieser Test schreibt,
 * traegt einen Benutzernamen mit dem Testpraefix; nichts anderes wird angefasst. Eine von Hand
 * kuratierte Zeile ueberlebt den Lauf.
 *
 * <p><b>Und kein Testfall setzt eine Zahl aus dem Pflegestand voraus</b> <i>(seit 31.08.2026, Regel
 * T2)</i>. Zwei Faelle taten das bis dahin und waren seit der Kuratierung vom 27.08.2026 rot; sie
 * legen sich jetzt an, was sie brauchen. Was dabei verloren ging und was nicht, steht in {@code
 * docs/testfestigkeit.md} §1 — hier steht nur, was gilt: <b>Eine Erwartung dieses Tests nennt
 * entweder eine Zahl, die er selbst gebaut hat, oder gar keine.</b>
 *
 * <h2>Ein dritter Mandant, und er ist der Grund fuer diese Runde</h2>
 *
 * <p>Anlegen kann sich ein Test seine Katalogzeile nur auf einem Prozess <b>ohne</b> Zeile (reines
 * {@code INSERT}, siehe {@link #prozessOhneKatalogzeile}). Solche Prozesse hatten am 31.08.2026 nur
 * noch zwei Mandanten: {@code SUTTONS} mit 17 und {@code WOC} mit 4 (offener Punkt T-3). <b>Der
 * Auftraggeber hat am 31.08.2026 entschieden, {@code SUTTONS} zu kuratieren und {@code WOC}
 * dauerhaft unkuratiert zu lassen</b> — die beiden Uebernahmetests laufen deshalb seit diesem Tag
 * gegen {@code WOC} und nicht mehr gegen {@code SUTTONS}. Die Begruendung steht in {@code
 * docs/prozess-katalog.md}, damit sie in einem halben Jahr nicht wegkuratiert wird.
 *
 * <p><b>Regel M4 bleibt erfuellt:</b> {@code VOTG} gegen {@code WOC} sind zwei verschiedene
 * Haeuser, genau wie {@code VOTG} gegen {@code SUTTONS}. Die uebrigen Testfaelle bleiben auf {@code
 * SUTTONS} — sie brauchen keinen freien Prozess, sondern nur einen fremden Bestand, und den hat
 * {@code SUTTONS} kuratiert wie unkuratiert.
 */
class ProzessKatalogIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "katalog-votg";
  private static final String NUTZER_B = PRAEFIX + "katalog-suttons";
  private static final String NUTZER_FREI = PRAEFIX + "katalog-woc";
  private static final String NUTZER_OHNE_ROLLE = PRAEFIX + "katalog-mandant";
  private static final String NUTZER_OHNE_WAHL = PRAEFIX + "katalog-ohnewahl";
  private static final String PASSWORT = "einLangesPasswort1";

  /**
   * <b>Der Mandant, auf dem sich dieser Test seine Katalogzeilen anlegt</b> — und er steht hier
   * ausdruecklich statt in {@code SicherheitsTestbasis}, weil er nur diese eine Eigenschaft
   * mitbringen muss: <i>mindestens einen Prozess ohne Katalogzeile</i>.
   *
   * <p>Am 31.08.2026 hatten das nur {@code SUTTONS} (17 frei) und {@code WOC} (4 frei). Der
   * Auftraggeber hat an diesem Tag entschieden, {@code SUTTONS} zu kuratieren und <b>{@code WOC}
   * dauerhaft unkuratiert zu lassen</b>; {@code WOC} ist damit die Reserve dieses Tests. Der Satz
   * dazu steht in {@code docs/prozess-katalog.md} — <b>ohne ihn kuratiert ihn jemand in einem
   * halben Jahr weg</b>, und dann ist der Test wieder rot (offener Punkt T-3).
   *
   * <p>Zwei Prozesse werden gebraucht, nicht einer: {@link
   * #uebernahme_ignoriert_untergeschobenen_mandanten} legt im schlechtesten Fall zwei Zeilen an.
   * Vier freie reichen dafuer mit Reserve.
   */
  private static final String MANDANT_FREI = "WOC";

  /** Eine ProcessID, die es garantiert nicht gibt. */
  private static final String ERFUNDENER_PROZESS = "00000_GIBTESNICHT_XX_YY";

  /** Eine ProjectID, die es garantiert nicht gibt. */
  private static final String ERFUNDENES_PROJEKT = "000_GibtEsNicht";

  /**
   * Der Zeitstempel der von Hand angelegten Zeilen. <b>Fest und nicht {@code now()}</b> — Regel Z1
   * gilt fuer den Anwendungscode, und ein fester Wert macht sichtbar, dass der Endpunkt ihn
   * ueberschreibt.
   */
  private static final LocalDateTime ANGELEGT_AM = LocalDateTime.parse("2026-01-01T00:00:00");

  private Sitzung aufVotg;
  private Sitzung aufSuttons;

  /** Die Sitzung auf {@link #MANDANT_FREI} — nur die beiden Uebernahmetests brauchen sie. */
  private Sitzung aufWoc;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_A)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_FREI)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.ADMIN, MANDANT_A);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.ADMIN, MANDANT_B);
    legeNutzerAn(NUTZER_FREI, PASSWORT, Rolle.ADMIN, MANDANT_FREI);
    aufVotg = angemeldetAuf(NUTZER_A, MANDANT_A);
    aufSuttons = angemeldetAuf(NUTZER_B, MANDANT_B);
    aufWoc = angemeldetAuf(NUTZER_FREI, MANDANT_FREI);
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

  private static String uebernahme(String modus) {
    return """
        {"modus":%s}"""
        .formatted(alsJson(modus));
  }

  /**
   * Ein Prozess des Mandanten, der <b>noch keine Katalogzeile traegt</b>.
   *
   * <p><b>Diese Pruefung ist nicht Vorsicht, sondern eine Lehre.</b> Der erste Entwurf legte die
   * Testzeile per Upsert auf den <i>ersten</i> Prozess des Mandanten. Traegt der bereits eine
   * kuratierte Zeile, ueberschreibt der Upsert deren {@code geaendert_von} mit dem Testpraefix —
   * und die Aufraeumregel aus §7 loescht danach eine <b>fremde</b> Zeile. Genau das ist am
   * 26.08.2026 einmal passiert und hat eine Katalogzeile von {@code VOTG} gekostet.
   *
   * <p>Angelegt wird deshalb ausschliesslich auf Prozessen ohne Zeile, und {@link #legeVorschlagAn}
   * ist ein reines {@code INSERT}: Eine Kollision schlaegt laut fehl, statt still zu
   * ueberschreiben.
   */
  private String prozessOhneKatalogzeile(Sitzung sitzung) throws IOException, InterruptedException {
    List<String> alle = sitzung.hole("/api/katalog/prozesse").json("$[*].processId");
    assertThat(alle).as("Ohne Prozesse bewiese der Test nur, dass leer leer ist").isNotEmpty();
    Set<String> vorhanden =
        Set.copyOf(
            monitorDsl
                .select(PROCESS_CATALOG.PROCESS_ID)
                .from(PROCESS_CATALOG)
                .where(PROCESS_CATALOG.PROCESS_ID.in(alle))
                .fetch(satz -> satz.value1()));
    return alle.stream()
        .filter(kennung -> !vorhanden.contains(kennung))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "Jeder Prozess dieses Mandanten traegt bereits eine Katalogzeile. Dieser Test"
                        + " legt keine fremde Zeile um — er braucht eine freie."));
  }

  /**
   * Legt von Hand eine offene Zeile mit einem Partnervorschlag an — <b>ohne jede Regel</b> und
   * <b>ohne Upsert</b>.
   *
   * <p>Sie traegt das Testpraefix in {@code geaendert_von} und faellt damit unter die Aufraeumregel
   * aus §7. Der Endpunkt ueberschreibt die Spalte spaeter mit dem Namen des angemeldeten
   * Testnutzers — der ebenfalls das Praefix traegt, sonst hinterliesse der erste Lauf gepflegte
   * Zeilen auf der geteilten Testkopie.
   */
  private void legeVorschlagAn(String processId) {
    monitorDsl
        .insertInto(PROCESS_CATALOG)
        .set(PROCESS_CATALOG.PROCESS_ID, processId)
        .set(PROCESS_CATALOG.PARTNER, "ERFUNDENERPARTNER")
        .set(PROCESS_CATALOG.RICHTUNG, Richtung.EINGEHEND.name())
        .set(PROCESS_CATALOG.PFLEGESTATUS, Pflegestatus.OFFEN.name())
        .set(PROCESS_CATALOG.VORSCHLAG_HERKUNFT, VorschlagHerkunft.REGEL_A.name())
        .set(PROCESS_CATALOG.GEAENDERT_AM, ANGELEGT_AM)
        .set(PROCESS_CATALOG.GEAENDERT_VON, PRAEFIX + "vorschlag")
        .execute();
  }

  /**
   * <b>Der Wachposten vor jedem {@code AUSFUEHREN}.</b>
   *
   * <p>Der Knopf uebernimmt <b>alle</b> uebernehmbaren Zeilen des aktiven Mandanten, nicht nur die
   * dieses Tests. Traegt der Bestand welche, kuratierte der Testlauf sie mit — und die
   * Aufraeumregel aus {@code @AfterEach} loeschte sie danach, weil der Endpunkt {@code
   * geaendert_von} auf den Testnutzer setzt. <b>Das ist genau der Vorfall vom 26.08.2026, nur ueber
   * einen anderen Weg</b> ({@code docs/mandantentrennung.md} §5).
   *
   * <p>Deshalb faehrt kein Testfall {@code AUSFUEHREN}, ohne vorher nachzusehen. Findet er eine
   * fremde uebernehmbare Zeile, <b>bricht er ab, statt sie anzufassen</b>.
   */
  private void nurEigeneDuerfenUebernommenWerden(Sitzung sitzung)
      throws IOException, InterruptedException {
    List<String> prozesse = sitzung.hole("/api/katalog/prozesse").json("$[*].processId");
    List<String> fremde =
        monitorDsl
            .select(PROCESS_CATALOG.PROCESS_ID)
            .from(PROCESS_CATALOG)
            .where(PROCESS_CATALOG.PROCESS_ID.in(prozesse))
            .and(PROCESS_CATALOG.PFLEGESTATUS.eq(Pflegestatus.OFFEN.name()))
            .and(
                PROCESS_CATALOG.VORSCHLAG_HERKUNFT.in(
                    VorschlagHerkunft.REGEL_A.name(), VorschlagHerkunft.REGEL_B.name()))
            .and(PROCESS_CATALOG.GEAENDERT_VON.notLike(PRAEFIX + "%"))
            .fetch(satz -> satz.value1());
    assertThat(fremde)
        .as(
            "AUSFUEHREN wuerde diese kuratierten Zeilen mit uebernehmen, und die Aufraeumung"
                + " loeschte sie danach. Der Test bricht lieber ab.")
        .isEmpty();
  }

  private static int betroffen(Sitzung sitzung) throws IOException, InterruptedException {
    Antwort vorschau =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"));
    assertThat(vorschau.status()).isEqualTo(200);
    return vorschau.json("$.betroffen");
  }

  private static String pflegestatusVon(Sitzung sitzung, String processId)
      throws IOException, InterruptedException {
    List<?> treffer =
        sitzung
            .hole("/api/katalog/prozesse")
            .json("$[?(@.processId=='" + processId + "')].pflegestatus");
    assertThat(treffer).as("Prozess %s nicht in der Pflegeliste", processId).hasSize(1);
    return String.valueOf(treffer.getFirst());
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

  // ─── POST /api/katalog/vorschlaege-uebernehmen (E22 bis E24) ─────────────────

  /**
   * <b>Der sechste Endpunkt, und seine Gegenprobe steht auf der Ausgabe</b> (Regel M4).
   *
   * <p>Er nimmt keine Kennung entgegen — es gibt also keine Eingabe, ueber die sich Existenz
   * erfragen liesse. Die Gegenprobe verschiebt sich damit auf die <b>Wirkung</b>, so wie schon bei
   * {@code ProzesseIsolationDbIT} und beim Heuristik-Lauf.
   *
   * <h2>Der Nachweis steht auf einer Zeile, die dieser Test selbst angelegt hat</h2>
   *
   * <p><b>Bis zum 31.08.2026 verlangte er stattdessen, dass Mandant A von sich aus eine
   * uebernehmbare Zeile mitbringt</b> — {@code assertThat(votgVorher).isPositive()}. Seit der
   * Kuratierung von 505 Katalogzeilen am 27.08.2026 bringt kein Mandant mehr eine mit, und der Test
   * war rot. <b>Das war der Mangel, nicht die rote Farbe:</b> Seine Aussage hing an einer Zahl aus
   * dem Pflegestand einer geteilten Testkopie und haette beim naechsten Pflegevorgang erneut
   * gewechselt. Die Einzelheiten stehen in {@code docs/testfestigkeit.md} §1.
   *
   * <p>Gebaut ist er deshalb <b>umgekehrt</b>: Die eine uebernehmbare Zeile gehoert {@link
   * #MANDANT_FREI} und ist von diesem Test angelegt. <b>Die Zaehne stecken auf der Leseseite</b>,
   * und das ist kein Rueckzug, sondern die schaerfere Stelle:
   *
   * <ol>
   *   <li><b>Die Vorschau von {@code VOTG} sieht die neue Zeile von {@code WOC} nicht.</b>
   *       Verglichen wird die Zahl vor und nach dem Anlegen — sie muss <b>gleich</b> bleiben. Fiele
   *       der Mandantenfilter aus {@code findeUebernehmbareVorschlaege}, stuende hier eine mehr.
   *       Das ist die Isolationsaussage, und sie kostet keinen einzigen Schreibzugriff.
   *   <li><b>{@code AUSFUEHREN} als {@code WOC} nimmt genau diese Zeile.</b> Das gibt Schritt 1
   *       seine Zaehne: Die Zeile <i>war</i> uebernehmbar — sie ist in der Vorschau von {@code
   *       VOTG} also nicht deshalb ausgeblieben, weil es ohnehin nichts zu sehen gab.
   *   <li><b>Und {@code VOTG} hat sich dabei in keiner Spalte veraendert.</b> Verglichen wird der
   *       <b>ganze Antwortrumpf</b> der Pflegeliste vor und nach dem Lauf, nicht nur eine Zahl.
   * </ol>
   *
   * <h2>Warum der fremde Lauf nicht {@code AUSFUEHREN} fahren darf</h2>
   *
   * <p><b>Ein erster Entwurf dieser Runde hat genau das getan</b> — {@code AUSFUEHREN} als {@code
   * VOTG}, um zu zeigen, dass die fremde Zeile liegen bleibt. Das ist auf einer <b>geteilten</b>
   * Testkopie unzulaessig: Der Knopf uebernimmt <b>alle</b> uebernehmbaren Zeilen des aktiven
   * Mandanten. Haette {@code VOTG} welche, kuratierte der Testlauf sie mit, setzte dabei {@code
   * geaendert_von} auf den Testnutzer — und die Aufraeumung aus §7 loeschte sie danach. <b>Das ist
   * der Vorfall vom 26.08.2026, nur ueber einen anderen Weg.</b>
   *
   * <p>Dass es heute nicht knallt, liegt allein daran, dass {@code VOTG} zurzeit keine
   * uebernehmbare Zeile hat — also an genau dem Pflegestand, von dem dieser Test unabhaengig sein
   * soll. {@link #nurEigeneDuerfenUebernommenWerden} steht deshalb vor jedem {@code AUSFUEHREN} und
   * bricht ab, statt eine fremde Zeile anzufassen.
   */
  @Test
  @DisplayName("Vorschlagsuebernahme: sie erfasst nur die Zeilen des aktiven Mandanten")
  void uebernahme_erfasst_nur_den_aktiven_mandanten() throws Exception {
    int wocVorher = betroffen(aufWoc);
    int votgVorher = betroffen(aufVotg);
    String votgRumpfVorher = aufVotg.hole("/api/katalog/prozesse").rumpf();

    // Die eine Zeile, um die es geht. Sie ist von diesem Test angelegt und traegt das Praefix;
    // was der Bestand von sich aus hergibt, spielt fuer die Aussage keine Rolle mehr.
    String vonWoc = prozessOhneKatalogzeile(aufWoc);
    legeVorschlagAn(vonWoc);

    assertThat(betroffen(aufWoc))
        .as("Die eine angelegte Zeile ist bei ihrem Mandanten angekommen")
        .isEqualTo(wocVorher + 1);
    assertThat(pflegestatusVon(aufWoc, vonWoc))
        .as("Ausgangszustand: die Zeile ist offen und damit uebernehmbar")
        .isEqualTo(Pflegestatus.OFFEN.name());

    // 1. Die Isolationsaussage, und sie kostet keinen Schreibzugriff: Beim anderen Mandanten ist
    //    die neue Zeile nicht angekommen.
    assertThat(betroffen(aufVotg))
        .as(
            "Die Vorschau von %s zaehlt eine Zeile von %s mit — der Mandantenfilter der"
                + " uebernehmbaren Vorschlaege traegt nicht",
            MANDANT_A, MANDANT_FREI)
        .isEqualTo(votgVorher);

    // 2. Die Gegenprobe, die Schritt 1 seine Zaehne gibt: Dieselbe Zeile, und jetzt greift der
    //    Knopf — sie war also sehr wohl uebernehmbar.
    nurEigeneDuerfenUebernommenWerden(aufWoc);
    Antwort eigenerLauf =
        aufWoc.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("AUSFUEHREN"));
    assertThat(eigenerLauf.status()).isEqualTo(200);
    assertThat(eigenerLauf.<Integer>json("$.betroffen")).isEqualTo(wocVorher + 1);
    assertThat(pflegestatusVon(aufWoc, vonWoc))
        .as(
            "Ohne diesen Schritt bewiese Schritt 1 nur, dass es bei %s ohnehin nichts zu sehen gab",
            MANDANT_A)
        .isEqualTo(Pflegestatus.GEPFLEGT.name());

    // 3. Und die Gegenrichtung: der Lauf von WOC hat bei VOTG keine einzige Spalte angefasst.
    assertThat(aufVotg.hole("/api/katalog/prozesse").rumpf())
        .as(
            "Beim anderen Mandanten darf sich keine einzige Zeile geaendert haben — sonst schriebe"
                + " der Knopf ueber Mandantengrenzen")
        .isEqualTo(votgRumpfVorher);
    assertThat(betroffen(aufVotg)).isEqualTo(votgVorher);
  }

  /**
   * <b>Regel M1 in beiden Formen</b> — als untergeschobenes Feld im Anfragekoerper und als
   * Abfrageparameter.
   *
   * <p>Damit die Gegenprobe Zaehne hat, muessen die beiden Mandanten <b>verschieden viele</b>
   * uebernehmbare Zeilen haben: Waeren beide Zahlen gleich, bewiese ein gleicher Rumpf nichts.
   *
   * <h2>Den Unterschied stellt der Test her, statt ihn vorzufinden</h2>
   *
   * <p><b>Bis zum 31.08.2026 hat er ihn im Katalog vorausgesetzt</b> und war seit der Kuratierung
   * vom 27.08.2026 rot: Seither haben <i>alle</i> Mandanten null uebernehmbare Zeilen, und null ist
   * gleich null. Auch hier war der Mangel nicht die rote Farbe, sondern dass die Aussage des Tests
   * am Pflegestand einer geteilten Testkopie hing ({@code docs/testfestigkeit.md} §1).
   *
   * <p>Der Test misst deshalb beide Ausgangszahlen und legt danach <b>so viele eigene Zeilen an,
   * dass sie sich mit Sicherheit unterscheiden</b> — eine genuegt, ausser die fremde Zahl liegt
   * genau um eins darueber; dann sind es zwei. <b>Mehr als zwei sind nie noetig</b>, unabhaengig
   * vom Pflegestand. Die Erwartung unten nennt damit keine Zahl aus dem Katalog, sondern eine, die
   * dieser Test selbst gebaut hat.
   *
   * <p><b>Angelegt wird auf {@link #MANDANT_FREI}</b>, aus demselben Grund wie bei {@link
   * #uebernahme_erfasst_nur_den_aktiven_mandanten}: Es ist der Mandant, der dauerhaft freie
   * Prozesse behaelt. Zwei davon kann dieser Fall im schlechtesten Fall verbrauchen.
   */
  @Test
  @DisplayName("Vorschlagsuebernahme: ein untergeschobener Mandant bleibt wirkungslos (M1)")
  void uebernahme_ignoriert_untergeschobenen_mandanten() throws Exception {
    int eigenVorher = betroffen(aufWoc);
    int fremd = betroffen(aufVotg);

    // Eine Zeile genuegt — ausser die fremde Zahl liegt genau eins ueber der eigenen; dann
    // fuehrte eine einzelne Zeile die beiden zusammen, statt sie zu trennen.
    int anzulegen = fremd == eigenVorher + 1 ? 2 : 1;
    for (int i = 0; i < anzulegen; i++) {
      legeVorschlagAn(prozessOhneKatalogzeile(aufWoc));
    }
    int eigen = eigenVorher + anzulegen;

    assertThat(betroffen(aufWoc))
        .as("Die selbst angelegten Zeilen, und keine des anderen Mandanten")
        .isEqualTo(eigen);
    assertThat(eigen)
        .as("Waeren beide Zahlen gleich, bewiese ein gleicher Antwortrumpf nichts")
        .isNotEqualTo(fremd);

    Antwort ohneFeld = aufWoc.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"));
    Antwort mitFeld =
        aufWoc.sende(
            "/api/katalog/vorschlaege-uebernehmen",
            """
            {"modus":"VORSCHAU","mandantId":"%s"}"""
                .formatted(MANDANT_A));
    Antwort mitParameter =
        aufWoc.sende(
            "/api/katalog/vorschlaege-uebernehmen?mandant=" + MANDANT_A, uebernahme("VORSCHAU"));

    assertThat(mitFeld.status()).isEqualTo(200);
    assertThat(mitFeld.rumpf())
        .as("Ein untergeschobenes Feld darf die Menge nicht veraendern")
        .isEqualTo(ohneFeld.rumpf());
    assertThat(mitParameter.rumpf())
        .as("Der Mandant kommt aus der Sitzung — ein unbekannter Parameter aendert nichts")
        .isEqualTo(ohneFeld.rumpf());
    assertThat(mitFeld.<Integer>json("$.betroffen"))
        .as("Und die Zahl ist die des aktiven Mandanten, nicht die des untergeschobenen")
        .isEqualTo(eigen);
  }

  // ─── Die Rollengrenze ────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ein MANDANT-Nutzer bekommt auf jedem der sechs Endpunkte 403")
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
    assertThat(
            ohneRolle
                .sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"))
                .status())
        .isEqualTo(403);

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
    assertThat(
            ohneWahl.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU")).status())
        .as("Ein Knopf, der „alle Mandanten\" meinte, gibt es nicht")
        .isEqualTo(403);
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
