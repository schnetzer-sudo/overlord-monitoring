package de.kraftwerkone.overlord.monitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.Rollupzeitraum;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
 *       Sie ist ueber den Antwortrumpf <b>nicht</b> pruefbar — und das ist ein Befund und keine
 *       Nebensache, siehe den Kasten unten. Geprueft wird sie deshalb <b>am Repository</b>: {@link
 *       #die_kennzahlenkette_liefert_keine_fremde_zeile()}.
 * </ul>
 *
 * <h2>⚠️ Warum eine Zusicherung dieses Tests am Repository haengt und nicht am Rumpf</h2>
 *
 * <p><b>Gemessen in der Verletzungsprobe vom 02.09.2026</b>, und das Ergebnis war nicht das
 * erwartete:
 *
 * <table border="1">
 *   <caption>Verletzungsprobe: ein Filter entfernt, was wird rot?</caption>
 *   <tr><th>entfernt</th><th>Ergebnis</th></tr>
 *   <tr><td>Mandantenfilter des Geruests</td><td><b>5 von 9 Faellen rot</b>, darunter der
 *       Partnername</td></tr>
 *   <tr><td>Mandantenkette der Kennzahlen</td><td><b>alle 9 gruen</b></td></tr>
 * </table>
 *
 * <p><b>Der Grund ist die Bauform und nicht ein Loch im Test.</b> Der Dienst haengt die Kennzahlen
 * ueber die {@code ProcessID} an die Blaetter des Geruests. Blaetter, die es nicht gibt, bekommen
 * nichts — eine fremde Kennzahlzeile faellt beim Zusammensetzen lautlos heraus, und sie kann den
 * Rumpf gar nicht erreichen. Fuer die <i>Sicherheit</i> ist das gut (die Trennung haengt an zwei
 * unabhaengigen Riegeln), fuer den <i>Test</i> ist es das Gegenteil: Ein Leck im zweiten Statement
 * bliebe unsichtbar, bis jemand die Bauform des Dienstes aendert.
 *
 * <p><b>Regel M3 verlangt den Filter im Statement und nicht dahinter.</b> Ein Test, der nur den
 * Rumpf ansieht, waere mit einer nachgelagerten Pruefung zufrieden — also mit genau dem, was M3
 * verbietet. Deshalb greift die eine Zusicherung eine Ebene tiefer.
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

  /**
   * Fuer die eine Zusicherung, die eine Ebene tiefer greifen muss. Sie ist der Ausweg aus dem
   * Befund im Kasten oben und keine Bequemlichkeit — ueber HTTP ist die zweite Mandantenkette nicht
   * erreichbar.
   */
  @Autowired private ProzessbaumRepository prozessbaumRepository;

  /** Die Anwendungsuhr (Regel Z1) — im Profil {@code dev} die zurueckversetzte. */
  @Autowired private Clock anwendungsuhr;

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
   * <b>Die zweite Mandantenkette, dort geprueft, wo sie steht.</b>
   *
   * <p>Diese Zusicherung greift auf das Repository und nicht auf den Rumpf, und der Grund steht im
   * Kasten der Klassendokumentation: Ueber den Rumpf ist sie <b>nicht</b> pruefbar — die
   * Verletzungsprobe hat es gemessen. Der Dienst haengt die Kennzahlen ueber die {@code ProcessID}
   * an die Blaetter des Geruests; eine fremde Zeile faellt dabei lautlos heraus und erreicht die
   * Antwort nie.
   *
   * <p>Geprueft wird ueber <b>zwoelf Monate</b> und nicht ueber das Standardfenster: Ein enges
   * Fenster liefert wenige Zeilen, und eine Zusicherung ueber wenige Zeilen findet ein Leck
   * schlechter als eine ueber viele.
   *
   * <p><b>Regel T2:</b> Der Test nennt keine Zahl. Er verlangt, dass die eigene Antwort nicht leer
   * ist — sonst bewiese er nichts — und dass keine einzige fremde Kennung darin steht.
   */
  @Test
  @DisplayName("Die Kennzahlenabfrage liefert keine Zeile eines fremden Mandanten")
  void die_kennzahlenkette_liefert_keine_fremde_zeile() {
    List<String> fremdeProzesse =
        prozessbaumRepository.geruest(new MandantContext(MANDANT_SUTTONS)).stream()
            .map(Prozessgeruestzeile::processId)
            .toList();

    List<String> ausDenKennzahlen =
        prozessbaumRepository
            .kennzahlen(
                new MandantContext(MANDANT_NEXANS),
                Rollupzeitraum.MONATE_12,
                Rollupzeitraum.MONATE_12.fenster(LocalDateTime.now(anwendungsuhr)))
            .stream()
            .map(Prozesskennzahlzeile::processId)
            .toList();

    assertThat(fremdeProzesse).as("Ohne fremde Prozesse bewiese die Probe nichts").isNotEmpty();
    assertThat(ausDenKennzahlen)
        .as("Ohne eigene Zeilen bewiese die Probe nichts")
        .isNotEmpty()
        .doesNotContainAnyElementsOf(fremdeProzesse);
  }

  /**
   * Die Kopfzahl ist genau die Summe der Blaetter — sie enthaelt nichts, was nicht an einem eigenen
   * Blatt haengt.
   *
   * <p><b>Diese Zusicherung prueft die Mandantentrennung nicht</b>, und das steht hier, weil der
   * erste Bau dieses Tests genau das behauptet hat und die Verletzungsprobe es widerlegt hat. Sie
   * prueft die <i>Zusammensetzung</i> der Antwort: dass keine Ebene eine Zahl traegt, die ihre
   * Kinder nicht hergeben.
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
