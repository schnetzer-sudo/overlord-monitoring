package de.kraftwerkone.overlord.monitor.assistent;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.TASK_TYPE_MANDANT;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.assistent.modell.Modellanfrage;
import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht.Werkzeugergebnisse;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import de.kraftwerkone.overlord.monitor.security.Zugriffszaehler;
import de.kraftwerkone.overlord.monitor.task.SchreibkontextZaehlung;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jooq.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * <b>Der Pflicht-Isolationstest der fuenf neuen Endpunkte</b> (Regel M4, {@code docs/assistent.md}
 * §9.2): {@code GET /api/assistent}, {@code POST /api/assistent/gespraeche}, {@code POST
 * …/{id}/nachrichten}, {@code DELETE …/{id}} und {@code POST /api/tasks/pruefung}.
 *
 * <p><b>Statt des Anbieters spielt ein geskriptetes Modell</b> ({@link Skriptmodell} als
 * {@code @Primary}-Bean). Der Schalter ist an, der Schluessel ein Platzhalter, die Basis-URL ein
 * geschlossener Port auf dem eigenen Rechner: Es geht kein Aufruf hinaus, auch wenn die Verdrahtung
 * falsch waere.
 *
 * <p><b>Paarung {@code NEXANS} gegen {@code IBIS}</b>, beide fuer {@code WERK_ABLADESTELLE}
 * freigeschaltet; dazu legt der Test einen eigenen Typ an, der nur {@code IBIS} freigeschaltet ist
 * (Regel T2: reines {@code INSERT} auf eine freie Kennung, genau diese wird geloescht). {@code
 * SUTTONS} ist der Gegentest ohne Freischaltung.
 *
 * <p><b>Ununterscheidbar heisst dieselben Statements</b> — gezaehlt auf dem Schreib-Kontext ({@link
 * SchreibkontextZaehlung}). Derselbe Zaehler belegt, dass <b>kein Weg des Assistenten etwas
 * schreibt</b>: Jedes abgesetzte Statement ist ein {@code SELECT}.
 */
@TestPropertySource(
    properties = {
      "overlord.assistent.aktiv=true",
      "overlord.assistent.api-key=platzhalter-ohne-wert",
      "overlord.assistent.basis-url=http://127.0.0.1:9"
    })
@Import({AssistentIsolationDbIT.Ersatzmodell.class, SchreibkontextZaehlung.class})
class AssistentIsolationDbIT extends SicherheitsTestbasis {

  /** Das geskriptete Modell an Stelle des Adapters. */
  @TestConfiguration
  static class Ersatzmodell {

    @Bean
    @Primary
    Skriptmodell skriptmodell() {
      return new Skriptmodell();
    }
  }

  private static final String PASSWORT = "einLangesPasswort1";
  private static final String NEXANS = "NEXANS";
  private static final String IBIS = "IBIS";
  private static final String SUTTONS = "SUTTONS";
  private static final String WERK = Testdefinitionen.WERK;

  /** Ein Testtyp, nur fuer IBIS freigeschaltet; als Werkzeugname gueltig. */
  private static final String NUR_IBIS = PRAEFIX + "assistent-nur-ibis";

  private static final String NUR_IBIS_NAME = "Testtyp nur fuer IBIS";

  private static final String NUTZER_NEXANS = PRAEFIX + "assistent-nexans";
  private static final String NUTZER_NEXANS_2 = PRAEFIX + "assistent-nexans-2";
  private static final String NUTZER_IBIS = PRAEFIX + "assistent-ibis";
  private static final String NUTZER_SUTTONS = PRAEFIX + "assistent-suttons";
  private static final String NUTZER_ADMIN = PRAEFIX + "assistent-admin";

  private static final String ANPASSUNG =
      """
      {"aktion":"Anpassung","eintraege":[{"werk":"010","abladestelle":"18VZE","sndprn":"123456"}]}
      """;

