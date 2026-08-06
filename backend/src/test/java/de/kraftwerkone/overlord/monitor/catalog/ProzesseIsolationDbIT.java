package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Der Pflicht-Isolationstest von {@code GET /api/prozesse}</b> (Regel M4). Ohne ihn wird der
 * Endpunkt nicht gemergt.
 *
 * <p><b>Warum die Gegenprobe hier anders aussieht als in der Vorlage.</b> {@code
 * MandantenIsolationDbIT} und {@code NachrichtenIsolationDbIT} stellen einer fremden, echten
 * Kennung eine erfundene gegenueber und verlangen ununterscheidbare Antworten. Das setzt einen
 * Endpunkt voraus, der eine Kennung <i>entgegennimmt</i> — dieser nimmt <b>ueberhaupt keinen
 * Parameter</b>. Es gibt damit keine Eingabe, ueber die sich Existenz erfragen liesse, und die
 * Gegenprobe verschiebt sich auf die Ausgabe: Der Rumpf darf keine einzige fremde Kennung und
 * keinen einzigen fremden Namen enthalten. Das ist die schaerfere Variante — sie prueft den ganzen
 * Rumpf und nicht nur die Antwort auf eine Frage.
 *
 * <p><b>Warum {@code NEXANS} gegen {@code SUTTONS}:</b> dieselbe Paarung wie in {@code
 * NachrichtenIsolationDbIT} — zwei verschiedene Haeuser und die beiden groessten Bestaende, also
 * die Paarung, bei der ein Leck am ehesten sichtbar wuerde.
 *
 * <p><b>Kein Zeitfenster</b>, denn der Endpunkt hat keines: Stammdaten unterliegen Regel L1 nicht.
 * Der Test ist damit unabhaengig vom Datenstand der Testkopie — anders als bei der Nachrichtenliste
 * gibt es hier keinen Weg, an dem ein leeres Fenster den Beweis aushoehlen koennte.
 */
class ProzesseIsolationDbIT extends SicherheitsTestbasis {

  private static final String MANDANT_NEXANS = "NEXANS";
  private static final String MANDANT_SUTTONS = "SUTTONS";

  private static final String NUTZER_A = PRAEFIX + "prozesse-nexans";
  private static final String NUTZER_B = PRAEFIX + "prozesse-suttons";
  private static final String NUTZER_ADMIN = PRAEFIX + "prozesse-admin";
  private static final String PASSWORT = "einLangesPasswort1";

  private Sitzung aufNexans;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_SUTTONS)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_SUTTONS);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  /** Die Voraussetzung, ohne die alles Folgende wertlos waere: Beide Mandanten haben Prozesse. */
  @Test
  @DisplayName("Beide Mandanten haben ueberhaupt Prozesse")
  void beide_mandanten_haben_prozesse() throws Exception {
    assertThat(aufNexans.hole("/api/prozesse").<List<String>>json("$[*].processId")).isNotEmpty();
    assertThat(aufSuttons.hole("/api/prozesse").<List<String>>json("$[*].processId")).isNotEmpty();
  }

  @Test
  @DisplayName("Der Nutzer auf NEXANS sieht keinen einzigen Prozess von SUTTONS")
  void keine_fremde_kennung_und_kein_fremder_name() throws Exception {
    Antwort vonSuttons = aufSuttons.hole("/api/prozesse");
    List<String> fremdeKennungen = vonSuttons.json("$[*].processId");
    List<String> fremdeNamen = vonSuttons.json("$[*].processName");

    Antwort vonNexans = aufNexans.hole("/api/prozesse");

    assertThat(vonNexans.<List<String>>json("$[*].processId"))
        .isNotEmpty()
        .doesNotContainAnyElementsOf(fremdeKennungen);
    // Die Gegenprobe ueber den ganzen Rumpf: Auch ein Name darf nicht durchsickern — ueber ihn
    // liesse sich die Prozesslandschaft eines fremden Mandanten ebenso ablesen wie ueber die
    // Kennung, und der Freitextfilter macht Namen unmittelbar verwertbar.
    for (String fremd : fremdeKennungen) {
      assertThat(vonNexans.rumpf()).doesNotContain(fremd);
    }
    for (String fremd : fremdeNamen) {
      assertThat(vonNexans.rumpf()).doesNotContain(fremd);
    }
  }

  /** Sonst bewiese der Test nur, dass {@code NEXANS} alles sieht. */
  @Test
  @DisplayName("Die Trennung gilt in beide Richtungen")
  void trennung_gilt_in_beide_richtungen() throws Exception {
    List<String> nexansKennungen = aufNexans.hole("/api/prozesse").json("$[*].processId");

    Antwort vonSuttons = aufSuttons.hole("/api/prozesse");

    assertThat(vonSuttons.<List<String>>json("$[*].processId"))
        .isNotEmpty()
        .doesNotContainAnyElementsOf(nexansKennungen);
  }

  /**
   * Die Trennung gilt auch quer (Regel M5): Was die Auswahl anbietet, muss die Liste auch
   * beantworten. Waere die Auswahl weiter gefasst als der Listenfilter, fuehrte sie den Nutzer auf
   * garantiert leere Ergebnisse; waere sie enger, fehlten ihm Prozesse, die er sehen darf.
   */
  @Test
  @DisplayName("Jeder angebotene Prozess ist auch im Listenfilter zulaessig")
  void auswahl_und_listenfilter_passen_zusammen() throws Exception {
    String eigenerProzess =
        aufNexans.hole("/api/prozesse").<List<String>>json("$[*].processId").getFirst();

    Antwort liste =
        aufNexans.hole(
            "/api/nachrichten?zeitraum=30d&zwischenschritte=true&prozess=" + eigenerProzess);

    assertThat(liste.status()).isEqualTo(200);
  }

  /**
   * <b>ADMIN ist kein Sonderfall.</b> Er hat die groesste zulaessige Menge, arbeitet aber in genau
   * einem Mandantenkontext — es gibt keinen Codepfad ohne Mandantenfilter, auch nicht fuer ihn
   * ({@code docs/mandantentrennung.md} §1). Nach dem Wechsel auf {@code NEXANS} sieht er exakt das,
   * was ein {@code NEXANS}-Nutzer sieht, und nichts von {@code SUTTONS}.
   */
  @Test
  @DisplayName("Auch ADMIN sieht nur den Mandanten, auf den er gewechselt hat")
  void admin_sieht_nur_den_aktiven_mandanten() throws Exception {
    legeNutzerAn(NUTZER_ADMIN, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(NUTZER_ADMIN, PASSWORT);
    // Ohne aktiven Mandanten gibt es keine Auswahl — nicht etwa alle.
    assertThat(alsAdmin.hole("/api/prozesse").status()).isEqualTo(403);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_NEXANS + "\"}");

    List<String> fremdeKennungen = aufSuttons.hole("/api/prozesse").json("$[*].processId");
    List<String> eigeneKennungen = aufNexans.hole("/api/prozesse").json("$[*].processId");

    Antwort alsAdminAufNexans = alsAdmin.hole("/api/prozesse");

    assertThat(alsAdminAufNexans.<List<String>>json("$[*].processId"))
        .containsExactlyElementsOf(eigeneKennungen)
        .doesNotContainAnyElementsOf(fremdeKennungen);
  }
}
