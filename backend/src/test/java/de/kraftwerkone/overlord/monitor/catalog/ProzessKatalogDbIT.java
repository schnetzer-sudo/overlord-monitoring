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
 * Der Prozess-Katalog gegen die Testkopie — die Zusicherungen, die nur mit echten Daten haltbar
 * sind.
 *
 * <p><b>Gefahren wird auf {@code SUTTONS}</b>: ein Projekt, 17 Prozesse. Das ist bewusst der
 * kleinste Bestand und nicht der groesste — jeder Schreibvorgang hier ist ein Commit auf einer
 * gemeinsam genutzten Datenbank, und die Fragen dieses Tests haengen an der Logik und nicht an der
 * Menge. Die Mengenfrage beantwortet die Messung, nicht dieser Test.
 *
 * <p>{@code SUTTONS} hat noch eine Eigenschaft, die hier passt: Die Heuristik liefert dort
 * <b>nichts</b> — der Mandant ist von Regel A namentlich ausgenommen (M76). Damit ist jeder
 * Partner, der in diesen Tests auftaucht, von Hand gesetzt und keiner aus einer Regel.
 *
 * <p><b>Aufgeraeumt wird ueber {@code geaendert_von}</b> mit dem Testpraefix. Eine von Hand
 * kuratierte Zeile ueberlebt den Lauf.
 */
class ProzessKatalogDbIT extends SicherheitsTestbasis {

  private static final String NUTZER = PRAEFIX + "katalog-pflege";
  private static final String PASSWORT = "einLangesPasswort1";
  private static final String PARTNER = "ERFUNDENERPARTNER";
  private static final String ZWEITER_PARTNER = "ANDERERPARTNER";

  /**
   * Wer die Zeilen der Uebernahmetests von Hand anlegt. <b>Mit dem Testpraefix</b>, sonst greift
   * die Aufraeumregel aus §7 nicht.
   */
  private static final String ANLEGER = PRAEFIX + "vorschlag";

  /**
   * Der Zeitstempel der von Hand angelegten Zeilen — <b>fest und nicht {@code now()}</b>. Regel Z1
   * gilt fuer den Anwendungscode; ein fester Wert macht hier sichtbar, dass der Endpunkt ihn
   * ueberschreibt.
   */
  private static final LocalDateTime ANGELEGT_AM = LocalDateTime.parse("2026-01-01T00:00:00");