  private static final String GATTUNG_FREIGEBEN =
      """
      {"aktion":"Freigeben","eintraege":[{"konto":"17"}]}
      """;

  @Autowired private Skriptmodell modell;
  @Autowired private Zugriffszaehler zugriffe;

  private Sitzung aufNexans;
  private Sitzung aufIbis;
  private Sitzung aufSuttons;

  @BeforeEach
  void vorbedingungenUndAnmeldung() throws Exception {
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(IBIS)).isTrue();
    assertThat(mandantRepository.existiert(SUTTONS)).isTrue();
    assertThat(freigeschaltet(NEXANS)).as("WERK_ABLADESTELLE fuer NEXANS").contains(WERK);
    assertThat(freigeschaltet(IBIS)).as("WERK_ABLADESTELLE fuer IBIS").contains(WERK);
    assertThat(freigeschaltet(SUTTONS)).as("kein aktiver Typ fuer SUTTONS").isEmpty();
    assertThat(monitorDsl.fetchCount(TASK_TYPE, TASK_TYPE.TASK_TYP.eq(NUR_IBIS)))
        .as("die Kennung des Testtyps ist frei")
        .isZero();

    monitorDsl
        .insertInto(TASK_TYPE)
        .set(TASK_TYPE.TASK_TYP, NUR_IBIS)
        .set(TASK_TYPE.NAME, NUR_IBIS_NAME)
        .set(TASK_TYPE.BESCHREIBUNG, "Ein Testtyp ohne fachliche Bedeutung.")
        .set(TASK_TYPE.STRUKTUR, JSON.valueOf(Testdefinitionen.GATTUNG_STRUKTUR))
        .set(TASK_TYPE.AKTIV, true)
        .execute();
    monitorDsl
        .insertInto(TASK_TYPE_MANDANT)
        .set(TASK_TYPE_MANDANT.TASK_TYP, NUR_IBIS)
        .set(TASK_TYPE_MANDANT.MANDANT_ID, IBIS)
        .execute();

