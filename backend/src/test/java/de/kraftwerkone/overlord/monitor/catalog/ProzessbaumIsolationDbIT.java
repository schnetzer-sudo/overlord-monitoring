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
 * <b>Der Pflicht-Isolationstest von {@code GET /api/prozesse/baum}</b> (Regel M4). Ohne ihn wird
 * der Endpunkt nicht gemergt.
 *
 * <h2>Warum die Gegenprobe hier auf der Ausgabe liegt</h2>
 *
 * <p>{@code MandantenIsolationDbIT} und {@code NachrichtenIsolationDbIT} stellen einer fremden,
 * echten Kennung eine erfundene gegenueber und verlangen ununterscheidbare Antworten. Das setzt
 * einen Endpunkt voraus, der eine Kennung <i>entgegennimmt</i> — dieser nimmt <b>keine</b>
 * entgegen; sein einziger Parameter ist der Zeitraum. Es gibt damit keine Eingabe, ueber die sich
 * Existenz erfragen liesse, und die Gegenprobe verschiebt sich auf die <b>Ausgabe</b>: Der Rumpf
 * darf keine einzige fremde Kennung, keinen fremden Namen und keinen fremden Partnernamen
 * enthalten. Dieselbe Bauform wie in {@code ProzesseIsolationDbIT}.
 *
 * <h2>Was hier ueber die Prozessauswahl hinaus zu pruefen ist</h2>
 *
 * <p>Der Baum traegt <b>mehr als die Auswahl</b>: kuratierte <b>Partnernamen</b> und
 * <b>Kennzahlen</b>. Beides sind eigene Leckwege.
 *
 * <ul>
 *   <li><b>Partnernamen sind mandantengebunden</b> ({@code docs/prozess-katalog.md} E3): {@code
 *       BAYER} bei {@code VOTG} und bei {@code SUTTONS} sind zwei Werte, dieselbe Firma, zwei
 *       EDI-Beziehungen. Wer die Partnerliste eines fremden Mandanten liest, liest dessen
 *       Geschaeftsbeziehungen.
 *   <li><b>Die Kennzahlen kommen aus einer zweiten Abfrage mit einer zweiten Mandantenkette.</b>
 *       Sie koennte fuer sich allein leck sein, ohne dass ein einziger fremder Prozess im Baum
 *       auftauchte — die Summe waere dann still zu hoch. Deshalb prueft {@link
 *       #summen_bleiben_unter_dem_eigenen_bestand()} sie gegen die eigene Obergrenze.
 * </ul>
 *
 * <h2>Regel T2: keine Zahl aus dem Pflegestand</h2>
 *
 * <p>Der Test nennt <b>keinen</b> Erwartungswert aus dem Bestand. Er vergleicht ausschliesslich
 * zwei Antworten miteinander und prueft Eigenschaften, die von der Kuratierung unabhaengig sind. Wo
 * eine Zusicherung ohne kuratierte Daten leer liefe, steht das ausdruecklich dabei.
 */
class ProzessbaumIsolationDbIT extends SicherheitsTestbasis {

  private static final String PFAD = "/api/prozesse/baum";

  private static final String MANDANT_NEXANS = "NEXANS";
  private static final String MANDANT_SUTTONS = "SUTTONS";

  /**
   * Der dritte Mandant, und er steht hier fuer <b>eine</b> Zusicherung: {@code SUTTONS} traegt
   * keine einzige Katalogzeile, seine Antwort enthaelt also gar keinen Partnernamen. Der
   * Partner-Leckweg liesse sich gegen ihn nicht pruefen — die Zusicherung waere leer und faende nie
   * etwas.
   */
  private static final String MANDANT_VOTG = "VOTG";

  private static final String NUTZER_A = PRAEFIX + "baum-nexans";
  private static final String NUTZER_B = PRAEFIX + "baum-suttons";
  private static final String NUTZER_C = PRAEFIX + "baum-votg";
  private static final String NUTZER_ADMIN = PRAEFIX + "baum-admin";
  private static final String PASSWORT = "einLangesPasswort1";

  private Sitzung aufNexans;
  private Sitzung aufSuttons;
  private Sitzung aufVotg;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_SUTTONS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_VOTG)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_SUTTONS);
    legeNutzerAn(NUTZER_C, PASSWORT, Rolle.MANDANT, MANDANT_VOTG);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
    aufVotg = anmelden(NUTZER_C, PASSWORT);
  }

  /** Alle Blattkennungen des Baums, ueber alle Partner und Richtungen. */
  private static List<String> kennungen(Antwort antwort) {
    return antwort.json("$.partner[*].richtungen[*].prozesse[*].processId");
  }

  private static List<String> namen(Antwort antwort) {
    return antwort.json("$.partner[*].richtungen[*].prozesse[*].processName");
  }

  /** Die kuratierten Partnernamen. Ohne Katalogzeilen ist die Liste leer — das ist kein Fehler. */
  private static List<String> partnernamen(Antwort antwort) {
    return antwort.json("$.partner[*].partner");
  }

  /** Die Voraussetzung, ohne die alles Folgende wertlos waere: Beide Mandanten haben Prozesse. */
  @Test
  @DisplayName("Beide Mandanten haben ueberhaupt einen Baum")
  void beide_mandanten_haben_einen_baum() throws Exception {
    assertThat(kennungen(aufNexans.hole(PFAD))).isNotEmpty();
    assertThat(kennungen(aufSuttons.hole(PFAD))).isNotEmpty();
  }

  @Test
  @DisplayName("Der Nutzer auf NEXANS sieht keinen einzigen Prozess von SUTTONS")
  void keine_fremde_kennung_und_kein_fremder_name() throws Exception {
    Antwort vonSuttons = aufSuttons.hole(PFAD);
    List<String> fremdeKennungen = kennungen(vonSuttons);
    List<String> fremdeNamen = namen(vonSuttons);

    Antwort vonNexans = aufNexans.hole(PFAD);

    assertThat(kennungen(vonNexans)).isNotEmpty().doesNotContainAnyElementsOf(fremdeKennungen);
    // Die Gegenprobe ueber den ganzen Rumpf: Auch ein Name darf nicht durchsickern — ueber ihn
    // liesse sich die Prozesslandschaft eines fremden Mandanten ebenso ablesen wie ueber die
    // Kennung.
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
    List<String> nexansKennungen = kennungen(aufNexans.hole(PFAD));

    Antwort vonSuttons = aufSuttons.hole(PFAD);

    assertThat(kennungen(vonSuttons)).isNotEmpty().doesNotContainAnyElementsOf(nexansKennungen);
  }

  /**
   * <b>Der eigene Leckweg des Baums.</b> Ein Partnername ist eine Geschaeftsbeziehung, und er steht
   * nur hier — die Prozessauswahl kennt ihn nicht.
   *
   * <p><b>{@code VOTG} und nicht {@code SUTTONS}</b>: {@code SUTTONS} traegt keine Katalogzeile,
   * gegen ihn liefe die Zusicherung leer. Eine Zahl behauptet der Test trotzdem nicht (Regel T2) —
   * er vergleicht nur, was die andere Antwort tatsaechlich enthaelt.
   */
  @Test
  @DisplayName("Kein kuratierter Partnername eines fremden Mandanten steht im Rumpf")
  void keine_fremden_partnernamen() throws Exception {
    List<String> fremdePartner =
        partnernamen(aufVotg.hole(PFAD)).stream().filter(name -> name != null).toList();

    Antwort vonNexans = aufNexans.hole(PFAD);

    for (String fremd : fremdePartner) {
      assertThat(vonNexans.rumpf())
          .as("Partnername %s von VOTG darf bei NEXANS nirgends stehen", fremd)
          .doesNotContain("\"" + fremd + "\"");
    }
  }

  /**
   * <b>Die zweite Mandantenkette, einzeln geprueft.</b> Die Kennzahlen kommen aus einem eigenen
   * Statement mit einem eigenen {@code EXISTS}. Waere <i>nur dieses</i> leck, taeuchte kein fremder
   * Prozess im Baum auf — die Summen waeren still zu hoch.
   *
   * <p>Geprueft wird gegen eine <b>pflegeunabhaengige Obergrenze</b>: Die Summe ueber den Baum kann
   * niemals groesser sein als die Summe der Blaetter, und jedes Blatt gehoert dem Mandanten. Der
   * Test nennt keine Zahl aus dem Bestand (Regel T2).
   */
  @Test
  @DisplayName("Die Summen des Baums sind genau die Summen seiner eigenen Blaetter")
  void summen_bleiben_unter_dem_eigenen_bestand() throws Exception {
    Antwort vonNexans = aufNexans.hole(PFAD);

    List<Integer> jeBlatt = vonNexans.json("$.partner[*].richtungen[*].prozesse[*].nachrichten");
    long ausBlaettern = jeBlatt.stream().mapToLong(Integer::longValue).sum();
    long gesamt = ((Number) vonNexans.json("$.gesamt.nachrichten")).longValue();

    assertThat(gesamt)
        .as("Die Kopfzahl darf nichts enthalten, was nicht an einem eigenen Blatt haengt")
        .isEqualTo(ausBlaettern);
  }

  /**
   * Die Trennung gilt auch quer (Regel M5): Der Baum und die Prozessauswahl beschreiben dieselbe
   * Menge. Waere der Baum weiter gefasst, zeigte er Prozesse, die der Listenfilter nicht annimmt;
   * waere er enger, fehlten dem Nutzer Prozesse, die er sehen darf.
   */
  @Test
  @DisplayName("Der Baum und die Prozessauswahl beschreiben dieselbe Menge")
  void baum_und_auswahl_decken_sich() throws Exception {
    List<String> ausDerAuswahl = aufNexans.hole("/api/prozesse").json("$[*].processId");

    List<String> ausDemBaum = kennungen(aufNexans.hole(PFAD));

    assertThat(ausDemBaum).containsExactlyInAnyOrderElementsOf(ausDerAuswahl);
  }

  /**
   * Regel M1: Ein {@code ?mandant=…} ist kein Fehler, sondern wirkungslos. Es gibt keinen Codepfad,
   * auf dem ein Parameter den Mandanten der Sitzung ueberschriebe.
   */
  @Test
  @DisplayName("Ein mandant-Parameter ist wirkungslos, nicht wirksam")
  void mandantenparameter_ist_wirkungslos() throws Exception {
    Antwort ohne = aufNexans.hole(PFAD);
    Antwort mit = aufNexans.hole(PFAD + "?mandant=" + MANDANT_SUTTONS);

    assertThat(mit.status()).isEqualTo(200);
    assertThat(kennungen(mit)).containsExactlyElementsOf(kennungen(ohne));
  }

  /**
   * Der Zeitraum aendert die <b>Zahlen</b>, nie den <b>Umfang</b>. Das ist der Kern der Ansicht:
   * Ein Prozess, der im Fenster nichts getragen hat, steht trotzdem im Baum — und er kann ueber
   * kein Fenster verschwinden.
   */
  @Test
  @DisplayName("Ueber alle drei Zeitraeume steht derselbe Baum")
  void der_umfang_haengt_nicht_am_zeitraum() throws Exception {
    List<String> ausDerVorgabe = kennungen(aufNexans.hole(PFAD));

    for (String code : List.of("48H", "30T", "12M")) {
      Antwort antwort = aufNexans.hole(PFAD + "?zeitraum=" + code);
      assertThat(antwort.status()).as("Zeitraum %s", code).isEqualTo(200);
      assertThat(kennungen(antwort))
          .as("Zeitraum %s", code)
          .containsExactlyElementsOf(ausDerVorgabe);
    }
  }

  /**
   * <b>ADMIN ist kein Sonderfall.</b> Er hat die groesste zulaessige Menge, arbeitet aber in genau
   * einem Mandantenkontext — es gibt keinen Codepfad ohne Mandantenfilter, auch nicht fuer ihn
   * ({@code docs/mandantentrennung.md} §1).
   */
  @Test
  @DisplayName("Auch ADMIN sieht nur den Mandanten, auf den er gewechselt hat")
  void admin_sieht_nur_den_aktiven_mandanten() throws Exception {
    legeNutzerAn(NUTZER_ADMIN, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(NUTZER_ADMIN, PASSWORT);
    // Ohne aktiven Mandanten gibt es keinen Baum — nicht etwa alle.
    assertThat(alsAdmin.hole(PFAD).status()).isEqualTo(403);

    alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_NEXANS + "\"}");

    List<String> fremdeKennungen = kennungen(aufSuttons.hole(PFAD));
    List<String> eigeneKennungen = kennungen(aufNexans.hole(PFAD));

    Antwort alsAdminAufNexans = alsAdmin.hole(PFAD);

    assertThat(kennungen(alsAdminAufNexans))
        .containsExactlyElementsOf(eigeneKennungen)
        .doesNotContainAnyElementsOf(fremdeKennungen);
  }
}