  private Sitzung sitzung;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();
    legeNutzerAn(NUTZER, PASSWORT, Rolle.ADMIN, MANDANT_B);
    sitzung = anmelden(NUTZER, PASSWORT);
    Antwort gewechselt =
        sitzung.sende(
            "/api/auth/mandant",
            """
            {"mandantId":"%s"}"""
                .formatted(MANDANT_B));
    assertThat(gewechselt.status()).isEqualTo(200);
  }

  @AfterEach
  void raeumeKatalogzeilenWeg() {
    monitorDsl
        .deleteFrom(PROCESS_CATALOG)
        .where(PROCESS_CATALOG.GEAENDERT_VON.like(PRAEFIX + "%"))
        .execute();
  }

  private List<String> prozesse() throws IOException, InterruptedException {
    Antwort liste = sitzung.hole("/api/katalog/prozesse");
    assertThat(liste.status()).isEqualTo(200);
    List<String> kennungen = liste.json("$[*].processId");
    assertThat(kennungen).as("Ohne Prozesse prueft dieser Test nichts").isNotEmpty();
    return kennungen;
  }

  private String einProjekt() throws IOException, InterruptedException {
    List<String> projekte = sitzung.hole("/api/katalog/prozesse").json("$[*].projectId");
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
        {"projectId":"%s","feld":"%s","wert":%s,"modus":"%s"}"""
        .formatted(projectId, feld, alsJson(wert), modus);
  }

  private static String uebernahme(String modus) {
    return """
        {"modus":"%s"}"""
        .formatted(modus);
  }

  /**
   * Prozesse des Mandanten, die <b>noch keine Katalogzeile tragen</b> — nur auf ihnen legt dieser
   * Test etwas an.
   *
   * <p><b>Das ist nicht Vorsicht, sondern eine Lehre.</b> Der erste Entwurf schrieb die Testzeile
   * per Upsert auf den <i>ersten</i> Prozess des Mandanten. Traegt der bereits eine kuratierte
   * Zeile, ueberschreibt der Upsert deren {@code geaendert_von} mit dem Testpraefix — und die
   * Aufraeumregel loescht danach eine <b>fremde</b> Zeile. Genau das ist am 26.08.2026 einmal
   * passiert und hat eine Katalogzeile von {@code VOTG} gekostet.
   */
  private List<String> freieProzesse(int wieviele) throws IOException, InterruptedException {
    List<String> alle = prozesse();
    Set<String> vorhanden =
        Set.copyOf(
            monitorDsl
                .select(PROCESS_CATALOG.PROCESS_ID)
                .from(PROCESS_CATALOG)
                .where(PROCESS_CATALOG.PROCESS_ID.in(alle))
                .fetch(satz -> satz.value1()));
    List<String> frei =
        alle.stream().filter(kennung -> !vorhanden.contains(kennung)).limit(wieviele).toList();
    assertThat(frei)
        .as(
            "Dieser Test legt keine fremde Zeile um — er braucht %d Prozesse ohne Katalogzeile."
                + " Stehen weniger zur Verfuegung, ist der Katalog von %s kuratiert und das ist"
                + " der Befund",
            wieviele, MANDANT_B)
        .hasSize(wieviele);
    return frei;
  }

  /**
   * Legt von Hand eine Katalogzeile an — <b>ohne jede Regel</b> und <b>ohne Upsert</b>.
   *
   * <p>Das ist der Grund, warum diese Tests auf {@code SUTTONS} laufen: Die Heuristik liefert dort
   * <b>nichts</b>, es entsteht also keine Zeile mit {@code REGEL_A}/{@code REGEL_B} von selbst.
   * Jede Vorschlagszeile hier ist gesetzt und keine abgeleitete — sonst pruefte der Test die
   * Heuristik statt der Uebernahme.
   *
   * <p><b>Ein reines {@code INSERT}:</b> Eine Kollision schlaegt laut fehl, statt still eine fremde
   * Zeile zu ueberschreiben. Die Kennungen kommen aus {@link #freieProzesse}.
   *
   * <p><b>{@code geaendert_von} traegt das Testpraefix.</b> Die Aufraeumregel aus §7 haengt daran;
   * ohne sie hinterliesse der erste Lauf gepflegte Zeilen auf der geteilten Testkopie, und die
   * ueberleben jeden weiteren Lauf.
   */
  private void legeZeileAn(
      String processId, Pflegestatus status, VorschlagHerkunft herkunft, String richtung) {
    monitorDsl
        .insertInto(PROCESS_CATALOG)
        .set(PROCESS_CATALOG.PROCESS_ID, processId)
        .set(PROCESS_CATALOG.PARTNER, herkunft == VorschlagHerkunft.KEINE ? null : PARTNER)
        .set(PROCESS_CATALOG.RICHTUNG, richtung)
        .set(PROCESS_CATALOG.PFLEGESTATUS, status.name())
        .set(PROCESS_CATALOG.VORSCHLAG_HERKUNFT, herkunft.name())
        .set(PROCESS_CATALOG.GEAENDERT_AM, ANGELEGT_AM)
        .set(PROCESS_CATALOG.GEAENDERT_VON, ANLEGER)
        .execute();
  }

  /** Der Aenderungsvermerk direkt aus der Tabelle — er steht in keiner Antwort. */
  private String[] vermerkVon(String processId) {
    var satz =
        monitorDsl
            .select(PROCESS_CATALOG.GEAENDERT_AM, PROCESS_CATALOG.GEAENDERT_VON)
            .from(PROCESS_CATALOG)
            .where(PROCESS_CATALOG.PROCESS_ID.eq(processId))
            .fetchOne();
    assertThat(satz).as("Zeile %s fehlt in process_catalog", processId).isNotNull();
    return new String[] {String.valueOf(satz.value1()), satz.value2()};
  }

  private String feldVon(String processId, String feld) throws IOException, InterruptedException {
    Object wert =
        sitzung
            .hole("/api/katalog/prozesse")
            .json("$[?(@.processId=='" + processId + "')]." + feld);
    List<?> treffer = (List<?>) wert;
    assertThat(treffer).as("Prozess %s nicht in der Pflegeliste", processId).hasSize(1);
    return treffer.getFirst() == null ? null : String.valueOf(treffer.getFirst());
  }

  // ─── Der leere Partner (E4) ──────────────────────────────────────────────────

  @Test
  @DisplayName("Ein leerer Partner ist speicherbar und setzt den Status auf GEPFLEGT")
  void leerer_partner_ist_speicherbar() throws Exception {
    String prozess = prozesse().getFirst();

    Antwort gesetzt = sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(null, null));

    assertThat(gesetzt.status()).isEqualTo(200);
    assertThat(gesetzt.<String>json("$.partner"))
        .as("„hingesehen, es gibt nichts\" ist ein gueltiger gepflegter Zustand (E4)")
        .isNull();
    assertThat(gesetzt.<String>json("$.pflegestatus")).isEqualTo("GEPFLEGT");
    assertThat(feldVon(prozess, "pflegestatus")).isEqualTo("GEPFLEGT");
    assertThat(feldVon(prozess, "partner")).isNull();
  }

  @Test
  @DisplayName("Ein Partner aus Leerzeichen ist kein Partner, sondern leer")
  void leerraum_ist_kein_partner() throws Exception {
    String prozess = prozesse().getFirst();

    Antwort gesetzt = sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung("   ", null));

    assertThat(gesetzt.status()).isEqualTo(200);
    assertThat(gesetzt.<String>json("$.partner"))
        .as("Ein gefuelltes Feld ohne Inhalt waere ein dritter Zustand durch die Hintertuer")
        .isNull();
  }

  @Test
  @DisplayName("Ein gepflegter Partner erscheint in der abgeleiteten Auswahlliste (E2)")
  void gepflegter_partner_erscheint_in_der_auswahlliste() throws Exception {
    String prozess = prozesse().getFirst();
    assertThat(sitzung.hole("/api/katalog/partner").<List<String>>json("$"))
        .doesNotContain(PARTNER);

    sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, "AUSGEHEND"));

    assertThat(sitzung.hole("/api/katalog/partner").<List<String>>json("$")).contains(PARTNER);
  }

  // ─── Die Massenzuordnung (E11, E12) ──────────────────────────────────────────

  @Test
  @DisplayName("Vorschau und Ausfuehrung liefern dieselbe Zahl")
  void vorschau_und_ausfuehrung_liefern_dieselbe_zahl() throws Exception {
    String projekt = einProjekt();

    Antwort vorschau =
        sitzung.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(projekt, "PARTNER", PARTNER, "VORSCHAU"));
    Antwort ausgefuehrt =
        sitzung.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(projekt, "PARTNER", PARTNER, "AUSFUEHREN"));

    assertThat(vorschau.status()).isEqualTo(200);
    assertThat(ausgefuehrt.status()).isEqualTo(200);
    assertThat(vorschau.<Integer>json("$.betroffen"))
        .as("Der Nutzer bestaetigt genau die Zahl, die dann passiert")
        .isEqualTo(ausgefuehrt.<Integer>json("$.betroffen"))
        .isPositive();
    assertThat(vorschau.<Integer>json("$.davonGepflegt"))
        .isEqualTo(ausgefuehrt.<Integer>json("$.davonGepflegt"));
    assertThat(vorschau.<String>json("$.modus")).isEqualTo("VORSCHAU");
    assertThat(ausgefuehrt.<String>json("$.modus")).isEqualTo("AUSFUEHREN");
  }

  @Test
  @DisplayName("Die Vorschau schreibt nichts")
  void vorschau_schreibt_nichts() throws Exception {
    String projekt = einProjekt();
    String vorher = sitzung.hole("/api/katalog/prozesse").rumpf();

    sitzung.sende(
        "/api/katalog/massenzuordnung", massenzuordnung(projekt, "PARTNER", PARTNER, "VORSCHAU"));

    assertThat(sitzung.hole("/api/katalog/prozesse").rumpf()).isEqualTo(vorher);
  }

  @Test
  @DisplayName("Die Massenzuordnung ueberschreibt gepflegte Zeilen und zaehlt sie vorher (E12)")
  void massenzuordnung_ueberschreibt_gepflegte_zeilen() throws Exception {
    String projekt = einProjekt();
    String prozess = prozesse().getFirst();
    sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, null));

    Antwort vorschau =
        sitzung.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(projekt, "PARTNER", ZWEITER_PARTNER, "VORSCHAU"));
    assertThat(vorschau.<Integer>json("$.davonGepflegt"))
        .as("Das ist die Zahl, die verloren geht — deshalb ist die Vorschau Pflicht")
        .isEqualTo(1);

    Antwort ausgefuehrt =
        sitzung.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(projekt, "PARTNER", ZWEITER_PARTNER, "AUSFUEHREN"));

    assertThat(ausgefuehrt.status()).isEqualTo(200);
    assertThat(feldVon(prozess, "partner"))
        .as("Der Schutzmodus „nur offene Zeilen\" machte genau diese Korrektur unmoeglich")
        .isEqualTo(ZWEITER_PARTNER);
  }

  @Test
  @DisplayName("Die Massenzuordnung setzt genau ein Feld und laesst das andere stehen (E11)")
  void massenzuordnung_setzt_genau_ein_feld() throws Exception {
    String projekt = einProjekt();
    String prozess = prozesse().getFirst();

    sitzung.sende(
        "/api/katalog/massenzuordnung", massenzuordnung(projekt, "PARTNER", PARTNER, "AUSFUEHREN"));
    assertThat(feldVon(prozess, "partner")).isEqualTo(PARTNER);
    assertThat(feldVon(prozess, "richtung")).isNull();

    sitzung.sende(
        "/api/katalog/massenzuordnung",
        massenzuordnung(projekt, "RICHTUNG", "EINGEHEND", "AUSFUEHREN"));

    assertThat(feldVon(prozess, "richtung")).isEqualTo("EINGEHEND");
    assertThat(feldVon(prozess, "partner"))
        .as("Feldweise heisst: das andere Feld bleibt unangetastet")
        .isEqualTo(PARTNER);
  }

  @Test
  @DisplayName("Eine unbekannte Richtung ist 400 und leert kein Projekt")
  void unbekannte_richtung_ist_ein_fehler() throws Exception {
    String projekt = einProjekt();

    Antwort abgewiesen =
        sitzung.sende(
            "/api/katalog/massenzuordnung",
            massenzuordnung(projekt, "RICHTUNG", "SEITWAERTS", "AUSFUEHREN"));

    assertThat(abgewiesen.status())
        .as("Ein Tippfehler darf nicht stillschweigend ein ganzes Projekt leeren")
        .isEqualTo(400);
    assertThat(abgewiesen.<String>json("$.type")).endsWith("richtung-unbekannt");
  }

  // ─── Der Heuristik-Lauf (E13) ────────────────────────────────────────────────

  @Test
  @DisplayName("Der Heuristik-Lauf fasst gepflegte Zeilen nicht an")
  void heuristiklauf_ruehrt_gepflegte_zeilen_nicht_an() throws Exception {
    String prozess = prozesse().getFirst();
    sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, "AUSGEHEND"));

    Antwort lauf = sitzung.sende("/api/katalog/vorschlagen", "{}");

    assertThat(lauf.status()).isEqualTo(200);
    assertThat(lauf.<Integer>json("$.unberuehrt")).isEqualTo(1);
    assertThat(feldVon(prozess, "partner")).isEqualTo(PARTNER);
    assertThat(feldVon(prozess, "richtung")).isEqualTo("AUSGEHEND");
    assertThat(feldVon(prozess, "pflegestatus")).isEqualTo("GEPFLEGT");
  }

  @Test
  @DisplayName("Ein zweiter Lauf legt nichts an und frischt die offenen Zeilen auf")
  void zweiter_lauf_legt_nichts_an() throws Exception {
    int anzahl = prozesse().size();

    Antwort erster = sitzung.sende("/api/katalog/vorschlagen", "{}");
    Antwort zweiter = sitzung.sende("/api/katalog/vorschlagen", "{}");

    assertThat(erster.<Integer>json("$.angelegt")).isEqualTo(anzahl);
    assertThat(erster.<Integer>json("$.aufgefrischt")).isZero();
    assertThat(zweiter.<Integer>json("$.angelegt"))
        .as("Beim zweiten Lauf gibt es nichts mehr anzulegen")
        .isZero();
    assertThat(zweiter.<Integer>json("$.aufgefrischt"))
        .as("Sonst friert der erste Lauf jeden spaeteren Regelfehler ein (E13)")
        .isEqualTo(anzahl);
    assertThat(zweiter.<Integer>json("$.unberuehrt")).isZero();
  }

  @Test
  @DisplayName("Bei SUTTONS liefert die Heuristik nichts — und sagt das mit KEINE")
  void heuristik_liefert_bei_suttons_nichts() throws Exception {
    int anzahl = prozesse().size();

    Antwort lauf = sitzung.sende("/api/katalog/vorschlagen", "{}");

    assertThat(lauf.<Integer>json("$.regelA"))
        .as("SUTTONS ist von Regel A namentlich ausgenommen (M76)")
        .isZero();
    assertThat(lauf.<Integer>json("$.regelB")).isZero();
    assertThat(lauf.<Integer>json("$.keine"))
        .as("„geprueft, nichts abgeleitet\" ist ein Ergebnis und kein fehlender Wert (Q4)")
        .isEqualTo(anzahl);

    // Und in der Liste steht danach ueberall OFFEN mit leeren Feldern.
    Antwort liste = sitzung.hole("/api/katalog/prozesse");
    assertThat(liste.<List<String>>json("$[*].pflegestatus")).containsOnly("OFFEN");
    assertThat(liste.<List<String>>json("$[*].vorschlagHerkunft")).containsOnly("KEINE");
  }

  @Test
  @DisplayName("Prozesse ohne Katalogzeile stehen als OFFEN mit leeren Feldern in der Liste (E5)")
  void prozesse_ohne_katalogzeile_sind_offen() throws Exception {
    Antwort liste = sitzung.hole("/api/katalog/prozesse");

    assertThat(liste.<List<String>>json("$[*].pflegestatus"))
        .as("Ein Prozess ohne Zeile ist offen und nicht zustandslos")
        .containsOnly("OFFEN");
    assertThat(liste.<List<String>>json("$[*].vorschlagHerkunft")).containsOnly("KEINE");
    assertThat(liste.<List<String>>json("$[*].processId")).isNotEmpty();
  }

  @Test
  @DisplayName("Der Filter nurOffene blendet gepflegte Zeilen aus")
  void filter_blendet_gepflegte_zeilen_aus() throws Exception {
    List<String> alle = prozesse();
    String prozess = alle.getFirst();
    sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, null));

    Antwort nurOffene = sitzung.hole("/api/katalog/prozesse?nurOffene=true");

    assertThat(nurOffene.<List<String>>json("$[*].processId"))
        .hasSize(alle.size() - 1)
        .doesNotContain(prozess);
  }

  // ─── Der Bestandslauf (E14, E15) ─────────────────────────────────────────────

  @Test
  @DisplayName("Vor dem ersten Lauf ist traegtNachrichten null — nicht false (E14)")
  void vor_dem_ersten_lauf_ist_der_bestand_unbekannt() throws Exception {
    String prozess = prozesse().getFirst();

    assertThat(feldVon(prozess, "traegtNachrichten"))
        .as(
            "null heisst „noch nie geprueft\" und ist etwas anderes als „geprueft und tot\"."
                + " Wuerde hier false stehen, waere der Unterschied nicht wiederherstellbar (E20)")
        .isNull();
    assertThat(feldVon(prozess, "bestandGeprueftAm")).isNull();

    // Auch eine kuratierte Zeile bleibt unbekannt: Zuordnen ist keine Erhebung.
    sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, null));
    assertThat(feldVon(prozess, "pflegestatus")).isEqualTo("GEPFLEGT");
    assertThat(feldVon(prozess, "traegtNachrichten")).isNull();
  }

  @Test
  @DisplayName("Der Bestandslauf setzt das Flag und meldet beide Zahlen (E14)")
  void bestandslauf_setzt_das_flag() throws Exception {
    int anzahl = prozesse().size();

    Antwort lauf = sitzung.sende("/api/katalog/vorschlagen", "{}");

    assertThat(lauf.status()).isEqualTo(200);
    assertThat(lauf.<Integer>json("$.bestandGeprueft"))
        .as("Ohne diese Zahl ist ein reihenweise wirkungsloser Lauf nicht erkennbar")
        .isEqualTo(anzahl);
    assertThat(lauf.<Integer>json("$.ohneNachrichten"))
        .as("Bei SUTTONS traegt jeder der 17 Prozesse Nachrichten (M83-5) — hier bleibt es leer")
        .isZero();

    Antwort liste = sitzung.hole("/api/katalog/prozesse");
    assertThat(liste.<List<Object>>json("$[*].traegtNachrichten"))
        .as("Nach dem Lauf ist keine Zeile mehr unbekannt")
        .hasSize(anzahl)
        .containsOnly(Boolean.TRUE);
    assertThat(liste.<List<Object>>json("$[*].bestandGeprueftAm")).hasSize(anzahl);
  }

  @Test
  @DisplayName(
      "Der Bestandslauf schreibt auch auf GEPFLEGT und laesst die Kuratierung stehen (E15)")
  void bestandslauf_schreibt_auch_auf_gepflegte_zeilen() throws Exception {
    String prozess = prozesse().getFirst();
    sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, "AUSGEHEND"));
    assertThat(feldVon(prozess, "traegtNachrichten"))
        .as("Eine Zuordnung erhebt den Bestand nicht — sie ist Kuratierung, keine Beobachtung")
        .isNull();

    Antwort lauf = sitzung.sende("/api/katalog/vorschlagen", "{}");

    assertThat(lauf.<Integer>json("$.unberuehrt"))
        .as("Die Heuristik laesst die gepflegte Zeile in Ruhe (E13)")
        .isEqualTo(1);
    assertThat(feldVon(prozess, "traegtNachrichten"))
        .as(
            "Der Bestandslauf schreibt trotzdem auf sie: E13 schuetzt Kuratierung, nicht"
                + " Beobachtung (E15)")
        .isEqualTo("true");
    assertThat(feldVon(prozess, "bestandGeprueftAm")).isNotNull();

    // Und die kuratierten Felder sind unberuehrt geblieben.
    assertThat(feldVon(prozess, "partner")).isEqualTo(PARTNER);
    assertThat(feldVon(prozess, "richtung")).isEqualTo("AUSGEHEND");
    assertThat(feldVon(prozess, "pflegestatus")).isEqualTo("GEPFLEGT");
  }

  @Test
  @DisplayName("Eine Zuordnung nach dem Lauf traegt den erhobenen Bestand weiter (E14)")
  void zuordnung_nach_dem_lauf_behaelt_den_bestand() throws Exception {
    sitzung.sende("/api/katalog/vorschlagen", "{}");
    String prozess = prozesse().getFirst();

    Antwort zugeordnet =
        sitzung.aendere("/api/katalog/prozesse/" + prozess, zuordnung(PARTNER, null));

    assertThat(zugeordnet.status()).isEqualTo(200);
    assertThat(zugeordnet.<Boolean>json("$.traegtNachrichten"))
        .as(
            "Die Antwort wird gebaut und nicht nachgelesen — sie muss den erhobenen Bestand"
                + " trotzdem mitfuehren, sonst sieht die Zeile nach dem Speichern ungeprueft aus")
        .isTrue();
    assertThat(zugeordnet.<Object>json("$.bestandGeprueftAm")).isNotNull();
  }

  // ─── Die Vorschlagsuebernahme (E22 bis E24) ──────────────────────────────────

  @Test
  @DisplayName("Die Vorschau schreibt nicht und nennt die Zahl (E24)")
  void uebernahme_vorschau_schreibt_nicht() throws Exception {
    List<String> alle = freieProzesse(2);
    legeZeileAn(alle.get(0), Pflegestatus.OFFEN, VorschlagHerkunft.REGEL_A, null);
    legeZeileAn(alle.get(1), Pflegestatus.OFFEN, VorschlagHerkunft.REGEL_A, null);

    Antwort vorschau =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"));

    assertThat(vorschau.status()).isEqualTo(200);
    assertThat(vorschau.<String>json("$.modus")).isEqualTo("VORSCHAU");
    assertThat(vorschau.<Integer>json("$.betroffen")).isEqualTo(2);
    assertThat(vorschau.<Integer>json("$.regelA")).isEqualTo(2);
    assertThat(vorschau.<Integer>json("$.regelB")).isZero();

    assertThat(feldVon(alle.get(0), "pflegestatus"))
        .as("Wer den Modus vergisst oder die Vorschau holt, veraendert nichts")
        .isEqualTo("OFFEN");
    assertThat(feldVon(alle.get(1), "pflegestatus")).isEqualTo("OFFEN");
  }

  /**
   * <b>Ausfuehren setzt genau den Status — und sonst nichts.</b>
   *
   * <p>Das ist der Kern von E22: Partner und Richtung stehen bereits in der Zeile, die Uebernahme
   * kopiert keinen Feldwert. Und {@code vorschlag_herkunft} bleibt stehen, weil sie danach der
   * einzige Hinweis darauf ist, dass der Wert aus einer Regel und nicht aus einem Kopf stammt.
   */
  @Test
  @DisplayName("Ausfuehren setzt GEPFLEGT und laesst Partner, Richtung und Herkunft stehen (E22)")
  void uebernahme_setzt_genau_den_status() throws Exception {
    String prozess = freieProzesse(1).getFirst();
    legeZeileAn(prozess, Pflegestatus.OFFEN, VorschlagHerkunft.REGEL_A, Richtung.EINGEHEND.name());
    String[] vorher = vermerkVon(prozess);

    Antwort ausgefuehrt =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("AUSFUEHREN"));

    assertThat(ausgefuehrt.status()).isEqualTo(200);
    assertThat(ausgefuehrt.<String>json("$.modus")).isEqualTo("AUSFUEHREN");
    assertThat(ausgefuehrt.<Integer>json("$.betroffen")).isEqualTo(1);

    assertThat(feldVon(prozess, "pflegestatus")).isEqualTo("GEPFLEGT");
    assertThat(feldVon(prozess, "partner"))
        .as("Uebernehmen ist eine Statusaenderung und kein Kopieren von Werten")
        .isEqualTo(PARTNER);
    assertThat(feldVon(prozess, "richtung")).isEqualTo("EINGEHEND");
    assertThat(feldVon(prozess, "vorschlagHerkunft"))
        .as("Sie ist der einzige verbliebene Beleg dafuer, dass der Wert aus einer Regel stammt")
        .isEqualTo("REGEL_A");

    String[] nachher = vermerkVon(prozess);
    assertThat(nachher[0]).as("geaendert_am ist neu gesetzt").isNotEqualTo(vorher[0]);
    assertThat(nachher[1])
        .as(
            "geaendert_von traegt den angemeldeten Testnutzer — und der MUSS das Praefix tragen,"
                + " sonst greift die Aufraeumregel aus §7 nicht und die Zeile ueberlebt jeden"
                + " weiteren Lauf auf der geteilten Testkopie")
        .isEqualTo(NUTZER)
        .startsWith(PRAEFIX);
  }

  /**
   * <b>E22 am laufenden Endpunkt</b> — der Fall, der die 224 {@code NEXANS}-Zeilen schuetzt.
   *
   * <p>Eine Zeile mit {@code KEINE} und gefuellter Richtung wird nicht angefasst. „Gepflegt mit
   * leerem Partner" heisst in diesem Katalog ausdruecklich <i>„hingesehen, es gibt keinen"</i> (E4)
   * — ein Knopf, der solche Zeilen mitnaehme, erfaende Behauptungen, die kein Mensch getroffen hat.
   */
  @Test
  @DisplayName("Eine Zeile mit KEINE und gefuellter Richtung bleibt offen (E22)")
  void uebernahme_laesst_zeilen_ohne_partnervorschlag_offen() throws Exception {
    String prozess = freieProzesse(1).getFirst();
    legeZeileAn(prozess, Pflegestatus.OFFEN, VorschlagHerkunft.KEINE, Richtung.AUSGEHEND.name());

    Antwort vorschau =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"));
    assertThat(vorschau.<Integer>json("$.betroffen"))
        .as("Eine Richtung ist kein Partnervorschlag")
        .isZero();

    sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("AUSFUEHREN"));

    assertThat(feldVon(prozess, "pflegestatus")).isEqualTo("OFFEN");
    assertThat(feldVon(prozess, "richtung")).isEqualTo("AUSGEHEND");
    assertThat(feldVon(prozess, "partner")).isNull();
  }

  @Test
  @DisplayName("Eine bereits gepflegte Zeile taucht in betroffen nicht auf und bleibt unberuehrt")
  void uebernahme_faengt_gepflegte_zeilen_nicht_ein() throws Exception {
    String prozess = freieProzesse(1).getFirst();
    legeZeileAn(prozess, Pflegestatus.GEPFLEGT, VorschlagHerkunft.REGEL_B, null);
    String[] vorher = vermerkVon(prozess);

    Antwort vorschau =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"));
    assertThat(vorschau.<Integer>json("$.betroffen"))
        .as("Die Bedingung aus E22 verlangt OFFEN — eine gepflegte Zeile ist schon entschieden")
        .isZero();

    sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("AUSFUEHREN"));

    assertThat(vermerkVon(prozess))
        .as("Der Aenderungsvermerk einer gepflegten Zeile bleibt stehen")
        .isEqualTo(vorher);
  }

  @Test
  @DisplayName("Die leere Menge antwortet mit Nullen und wirft nicht")
  void uebernahme_ohne_vorschlaege_ist_kein_fehler() throws Exception {
    Antwort ausgefuehrt =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("AUSFUEHREN"));

    assertThat(ausgefuehrt.status())
        .as("„es gibt nichts zu uebernehmen\" ist kein Fehler")
        .isEqualTo(200);
    assertThat(ausgefuehrt.<Integer>json("$.betroffen"))
        .as(
            "Bei SUTTONS liefert die Heuristik nichts (M76). Steht hier eine Zahl, hat jemand"
                + " kuratiert — dann ist das der Befund und nicht der Test")
        .isZero();
    assertThat(ausgefuehrt.<Integer>json("$.regelA")).isZero();
    assertThat(ausgefuehrt.<Integer>json("$.regelB")).isZero();
  }

  @Test
  @DisplayName("regelA plus regelB ergibt betroffen — bei gemischtem Bestand")
  void uebernahme_schluesselt_die_herkunft_vollstaendig_auf() throws Exception {
    List<String> alle = freieProzesse(5);
    legeZeileAn(alle.get(0), Pflegestatus.OFFEN, VorschlagHerkunft.REGEL_A, null);
    legeZeileAn(alle.get(1), Pflegestatus.OFFEN, VorschlagHerkunft.REGEL_B, null);
    legeZeileAn(alle.get(2), Pflegestatus.OFFEN, VorschlagHerkunft.REGEL_B, null);
    // Und zwei, die nicht mitzaehlen duerfen: ohne Partnervorschlag und bereits gepflegt.
    legeZeileAn(alle.get(3), Pflegestatus.OFFEN, VorschlagHerkunft.KEINE, null);
    legeZeileAn(alle.get(4), Pflegestatus.GEPFLEGT, VorschlagHerkunft.REGEL_A, null);

    Antwort vorschau =
        sitzung.sende("/api/katalog/vorschlaege-uebernehmen", uebernahme("VORSCHAU"));

    int betroffen = vorschau.json("$.betroffen");
    int regelA = vorschau.json("$.regelA");
    int regelB = vorschau.json("$.regelB");

    assertThat(regelA).isEqualTo(1);
    assertThat(regelB).isEqualTo(2);
    assertThat(regelA + regelB)
        .as("Die Aufschluesselung ist vollstaendig — sonst waere sie als Kontrolle wertlos")
        .isEqualTo(betroffen);
  }
}