    legeNutzerAn(NUTZER_NEXANS, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_NEXANS_2, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_IBIS, PASSWORT, Rolle.MANDANT, IBIS);
    legeNutzerAn(NUTZER_SUTTONS, PASSWORT, Rolle.MANDANT, SUTTONS);
    aufNexans = anmelden(NUTZER_NEXANS, PASSWORT);
    aufIbis = anmelden(NUTZER_IBIS, PASSWORT);
    aufSuttons = anmelden(NUTZER_SUTTONS, PASSWORT);
    modell.zuruecksetzen();
  }

  @AfterEach
  void raeumeTestTypWeg() {
    // ON DELETE CASCADE raeumt task_type_mandant mit. Genau die eigene Kennung, nichts sonst.
    monitorDsl.deleteFrom(TASK_TYPE).where(TASK_TYPE.TASK_TYP.eq(NUR_IBIS)).execute();
  }

  // ─── GET /api/assistent ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Stand: NEXANS ohne den IBIS-Typ, IBIS mit ihm, SUTTONS verfuegbar false")
  void stand() throws Exception {
    Antwort nexans = aufNexans.hole("/api/assistent");
    assertThat(nexans.status()).isEqualTo(200);
    assertThat(nexans.<Boolean>json("$.verfuegbar")).isTrue();
    assertThat(nexans.<List<String>>json("$.typen"))
        .contains(Testdefinitionen.WERK_NAME)
        .doesNotContain(NUR_IBIS_NAME);

    assertThat(aufIbis.hole("/api/assistent").<List<String>>json("$.typen"))
        .contains(Testdefinitionen.WERK_NAME, NUR_IBIS_NAME);

    Antwort suttons = aufSuttons.hole("/api/assistent");
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(suttons.rumpf()).isEqualTo("{\"verfuegbar\":false,\"typen\":[]}");
  }

  // ─── POST /api/assistent/gespraeche ────────────────────────────────────────────────────────

  @Test
  @DisplayName("Eroeffnen: SUTTONS bekommt 503, kein Aufruf des Modells")
  void suttons_nicht_verfuegbar() throws Exception {
    Antwort antwort = aufSuttons.sende("/api/assistent/gespraeche", "");

    assertThat(antwort.status()).isEqualTo(503);
    assertThat(antwort.<String>json("$.type")).endsWith("/assistent-nicht-verfuegbar");
    assertThat(modell.anfragen()).isEmpty();
  }

  @Test
  @DisplayName("Werkzeuge: NEXANS bekommt den IBIS-Typ nicht, IBIS bekommt ihn")
  void werkzeuge_je_mandant() throws Exception {
    modell.text("Guten Tag.").text("Guten Tag.");

    nachricht(aufNexans, eroeffne(aufNexans), "Hallo");
    List<String> werkzeugeNexans = werkzeugnamen(modell.letzteAnfrage());
    nachricht(aufIbis, eroeffne(aufIbis), "Hallo");
    List<String> werkzeugeIbis = werkzeugnamen(modell.letzteAnfrage());

    assertThat(werkzeugeNexans).contains(WERK).doesNotContain(NUR_IBIS);
    assertThat(werkzeugeIbis).contains(WERK, NUR_IBIS);
  }

  // ─── POST …/nachrichten ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Ein Werkzeugaufruf fuer den IBIS-Typ wird unter NEXANS abgewiesen, unter IBIS nicht")
  void fremder_typ_wird_abgewiesen() throws Exception {
    modell.werkzeug(NUR_IBIS, GATTUNG_FREIGEBEN).text("Das geht hier nicht.");
    Antwort nexans = nachricht(aufNexans, eroeffne(aufNexans), "Konto 17 freigeben");

    assertThat(nexans.status()).isEqualTo(200);
    assertThat(nexans.rumpf()).contains("\"vorschlag\":null");
    String ergebnisNexans = einzigesWerkzeugergebnis(modell.letzteAnfrage());
    assertThat(ergebnisNexans).contains(Gespraechsfuehrung.NICHT_ANGEBOTEN);

    // Gegenprobe: Derselbe Aufruf unter IBIS wird zur Karte.
    modell.werkzeug(NUR_IBIS, GATTUNG_FREIGEBEN).text("Bitte prüfen.");
    Antwort ibis = nachricht(aufIbis, eroeffne(aufIbis), "Konto 17 freigeben");
    assertThat(ibis.<String>json("$.vorschlag.typ")).isEqualTo(NUR_IBIS);
  }

  @Test
  @DisplayName("Karte ueber HTTP: alle Felder der Aktion, leere als null; nichts wird geschrieben")
  void karte_und_nichts_geschrieben() throws Exception {
    String gespraech = eroeffne(aufNexans);
    modell.werkzeug(WERK, ANPASSUNG).text("Bitte prüfen Sie die Karte.");

    zugriffe.zuruecksetzen();
    Antwort antwort = nachricht(aufNexans, gespraech, "SNDPRN von 010/18VZE auf 123456 ändern");
    List<String> statements = zugriffe.abgesetzt();

    assertThat(antwort.status()).as(antwort.rumpf()).isEqualTo(200);
    assertThat(antwort.<String>json("$.text")).isEqualTo("Bitte prüfen Sie die Karte.");
    assertThat(antwort.<String>json("$.vorschlag.typ")).isEqualTo(WERK);
    assertThat(antwort.<String>json("$.vorschlag.aktion")).isEqualTo("Anpassung");
    assertThat(antwort.<List<String>>json("$.vorschlag.felder[*].name"))
        .containsExactly("lieferantennummer", "werk", "abladestelle", "sndprn", "sndprn4908");
    assertThat(antwort.rumpf())
        .contains(
            "\"sndprn4908\":null", "\"lieferantennummer\":null", "\"schluessel\":\"010/18VZE\"");

    assertThat(statements).isNotEmpty();
    assertThat(statements)
        .as("der Assistent liest nur")
        .allMatch(sql -> sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("select"));
    assertThat(monitorDsl.fetchCount(TASK, TASK.ANGELEGT_VON.like(PRAEFIX + "assistent%")))
        .isZero();
  }

  @Test
  @DisplayName("Was das Modell bekommt, nennt weder Benutzername noch Mandant (E‑1035)")
  void datensparsamkeit() throws Exception {
    modell.werkzeug(WERK, ANPASSUNG).text("Bitte prüfen.");

    nachricht(aufNexans, eroeffne(aufNexans), "SNDPRN ändern");

    for (Modellanfrage anfrage : modell.anfragen()) {
      StringBuilder alles = new StringBuilder(anfrage.anweisung());
      for (Werkzeug werkzeug : anfrage.werkzeuge()) {
        alles.append(werkzeug.beschreibung()).append(werkzeug.eingabeschema());
      }
      alles.append(anfrage.verlauf());
      assertThat(alles.toString()).doesNotContainIgnoringCase(NUTZER_NEXANS).doesNotContain(NEXANS);
    }
  }

  @Test
  @DisplayName("Fremdes Gespraech (anderer Nutzer, anderer Mandant): 404 wie ein erfundenes")
  void fremdes_gespraech() throws Exception {
    String gespraech = eroeffne(aufNexans);
    Sitzung andererNutzer = anmelden(NUTZER_NEXANS_2, PASSWORT);
    String erfunden = UUID.randomUUID().toString();

    Antwort fremd = nachricht(andererNutzer, gespraech, "Hallo");
    Antwort ausgedacht = nachricht(andererNutzer, erfunden, "Hallo");
    Antwort ausIbis = nachricht(aufIbis, gespraech, "Hallo");

    assertThat(fremd.status()).isEqualTo(404);
    assertThat(ausgedacht.status()).isEqualTo(404);
    assertThat(ausIbis.status()).isEqualTo(404);
    assertThat(fremd.rumpfOhneTraceId().replace(gespraech, "<id>"))
        .isEqualTo(ausgedacht.rumpfOhneTraceId().replace(erfunden, "<id>"));
    assertThat(andererNutzer.loesche("/api/assistent/gespraeche/" + gespraech).status())
        .isEqualTo(404);
    assertThat(modell.anfragen()).isEmpty();

    // Gegenprobe: Fuer den Eigentuemer besteht es weiter.
    modell.text("Gern.");
    assertThat(nachricht(aufNexans, gespraech, "Hallo").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("Ein Gespraech derselben Person aus einer anderen Sitzung ist 404")
  void andere_sitzung_desselben_nutzers() throws Exception {
    String gespraech = eroeffne(aufNexans);
    Sitzung zweiteSitzung = anmelden(NUTZER_NEXANS, PASSWORT);

    assertThat(nachricht(zweiteSitzung, gespraech, "Hallo").status()).isEqualTo(404);
    assertThat(zweiteSitzung.loesche("/api/assistent/gespraeche/" + gespraech).status())
        .isEqualTo(404);

    modell.text("Gern.");
    assertThat(nachricht(aufNexans, gespraech, "Hallo").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("ADMIN: ohne Mandant 403; ein Mandantenwechsel beendet die Gespraeche der Sitzung")
  void admin_mandantenwechsel() throws Exception {
    legeNutzerAn(NUTZER_ADMIN, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(NUTZER_ADMIN, PASSWORT);

    assertThat(alsAdmin.hole("/api/assistent").status()).isEqualTo(403);
    assertThat(alsAdmin.sende("/api/assistent/gespraeche", "").status()).isEqualTo(403);
    assertThat(alsAdmin.sende("/api/tasks/pruefung", pruefung(WERK, ANPASSUNG)).status())
        .isEqualTo(403);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}");
    String gespraech = eroeffne(alsAdmin);
    modell.text("Gern.");
    assertThat(nachricht(alsAdmin, gespraech, "Hallo").status()).isEqualTo(200);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + IBIS + "\"}");
    assertThat(nachricht(alsAdmin, gespraech, "Hallo").status()).isEqualTo(404);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}");
    assertThat(nachricht(alsAdmin, gespraech, "Hallo").status())
        .as("beendet, nicht nur verdeckt")
        .isEqualTo(404);
    assertThat(modell.anfragen()).hasSize(1);
  }

  // ─── DELETE …/{id} ─────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Beenden: 204, danach 404 fuer Nachricht und erneutes Beenden")
  void beenden() throws Exception {
    String gespraech = eroeffne(aufNexans);

    assertThat(aufNexans.loesche("/api/assistent/gespraeche/" + gespraech).status()).isEqualTo(204);
    assertThat(aufNexans.loesche("/api/assistent/gespraeche/" + gespraech).status()).isEqualTo(404);
    assertThat(nachricht(aufNexans, gespraech, "Hallo").status()).isEqualTo(404);
  }

  // ─── POST /api/tasks/pruefung ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
      "Pruefung: gueltig leer, ungueltig mit Fehlern, untergeschobenes mandantId ignoriert")
  void pruefung() throws Exception {
    Antwort gueltig = aufNexans.sende("/api/tasks/pruefung", pruefung(WERK, ANPASSUNG));
    assertThat(gueltig.status()).as(gueltig.rumpf()).isEqualTo(200);
    assertThat(gueltig.rumpf()).isEqualTo("{\"errors\":[]}");

    Antwort ungueltig =
        aufNexans.sende(
            "/api/tasks/pruefung", pruefung(WERK, ANPASSUNG.replace("123456", "12345")));
    assertThat(ungueltig.status()).isEqualTo(200);
    assertThat(ungueltig.<List<String>>json("$.errors[*].code")).containsExactly("FORMAT");
    assertThat(ungueltig.<List<String>>json("$.errors[*].feld")).containsExactly("sndprn");

    zugriffe.zuruecksetzen();
    Antwort untergeschoben =
        aufNexans.sende(
            "/api/tasks/pruefung",
            pruefung(WERK, ANPASSUNG)
                .replaceFirst(
                    "\\{",
                    "{\"mandantId\":\""
                        + SUTTONS
                        + "\",\"anfrageId\":\""
                        + UUID.randomUUID()
                        + "\","));
    assertThat(untergeschoben.status()).as(untergeschoben.rumpf()).isEqualTo(200);
    assertThat(untergeschoben.rumpf()).isEqualTo("{\"errors\":[]}");
    assertThat(zugriffe.abgesetzt())
        .allMatch(sql -> sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("select"));
    assertThat(monitorDsl.fetchCount(TASK, TASK.ANGELEGT_VON.like(PRAEFIX + "assistent%")))
        .isZero();
  }

  @Test
  @DisplayName("Pruefung mit nicht angebotenem Typ: 404, ununterscheidbar vom erfundenen")
  void pruefung_nicht_angeboten() throws Exception {
    zugriffe.zuruecksetzen();
    Antwort suttons = aufSuttons.sende("/api/tasks/pruefung", pruefung(WERK, ANPASSUNG));
    List<String> statementsSuttons = zugriffe.abgesetzt();
    zugriffe.zuruecksetzen();
    Antwort erfunden =
        aufSuttons.sende("/api/tasks/pruefung", pruefung("WERK_ABLADESTELLX", ANPASSUNG));
    List<String> statementsErfunden = zugriffe.abgesetzt();

    assertThat(suttons.status()).isEqualTo(404);
    assertThat(erfunden.status()).isEqualTo(404);
    assertThat(suttons.rumpfOhneTraceId()).isEqualTo(erfunden.rumpfOhneTraceId());
    assertThat(statementsSuttons).isNotEmpty().isEqualTo(statementsErfunden);

    Antwort fremderTyp =
        aufNexans.sende("/api/tasks/pruefung", pruefung(NUR_IBIS, GATTUNG_FREIGEBEN));
    assertThat(fremderTyp.status()).isEqualTo(404);
    // Gegenprobe: Fuer IBIS ist derselbe Rumpf gueltig.
    assertThat(aufIbis.sende("/api/tasks/pruefung", pruefung(NUR_IBIS, GATTUNG_FREIGEBEN)).rumpf())
        .isEqualTo("{\"errors\":[]}");
  }

  // ─── Aenderungszwang ───────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Der Aenderungszwang beim Passwort sperrt alle fuenf Endpunkte")
  void aenderungszwang_sperrt() throws Exception {
    String nutzer = PRAEFIX + "assistent-zwang";
    legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, true, NEXANS);
    Sitzung sitzung = anmelden(nutzer, PASSWORT);
    String irgendein = UUID.randomUUID().toString();

    assertThat(sitzung.hole("/api/assistent").status()).isEqualTo(403);
    assertThat(sitzung.sende("/api/assistent/gespraeche", "").status()).isEqualTo(403);
    assertThat(nachricht(sitzung, irgendein, "Hallo").status()).isEqualTo(403);
    assertThat(sitzung.loesche("/api/assistent/gespraeche/" + irgendein).status()).isEqualTo(403);
    assertThat(sitzung.sende("/api/tasks/pruefung", pruefung(WERK, ANPASSUNG)).status())
        .isEqualTo(403);
  }

  // ─── Hilfen ────────────────────────────────────────────────────────────────────────────────

  private String eroeffne(Sitzung sitzung) throws Exception {
    Antwort antwort = sitzung.sende("/api/assistent/gespraeche", "");
    assertThat(antwort.status()).as("Eroeffnen: %s", antwort.rumpf()).isEqualTo(201);
    return antwort.json("$.gespraechId");
  }

  private static Antwort nachricht(Sitzung sitzung, String gespraech, String text)
      throws Exception {
    return sitzung.sende(
        "/api/assistent/gespraeche/" + gespraech + "/nachrichten",
        Skriptmodell.JSON.writeValueAsString(Map.of("text", text)));
  }

  /** Der Rumpf von {@code POST /api/tasks/pruefung}: Typ plus Aktion und Eintraege. */
  private static String pruefung(String typ, String eingabe) {
    return eingabe.strip().replaceFirst("\\{", "{\"typ\":\"" + typ + "\",");
  }

  private static List<String> werkzeugnamen(Modellanfrage anfrage) {
    return anfrage.werkzeuge().stream().map(Werkzeug::name).toList();
  }

  private static String einzigesWerkzeugergebnis(Modellanfrage anfrage) {
    Werkzeugergebnisse ergebnisse = (Werkzeugergebnisse) anfrage.verlauf().getLast();
    assertThat(ergebnisse.ergebnisse()).hasSize(1);
    return ergebnisse.ergebnisse().getFirst().inhalt();
  }

  /** Die aktiven, fuer den Mandanten freigeschalteten Typen ohne die eigenen Testtypen. */
  private List<String> freigeschaltet(String mandant) {
    return monitorDsl
        .select(TASK_TYPE.TASK_TYP)
        .from(TASK_TYPE_MANDANT)
        .join(TASK_TYPE)
        .on(TASK_TYPE.TASK_TYP.eq(TASK_TYPE_MANDANT.TASK_TYP))
        .where(TASK_TYPE_MANDANT.MANDANT_ID.eq(mandant))
        .and(TASK_TYPE.AKTIV.isTrue())
        .and(TASK_TYPE.TASK_TYP.notLike(PRAEFIX + "%"))
        .fetch(TASK_TYPE.TASK_TYP);
  }
}
