package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.PROCESS_CATALOG;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.FehlerLiveRepository;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZeile;
import de.kraftwerkone.overlord.monitor.common.FehlerLiveZustand;
import de.kraftwerkone.overlord.monitor.common.LiveRestZustand;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.Pflegestatus;
import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <b>Der Pflicht-Isolationstest des Dashboards</b> (Regel M4). Ohne ihn wird nicht gemergt.
 *
 * <h2>Warum die Trennung hier schwerer nachzuweisen ist als bei einer Liste</h2>
 *
 * <p>Eine Liste liefert Zeilen; man haelt die Kennungen des einen Mandanten gegen den Rumpf des
 * anderen und ist fertig. <b>Ein Dashboard liefert Zahlen</b> — und eine Zahl, die den fremden
 * Bestand mitzaehlt, sieht genauso aus wie eine richtige. Sie ist sogar plausibel.
 *
 * <p>Der Nachweis haengt deshalb an den wenigen Stellen, an denen die Antwort <b>Kennungen</b>
 * traegt: der Prozesskennung in „Zuletzt aufgefallen" und den kuratierten Werten des
 * Verteilungsblocks. Beide werden gegen die <b>vollstaendige Prozessliste</b> des anderen Mandanten
 * gehalten ({@code GET /api/prozesse}) — und zwar gegen den ganzen Antwortrumpf, nicht nur gegen
 * ein Feld.
 *
 * <p><b>Die Prozessliste ist dabei die stabile Bezugsgroesse</b> (Regel T2): Welche Prozesse einem
 * Mandanten gehoeren, haengt an {@code Process} und {@code ProjectMandant} und nicht am Pflegestand
 * des Katalogs. Ein Nachweis ueber Partnernamen waere beim naechsten Kuratierungsschritt eine
 * Momentaufnahme.
 *
 * <p><b>Die Paarung ist die der Vorlage:</b> {@code VOTG} gegen {@code SUTTONS} — zwei Mandanten
 * aus <b>verschiedenen Haeusern</b>. {@code NXHBE} und {@code IBISGUS} scheiden aus; zwei Mandanten
 * desselben Konzerns sind ein schlechter Beweis fuer eine Trennung, die zwischen Firmen greift.
 *
 * <p>Dazu kommen zwei Mandanten, die keine Trennung pruefen, sondern zwei Verhaltensweisen: {@code
 * NEXANS} fuer das Standardfenster und {@code EDITIONLINGERI} fuer den Leerzustand — dort gibt es
 * im <b>ganzen</b> Bestand keine einzige Nachricht (M95), der Leerzustand ist also lokal pruefbar.
 */
class DashboardIsolationDbIT extends SicherheitsTestbasis {

  private static final String NUTZER_A = PRAEFIX + "dash-votg";
  private static final String NUTZER_B = PRAEFIX + "dash-suttons";
  private static final String NUTZER_GROSS = PRAEFIX + "dash-nexans";
  private static final String NUTZER_LEER = PRAEFIX + "dash-leer";
  private static final String PASSWORT = "einLangesPasswort1";

  /** Der groesste Mandant — er traegt bei allen drei Paaren 100 % belegte Eimer (M95). */
  private static final String MANDANT_GROSS = "NEXANS";

  /**
   * Der Mandant ohne eine einzige Nachricht im ganzen Bestand (M95). <b>Der Leerzustand ist damit
   * lokal pruefbar</b> und haengt nicht daran, dass gerade ein Fenster leer ist.
   */
  private static final String MANDANT_LEER = "EDITIONLINGERI";

  private Sitzung aufVotg;
  private Sitzung aufSuttons;
  private Sitzung aufNexans;
  private Sitzung aufLeer;

  /** Fuer die Nachlesung am Repository (Teil B) — der Dienst verwirft fremde Zeilen lautlos. */
  @Autowired private DashboardRepository dashboardRepository;

  /** Fuer die Fehlerlesung am Repository (Fehler live, E-208) — aus demselben Grund. */
  @Autowired private FehlerLiveRepository fehlerLiveRepository;

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  /** Die Katalogzeile, die sich {@link #nachlesung_liefert_keine_fremde_zeile} selbst anlegt. */
  private static final String KATALOG_VON = PRAEFIX + "dash-live";

  @AfterEach
  void raeumeEigeneKatalogzeileWeg() {
    monitorDsl
        .deleteFrom(PROCESS_CATALOG)
        .where(PROCESS_CATALOG.GEAENDERT_VON.eq(KATALOG_VON))
        .execute();
  }

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(MANDANT_A)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_GROSS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_LEER)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, MANDANT_A);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    legeNutzerAn(NUTZER_GROSS, PASSWORT, Rolle.MANDANT, MANDANT_GROSS);
    legeNutzerAn(NUTZER_LEER, PASSWORT, Rolle.MANDANT, MANDANT_LEER);
    aufVotg = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
    aufNexans = anmelden(NUTZER_GROSS, PASSWORT);
    aufLeer = anmelden(NUTZER_LEER, PASSWORT);
  }

  private static String pfad(String zeitraum) {
    return "/api/dashboard?zeitraum=" + zeitraum;
  }

  private static List<String> prozesseVon(Sitzung sitzung)
      throws IOException, InterruptedException {
    Antwort liste = sitzung.hole("/api/prozesse");
    assertThat(liste.status()).isEqualTo(200);
    List<String> kennungen = liste.json("$[*].processId");
    assertThat(kennungen).as("Ohne Prozesse bewiese der Test nur, dass leer leer ist").isNotEmpty();
    return kennungen;
  }

  /** Kein Prozess des einen Mandanten kommt im Rumpf des anderen vor — an keiner Stelle. */
  private static void ohneFremdeProzesse(Antwort antwort, List<String> fremdeProzesse) {
    String rumpf = antwort.rumpf();
    for (String fremd : fremdeProzesse) {
      assertThat(rumpf)
          .as("Die Prozesskennung %s gehoert einem anderen Mandanten", fremd)
          .doesNotContain(fremd);
    }
  }

  // ─── Die Voraussetzung ───────────────────────────────────────────────────────

  /**
   * Ohne sie waere alles Folgende wertlos: Beide Mandanten sehen ueberhaupt Zahlen, und zwar
   * <b>verschiedene</b>. Waeren beide leer, bewiese die Trennung unten nur, dass null gleich null
   * ist.
   */
  @Test
  @DisplayName("Beide Mandanten sehen ihre eigenen Zahlen, und es sind verschiedene")
  void beide_mandanten_sehen_eigene_zahlen() throws Exception {
    Antwort votg = aufVotg.hole(pfad("12M"));
    Antwort suttons = aufSuttons.hole(pfad("12M"));

    assertThat(votg.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    long vonVotg = ((Number) votg.<Object>json("$.kacheln.nachrichten")).longValue();
    long vonSuttons = ((Number) suttons.<Object>json("$.kacheln.nachrichten")).longValue();

    assertThat(vonVotg).isPositive();
    assertThat(vonSuttons).isPositive();
    assertThat(vonVotg)
        .as("Waeren beide Zahlen gleich, bewiese ein Unterschied unten nichts")
        .isNotEqualTo(vonSuttons);
  }

  // ─── Regel M4 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Keine Prozesskennung des anderen Mandanten steht im Rumpf — in keinem Paar")
  void keine_fremden_prozesse() throws Exception {
    List<String> vonSuttons = prozesseVon(aufSuttons);
    List<String> vonVotg = prozesseVon(aufVotg);

    for (String zeitraum : List.of("48H", "30T", "12M")) {
      ohneFremdeProzesse(aufVotg.hole(pfad(zeitraum)), vonSuttons);
      ohneFremdeProzesse(aufSuttons.hole(pfad(zeitraum)), vonVotg);
    }
  }

  /**
   * <b>Die schaerfste Zusicherung dieses Tests, und sie ist erst nach der Verletzungsprobe
   * entstanden.</b>
   *
   * <p>Der Test darueber — kein fremder Prozess im Rumpf — blieb bei ausgehaengtem Mandantenfilter
   * <b>gruen</b>, und der Grund ist strukturell: Eine <i>aggregierte</i> Antwort traegt kaum
   * Kennungen. Der Verteilungsblock zeigt Partnernamen, der Verlauf nur Zahlen. Die zehn Zeilen aus
   * „Zuletzt aufgefallen" waren zufaellig alle von {@code NEXANS} — und gegen dessen Prozessliste
   * hat der Test gar nicht geprueft.
   *
   * <p><b>Diese Fassung dreht die Frage um:</b> Jede gezeigte Prozesskennung muss in der
   * <b>eigenen</b> Prozessliste stehen. Damit faellt sie, egal welchem fremden Mandanten die Zeile
   * gehoert — und sie faellt schon dann, wenn nur der Filter dieses einen Blocks ausfaellt.
   */
  @Test
  @DisplayName("Jede gezeigte Prozesskennung gehoert dem eigenen Mandanten")
  void nur_eigene_prozesse_in_den_zeilen() throws Exception {
    int gezeigteZeilen = 0;
    for (Sitzung sitzung : List.of(aufVotg, aufSuttons, aufNexans)) {
      List<String> eigene = prozesseVon(sitzung);
      for (String zeitraum : List.of("48H", "30T", "12M")) {
        List<String> gezeigt =
            sitzung.hole(pfad(zeitraum)).json("$.zuletztAufgefallen[*].processId");
        gezeigteZeilen += gezeigt.size();
        assertThat(eigene)
            .as("Fremde Prozesskennung in „Zuletzt aufgefallen“ (%s)", zeitraum)
            .containsAll(gezeigt);
      }
    }
    assertThat(gezeigteZeilen)
        .as("Zeigt kein Mandant eine einzige Zeile, bewiese der Vergleich oben nichts")
        .isPositive();
  }

  // Hier stand bis zum 16.09.2026 „Auch die Richtungssicht traegt nichts Fremdes": ein Aufruf mit
  // `?verteilung=RICHTUNG` und die Suche nach fremden Prozesskennungen im Rumpf. Den Parameter gibt
  // es nicht mehr — und der Test trug seine Aussage auch vorher nicht: Der Verteilungsblock zeigt
  // Partner und Richtungen, keine Prozesskennungen, also blieb er bei ausgehaengtem Filter gruen.
  // Dieselbe Falle wie in docs/dashboard.md §9. An seine Stelle treten die beiden Summentests
  // darunter, je Sicht einer; `keine_fremden_prozesse` prueft den ganzen Rumpf ohnehin weiter mit.

  /**
   * <b>Regel M4 fuer das Richtungsstatement, einzeln</b> (seit dem 16.09.2026).
   *
   * <h2>Warum eine Summe und keine Kennung</h2>
   *
   * <p>Der Block traegt Richtungen, und eine Richtung gehoert keinem Mandanten — sie ist kein
   * Beweis fuer irgendetwas. <b>Die Summe schon:</b> Die Zeilen einer Sicht zaehlen zusammen —
   * benannte Werte, „Übrige" und „nicht zugeordnet" — genau die Nachrichten des Fensters, denn der
   * Katalog haengt als {@code LEFT JOIN} ueber seinen Primaerschluessel an und vervielfacht nichts.
   * Diese Zahl steht in derselben Antwort noch einmal, als Kachel <i>Nachrichten</i>, und die kommt
   * aus einem <b>anderen</b> Statement (dem Verlauf).
   *
   * <p><b>Faellt der Mandantenfilter nur im Richtungsstatement, zaehlt die Sicht den ganzen Bestand
   * des Fensters, die Kachel weiter nur den eigenen</b> — und die beiden Zahlen gehen auseinander.
   * Das ist pflegeunabhaengig (Regel T2): Die Gleichheit haengt an keinem Katalogstand und an
   * keiner Zahl der Testkopie. Fallen beide Filter zugleich, bliebe sie bestehen; den Fall faengt
   * {@code summen_sind_verschieden}.
   */
  @Test
  @DisplayName("Richtungssicht: ihre Zeilen zaehlen genau die Nachrichten des eigenen Mandanten")
  void richtungssicht_ist_getrennt() throws Exception {
    sichtZaehltDieEigenenNachrichten("richtung");
  }

  /**
   * Dasselbe fuer die Partnersicht. <b>Es steht als eigener Test da</b>, damit eine
   * Verletzungsprobe, die den Filter nur in <i>einem</i> der beiden Statements aushaengt, genau
   * einen der beiden faellt — und die Meldung sagt, welchen.
   */
  @Test
  @DisplayName("Partnersicht: ihre Zeilen zaehlen genau die Nachrichten des eigenen Mandanten")
  void partnersicht_ist_getrennt() throws Exception {
    sichtZaehltDieEigenenNachrichten("partner");
  }

  private void sichtZaehltDieEigenenNachrichten(String sicht)
      throws IOException, InterruptedException {
    for (Sitzung sitzung : List.of(aufVotg, aufSuttons)) {
      for (String zeitraum : List.of("48H", "30T", "12M")) {
        Antwort antwort = sitzung.hole(pfad(zeitraum));
        assertThat(antwort.status()).isEqualTo(200);

        long kachel = ((Number) antwort.<Object>json("$.kacheln.nachrichten")).longValue();
        List<Number> zeilen = antwort.json("$.verteilung." + sicht + ".zeilen[*].anzahl");
        long summe = zeilen.stream().mapToLong(Number::longValue).sum();

        assertThat(summe)
            .as(
                "Die Sicht %s zaehlt %s Nachrichten, die Kachel %s — ohne Mandantenfilter im"
                    + " Verteilungsstatement saehe die Sicht den ganzen Bestand (%s)",
                sicht, summe, kachel, zeitraum)
            .isEqualTo(kachel);
      }
    }
  }

  /**
   * <b>Die Gegenprobe auf der Zahl selbst.</b> Fiele der Mandantenfilter aus, saehe jeder Mandant
   * dieselbe Summe — naemlich die des ganzen Bestands. Der Vergleich zweier verschiedener Summen
   * ueber <b>dasselbe</b> Fenster faellt genau dann, und er faellt auch dann, wenn im Rumpf keine
   * einzige Kennung steht.
   */
  @Test
  @DisplayName("Die Summen zweier Mandanten ueber dasselbe Fenster sind verschieden")
  void summen_sind_verschieden() throws Exception {
    for (String zeitraum : List.of("48H", "30T", "12M")) {
      long votg =
          ((Number) aufVotg.hole(pfad(zeitraum)).<Object>json("$.kacheln.nachrichten")).longValue();
      long suttons =
          ((Number) aufSuttons.hole(pfad(zeitraum)).<Object>json("$.kacheln.nachrichten"))
              .longValue();

      assertThat(votg)
          .as("Ohne Mandantenfilter saehen beide dieselbe Summe (%s)", zeitraum)
          .isNotEqualTo(suttons);
    }
  }

  // ─── Regel M1 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Der Endpunkt nimmt keine Mandanten-ID entgegen (Regel M1)")
  void keine_mandanten_id() throws Exception {
    Antwort ohneParameter = aufVotg.hole(pfad("30T"));
    Antwort mitParameter = aufVotg.hole(pfad("30T") + "&mandant=" + MANDANT_B);
    Antwort mitMandantId = aufVotg.hole(pfad("30T") + "&mandantId=" + MANDANT_B);

    assertThat(mitParameter.status()).isEqualTo(200);
    assertThat(mitParameter.rumpf())
        .as("Ein unbekannter Parameter aendert nichts — der Mandant kommt aus der Sitzung")
        .isEqualTo(ohneParameter.rumpf());
    assertThat(mitMandantId.rumpf()).isEqualTo(ohneParameter.rumpf());
  }

  // ─── Standardfenster und Leerzustand ─────────────────────────────────────────

  @Test
  @DisplayName("NEXANS bekommt ohne Angabe 48 Stunden — und die Antwort nennt das Paar")
  void standardfenster_nexans() throws Exception {
    Antwort antwort = aufNexans.hole("/api/dashboard");

    assertThat(antwort.status()).isEqualTo(200);
    assertThat(antwort.<String>json("$.zeitraum"))
        .as("Der groesste Mandant traegt bei 48 Stunden 100 %% belegte Eimer (M95)")
        .isEqualTo("48H");
    assertThat(antwort.<Boolean>json("$.leer")).isFalse();
    assertThat(antwort.<List<Object>>json("$.verlauf")).isNotEmpty();
  }

  /**
   * <b>Der Leerzustand, und er unterscheidet nicht.</b> {@code EDITIONLINGERI} hat im ganzen
   * Bestand keine Nachricht; ein stiller Sonntag saehe genauso aus. Der Satz, den die Oberflaeche
   * dann zeigt, ist in beiden Faellen wahr — das ist die bekannte Grenze und kein Fehler.
   */
  @Test
  @DisplayName("EDITIONLINGERI bekommt den Leerzustand und keinen Fehler")
  void leerzustand() throws Exception {
    Antwort antwort = aufLeer.hole("/api/dashboard");

    assertThat(antwort.status()).as("Leer ist kein Fehler").isEqualTo(200);
    assertThat(antwort.<Boolean>json("$.leer")).isTrue();
    assertThat(antwort.<String>json("$.zeitraum"))
        .as("Auch im Leerzustand nennt die Antwort ein Paar — die Oberflaeche braucht eines")
        .isEqualTo("48H");
    assertThat(antwort.<List<Object>>json("$.verlauf")).isEmpty();
    assertThat(((Number) antwort.<Object>json("$.kacheln.nachrichten")).longValue()).isZero();
    assertThat(antwort.<List<String>>json("$.verteilung.partner.zeilen[*].art"))
        .as("Auch hier sagt der Katalog etwas: alles nicht zugeordnet, naemlich null")
        .containsExactly("NICHT_ZUGEORDNET");
    assertThat(antwort.<List<String>>json("$.verteilung.richtung.zeilen[*].art"))
        .as("Und in der Richtungssicht dasselbe")
        .containsExactly("NICHT_ZUGEORDNET");
  }

  @Test
  @DisplayName("Der Leerzustand gilt in jedem Paar, nicht nur im gewaehlten")
  void leerzustand_in_jedem_paar() throws Exception {
    for (String zeitraum : List.of("48H", "30T", "12M")) {
      assertThat(aufLeer.hole(pfad(zeitraum)).<Boolean>json("$.leer")).as("%s", zeitraum).isTrue();
    }
  }

  // ─── Ein Aufruf ──────────────────────────────────────────────────────────────

  /**
   * <b>Ein Aufruf, eine Antwort.</b> Alle Bloecke stehen in derselben Antwort; keiner wird
   * nachgeladen. Faellt das, merkt es niemand an einer Zahl — nur an der Ladezeit, und dort erst in
   * Produktion.
   */
  @Test
  @DisplayName("Alle Bloecke stehen in einer Antwort")
  void ein_aufruf() throws Exception {
    Antwort antwort = aufNexans.hole(pfad("48H"));

    assertThat(antwort.rumpf())
        .contains("\"zeitraum\"")
        .contains("\"fenster\"")
        .contains("\"leer\"")
        .contains("\"verlauf\"")
        .contains("\"kacheln\"")
        .contains("\"nachrichten\"")
        .contains("\"fehler\"")
        .contains("\"laeuft\"")
        .contains("\"wartend\"")
        .contains("\"verteilung\"")
        .contains("\"zuletztAufgefallen\"")
        .contains("\"stand\"")
        .contains("\"plattform\"")
        .contains("\"dienste\"")
        .contains("\"ablagen\"");
    assertThat(antwort.hatFeld("$.verteilung.partner.zeilen"))
        .as("Seit dem 16.09.2026 stehen beide Sichten in derselben Antwort — Partner …")
        .isTrue();
    assertThat(antwort.hatFeld("$.verteilung.richtung.zeilen")).as("… und Richtung").isTrue();
  }

  /** Entscheidung E-d: „Unquittiert" ist aus dem MVP genommen — kein Feld, kein Platzhalter. */
  @Test
  @DisplayName("Die Antwort kennt kein „quittiert“-Feld")
  void kein_unquittiert() throws Exception {
    assertThat(aufNexans.hole(pfad("48H")).rumpf().toLowerCase(java.util.Locale.ROOT))
        .as("Die Einordnung QUITTIERT gibt es sehr wohl — die Problemkategorie nicht")
        .doesNotContain("unquittiert");
  }

  /**
   * <b>Die Kachel <i>Laeuft</i> steht bei jedem Mandanten</b> — auch bei einem ohne laufende
   * Nachrichten, und auch im Leerzustand. Dass gerade nichts laeuft, ist eine Auskunft.
   *
   * <p><b>Geprueft wird nicht die Zahl</b>, sondern dass die Kachel da ist und nicht als „nicht
   * ermittelbar" ankommt. Auf der Testkopie ist sie ueberall {@code 0}: {@code RUNNING} kommt dort
   * null Mal vor ({@code docs/message-status.md}). <b>Das ist kein Fehler und keine
   * Abnahmeluecke</b>, aber es heisst, dass dieser Test die Zahl nicht pruefen kann.
   */
  @Test
  @DisplayName("Die Kachel Laeuft steht bei jedem Mandanten und ist ermittelbar")
  void laeuft_steht_bei_jedem_mandanten() throws Exception {
    for (Sitzung sitzung : List.of(aufVotg, aufSuttons, aufNexans, aufLeer)) {
      Antwort antwort = sitzung.hole(pfad("48H"));

      assertThat(antwort.<Boolean>json("$.kacheln.laeuft.ermittelbar")).isTrue();
      assertThat(((Number) antwort.<Object>json("$.kacheln.laeuft.anzahl")).longValue())
          .isNotNegative();
    }
  }

  /**
   * <b>Der Isolationsnachweis der beiden neuen Kachelstatements (Regel M4).</b>
   *
   * <p><b>Er haengt an einer einzigen Eigenschaft der Testkopie, und die ist gemessen:</b> Alle 538
   * wartenden Zeilen gehoeren {@code NEXANS}; kein anderer Mandant hat auch nur eine (M90, in M144
   * am geltenden Anker bestaetigt). <b>Fiele der Mandantenfilter aus, saehe jeder Mandant dieselbe
   * Zahl</b> — naemlich die des ganzen Bestands. Der Vergleich faellt genau dann.
   *
   * <p><b>Warum das die schaerfere Fassung ist als „die Zahl stimmt":</b> Eine Zahl aus dem Bestand
   * waere ein Erwartungswert, der bei der naechsten Neubefuellung rot wird, ohne dass jemand etwas
   * falsch gemacht haette (Regel T2). Der <i>Unterschied</i> zwischen zwei Mandanten ist
   * pflegeunabhaengig — er verschwindet nur, wenn der Filter verschwindet.
   */
  @Test
  @DisplayName("Wartend: zwei Mandanten sehen verschiedene Zahlen (Regel M4)")
  void wartend_ist_je_mandant_verschieden() throws Exception {
    long beiNexans = wartendAnzahl(aufNexans);
    long beiVotg = wartendAnzahl(aufVotg);

    assertThat(beiNexans)
        .as("Ohne Mandantenfilter saehen beide die Zahl des ganzen Bestands")
        .isNotEqualTo(beiVotg);
  }

  private static long wartendAnzahl(Sitzung sitzung) throws IOException, InterruptedException {
    Antwort antwort = sitzung.hole(pfad("48H"));
    assertThat(antwort.<Boolean>json("$.kacheln.wartend.ermittelbar")).isTrue();
    return ((Number) antwort.<Object>json("$.kacheln.wartend.anzahl")).longValue();
  }

  /**
   * <b>Der Isolationsnachweis der Erscheinungsbedingung (Regel M4), und er ist der schaerfste der
   * drei.</b> Sie liefert einen einzelnen {@code boolean}; faellt der Mandantenfilter aus, ist er
   * fuer <b>jeden</b> Mandanten {@code true}, weil es die Bausteine im Bestand gibt.
   *
   * <p><b>Gemessen (M144):</b> {@code NEXANS} und {@code VOTG} haben Prozesse, deren geplanter
   * Ablauf einen {@code SUSPEND}-Baustein traegt; {@code SUTTONS} und die uebrigen sechs nicht.
   * <b>Die Kachel fehlt dort ganz</b> — kein {@code null}, kein {@code sichtbar: false}.
   */
  @Test
  @DisplayName("Die Kachel Wartend fehlt genau dort, wo der Mandant nie suspendiert (E-74)")
  void wartend_erscheint_nur_bei_suspendierenden_ablaeufen() throws Exception {
    assertThat(aufNexans.hole(pfad("48H")).rumpf())
        .as("NEXANS hat suspendierende Ablaeufe (M144)")
        .contains("\"wartend\"");
    assertThat(aufSuttons.hole(pfad("48H")).rumpf())
        .as(
            "SUTTONS hat keine — fiele der Mandantenfilter der Erscheinungsbedingung aus, staende"
                + " die Kachel auch hier")
        .doesNotContain("\"wartend\"");
  }

  /**
   * <b>Und der Fall, um dessentwillen E-74 strukturell entschieden worden ist.</b> {@code VOTG} hat
   * suspendierende Ablaeufe, aber gerade keine wartende Nachricht. <b>Die Kachel steht trotzdem,
   * mit einer Null</b> — <i>heute wartet nichts</i> und <i>dieser Mandant wartet nie</i> sind zwei
   * verschiedene Auskuenfte, und genau das war der Grund, sie nicht ueber {@code anzahl &gt; 0} zu
   * steuern.
   */
  @Test
  @DisplayName("Bei VOTG steht die Kachel mit einer Null — und die Null sagt etwas")
  void bei_votg_steht_die_kachel_mit_einer_null() throws Exception {
    Antwort antwort = aufVotg.hole(pfad("48H"));

    assertThat(antwort.rumpf()).contains("\"wartend\"");
    assertThat(((Number) antwort.<Object>json("$.kacheln.wartend.anzahl")).longValue())
        .as("Eine Null, die eine Auskunft ist — nicht eine fehlende Kachel")
        .isZero();
    assertThat(antwort.<Object>json("$.kacheln.wartend.aeltesteSekunden"))
        .as("Ohne Zeile gibt es kein Alter")
        .isNull();
  }

  /** <b>Die Kategorie <i>Ueberfaellig</i> kommt in keiner Antwort mehr vor</b> (E-71). */
  @Test
  @DisplayName("Die Antwort kennt kein „ueberfaellig“-Feld mehr")
  void kein_ueberfaellig_mehr() throws Exception {
    for (String zeitraum : List.of("48H", "30T", "12M")) {
      assertThat(aufNexans.hole(pfad(zeitraum)).rumpf().toLowerCase(java.util.Locale.ROOT))
          .as("%s", zeitraum)
          .doesNotContain("ueberfaellig")
          .doesNotContain("überfällig");
    }
  }

  // ─── Die Raender ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ein unbekannter Zeitraum ist 400 und nennt die erlaubten Werte")
  void unbekannter_zeitraum() throws Exception {
    Antwort antwort = aufVotg.hole(pfad("7d"));

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/zeitraum-unbekannt");
    assertThat(antwort.<String>json("$.detail")).contains("48H");
  }

  /**
   * <b>{@code verteilung} ist kein Parameter mehr</b> (16.09.2026) — ein mitgeschickter Wert ist
   * wirkungslos wie {@code ?mandant=}, und das gilt auch fuer einen unbekannten.
   *
   * <p>Bis zu diesem Tag stand hier „Eine unbekannte Verteilungssicht ist 400" mit dem Problemtyp
   * {@code verteilung-unbekannt}. Beides ist entfallen: Die Antwort traegt beide Sichten, und es
   * gibt nichts mehr zu waehlen.
   *
   * <p><b>Verglichen wird der Verteilungsblock und nicht der ganze Rumpf.</b> Im Rumpf steht auch
   * das Alter der Dienstlampen, und das laeuft zwischen zwei Aufrufen mit der Anwendungsuhr weiter
   * — eine Gleichheit darueber waere eine Zusicherung ueber Wanduhrzeit (Regel T1).
   */
  @Test
  @DisplayName("Ein mitgeschicktes verteilung ist wirkungslos — auch ein unbekanntes")
  void verteilung_ist_wirkungslos() throws Exception {
    Antwort ohne = aufVotg.hole(pfad("30T"));
    Map<String, Object> ohneBlock = ohne.json("$.verteilung");

    for (String wert : List.of("RICHTUNG", "PARTNER", "BELEGART")) {
      Antwort mit = aufVotg.hole(pfad("30T") + "&verteilung=" + wert);

      assertThat(mit.status()).as("verteilung=%s ist kein Fehler", wert).isEqualTo(200);
      assertThat(mit.<Map<String, Object>>json("$.verteilung"))
          .as("verteilung=%s aendert am Block nichts", wert)
          .isEqualTo(ohneBlock);
    }
  }

  /** Ohne aktiven Mandanten gibt es keinen Zugriff — auch nicht fuer ADMIN (Regeln M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antwortet der Endpunkt 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "dash-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort antwort = alsAdmin.hole("/api/dashboard");

    assertThat(antwort.status())
        .as("Ein Zugriff „einfach ueber alle Mandanten\" existiert nicht")
        .isEqualTo(403);
    assertThat(antwort.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
  }

  // ─── Der plattformweite Block (Schritt 10d, E-116) ───────────────────────────

  /**
   * <b>Zwei Mandanten sehen hier genau dasselbe — und das ist die Zusicherung, nicht die
   * Ausnahme.</b>
   *
   * <p>Der Block sagt nichts ueber Belege, sondern ueber die Anlage, auf der sie laufen: Kennungen
   * von Diensten und Ablagen, kein Prozess, keine Nachricht, keine Zahl eines Mandanten. <b>Waere
   * er je Mandant verschieden, waere genau das der Fehler</b> — dann haenge eine plattformweite
   * Auskunft am Datenausschnitt.
   *
   * <p><b>Verglichen wird alles ausser dem Alter</b> (Regel T1): {@code alterSekunden} und das
   * Alter des Pruefzeitpunkts laufen zwischen zwei HTTP-Aufrufen weiter, und eine Zusicherung
   * darueber waere eine Zusicherung ueber Wanduhrzeit. Der {@code stand} selbst ist davon
   * unberuehrt und wird mitgeprueft.
   */
  @Test
  @DisplayName("Der Block plattform ist fuer NEXANS und SUTTONS identisch")
  void plattform_ist_fuer_jeden_mandanten_gleich() throws Exception {
    Antwort nexans = aufNexans.hole(pfad("48H"));
    Antwort suttons = aufSuttons.hole(pfad("48H"));

    assertThat(nexans.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);

    assertThat(nexans.<List<String>>json("$.plattform.dienste[*].serviceId"))
        .as("Ohne eine einzige Lampe bewiese dieser Vergleich nichts")
        .isNotEmpty()
        .isEqualTo(suttons.<List<String>>json("$.plattform.dienste[*].serviceId"));
    assertThat(nexans.<List<String>>json("$.plattform.dienste[*].zustand"))
        .isEqualTo(suttons.<List<String>>json("$.plattform.dienste[*].zustand"));
    assertThat(nexans.<List<String>>json("$.plattform.dienste[*].rohwert"))
        .isEqualTo(suttons.<List<String>>json("$.plattform.dienste[*].rohwert"));
    assertThat(nexans.<List<String>>json("$.plattform.dienste[*].stand"))
        .isEqualTo(suttons.<List<String>>json("$.plattform.dienste[*].stand"));
    assertThat(nexans.<Object>json("$.plattform.ablagen.zustand"))
        .isEqualTo(suttons.<Object>json("$.plattform.ablagen.zustand"));
    assertThat(nexans.<List<String>>json("$.plattform.ablagen.ziele[*].serviceId"))
        .isEqualTo(suttons.<List<String>>json("$.plattform.ablagen.ziele[*].serviceId"));
  }

  /**
   * <b>Regel G1 am echten Antwortrumpf.</b> {@code PlattformAntwortTest} prueft dieselbe Zusage an
   * den Typen; dieser Test prueft, was tatsaechlich hinausgeht.
   *
   * <p>Gesucht wird nicht der Wert selbst — er darf in keinem Test stehen —, sondern seine
   * <b>Gestalt</b>: Alle elf Verbindungszeichenketten der Ablagen beginnen mit {@code http://}
   * (M52, Befund 4). Taucht das im Rumpf auf, ist eine Adresse mitgekommen.
   */
  @Test
  @DisplayName("Keine Verbindungszeichenkette und kein Betriebstext im Rumpf")
  void keine_verbindung_im_rumpf() throws Exception {
    String rumpf = aufNexans.hole(pfad("48H")).rumpf();

    assertThat(rumpf)
        .doesNotContain("http://")
        .doesNotContain("https://")
        .doesNotContain("serviceConnectString")
        .doesNotContain("serviceName")
        .doesNotContain("serviceDescription")
        .doesNotContain("serviceLastStatusMessage");
  }

  /**
   * Die Kachel steht da, auch wenn die Pruefung abgeschaltet ist — <b>mit benanntem Grund</b>.
   *
   * <p>Im Profil {@code dev} ist sie aus ({@code overlord.ablagenpruefung.aktiv: false}), und damit
   * ist dieser Test der Nachweis fuer den Zustand, den der Betrieb im reinen Rollup-Prozess
   * ebenfalls sieht. <b>Er nennt den Grund und nicht die Zahl der Ziele</b>: Welche Ablage
   * eingetragen ist, ist eine Frage der Daten (Regel T2).
   */
  @Test
  @DisplayName("Lokal ist die Ablagenpruefung aus, und die Kachel sagt genau das")
  void ablagenkachel_ist_lokal_abgeschaltet() throws Exception {
    Antwort antwort = aufNexans.hole(pfad("48H"));

    assertThat(antwort.<String>json("$.plattform.ablagen.zustand")).isEqualTo("UNGEKLAERT");
    assertThat(antwort.<String>json("$.plattform.ablagen.grund"))
        .as("Ein ungeklaerter Zustand ohne Grund waere ein Achselzucken")
        .isEqualTo("ABGESCHALTET");
  }

  /** Der Block nimmt keinen Parameter entgegen — auch keinen, der wie ein Dienst aussieht. */
  @Test
  @DisplayName("Ein erfundener Dienstparameter bleibt wirkungslos")
  void kein_dienstparameter() throws Exception {
    Antwort ohne = aufNexans.hole(pfad("48H"));
    Antwort mit = aufNexans.hole(pfad("48H") + "&dienst=DIENST_ERFUNDEN&ablage=ABLAGE_ERFUNDEN");

    assertThat(mit.status()).isEqualTo(200);
    assertThat(mit.<List<String>>json("$.plattform.dienste[*].serviceId"))
        .isEqualTo(ohne.<List<String>>json("$.plattform.dienste[*].serviceId"));
  }

  // ─── Der Live-Rest (Teil B, 17.09.2026) ──────────────────────────────────────

  /** Der Block steht in der Antwort — mit einem der drei Zustaende, wie im Prozessbaum. */
  @Test
  @DisplayName("Der Block liveRest steht in der Antwort, mit einem der drei Zustaende")
  void der_block_live_rest_steht_in_der_antwort() throws Exception {
    Antwort antwort = aufNexans.hole(pfad("48H"));

    assertThat(antwort.hatFeld("$.liveRest.zustand")).isTrue();
    assertThat(LiveRestZustand.valueOf(antwort.<String>json("$.liveRest.zustand")))
        .isIn((Object[]) LiveRestZustand.values());
    assertThat(antwort.rumpf()).contains("\"vollstaendigBis\"");
  }

  /**
   * <b>Die vierte Mandantenkette dieser Seite, am Repository</b> (E-191, Regel M4). Der Dienst
   * reicht der Nachlesung nur Kennungen aus einer mandantengefilterten Lesung — ein Leck zeigte
   * sich durch den Endpunkt deshalb nie. Hier bekommt sie eine <b>fremde</b> Kennung direkt.
   *
   * <p><b>Der Test legt sich seine Katalogzeile selbst an</b> (Regel T2, {@code
   * docs/testfestigkeit.md}): auf einem Prozess von {@code SUTTONS} <b>ohne</b> Zeile, mit reinem
   * {@code INSERT} und dem Testpraefix in {@code geaendert_von}; {@code @AfterEach} loescht genau
   * diese Zeile. Gibt es keinen freien Prozess mehr, faellt der Test mit dem Satz, warum — dann ist
   * es der Bestand und nicht der Code.
   *
   * <p><b>Die Eichung zuerst:</b> Fuer den eigenen Mandanten liefert die Nachlesung die Zeile. Ohne
   * sie bewiese die leere Antwort fuer {@code VOTG} nur, dass leer leer ist.
   */
  @Test
  @DisplayName("Die Katalog-Nachlesung liefert fuer eine fremde Kennung nichts — am Repository")
  void nachlesung_liefert_keine_fremde_zeile() {
    Optional<String> frei =
        glassfishDsl
            .select(PROCESS.PROCESSID)
            .from(PROCESS)
            .join(PROJECTMANDANT)
            .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
            .where(PROJECTMANDANT.MANDANTID.eq(MANDANT_B))
            .andNotExists(
                DSL.selectOne()
                    .from(PROCESS_CATALOG)
                    .where(PROCESS_CATALOG.PROCESS_ID.eq(PROCESS.PROCESSID)))
            .orderBy(PROCESS.PROCESSID)
            .limit(1)
            .fetchOptional(PROCESS.PROCESSID);
    assertThat(frei)
        .as(
            "%s hat keinen Prozess ohne Katalogzeile mehr — der Test kann sich keine anlegen"
                + " (docs/testfestigkeit.md, Punkt T-3); das ist der Bestand, nicht der Code",
            MANDANT_B)
        .isPresent();
    String prozess = frei.get();
    monitorDsl
        .insertInto(PROCESS_CATALOG)
        .set(PROCESS_CATALOG.PROCESS_ID, prozess)
        .set(PROCESS_CATALOG.PARTNER, "ERFUNDENERPARTNER")
        .set(PROCESS_CATALOG.RICHTUNG, "EINGEHEND")
        .set(PROCESS_CATALOG.PFLEGESTATUS, Pflegestatus.GEPFLEGT.name())
        .set(PROCESS_CATALOG.VORSCHLAG_HERKUNFT, "KEINE")
        .set(PROCESS_CATALOG.GEAENDERT_AM, LocalDateTime.parse("2026-09-17T12:00:00"))
        .set(PROCESS_CATALOG.GEAENDERT_VON, KATALOG_VON)
        .execute();

    List<Katalogzuordnungszeile> eigene =
        dashboardRepository.katalogzuordnung(new MandantContext(MANDANT_B), List.of(prozess));
    assertThat(eigene)
        .as("Eichung: fuer den eigenen Mandanten liefert die Nachlesung die angelegte Zeile")
        .extracting(
            Katalogzuordnungszeile::processId,
            Katalogzuordnungszeile::partner,
            Katalogzuordnungszeile::richtung)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(prozess, "ERFUNDENERPARTNER", "EINGEHEND"));

    List<Katalogzuordnungszeile> fremde =
        dashboardRepository.katalogzuordnung(new MandantContext(MANDANT_A), List.of(prozess));
    assertThat(fremde)
        .as(
            "Die Nachlesung fuer %s darf die Zeile des Prozesses %s von %s nicht liefern — ohne"
                + " Mandantenkette im Statement taete sie es",
            MANDANT_A, prozess, MANDANT_B)
        .isEmpty();
  }

  // ─── Fehler live (18.09.2026, E-208) ─────────────────────────────────────────

  /** Der Block steht in der Antwort — mit einem der zwei Zustaende, kein dritter, kein Fehlen. */
  @Test
  @DisplayName("Der Block fehlerLive steht in der Antwort, mit einem der zwei Zustaende")
  void der_block_fehler_live_steht_in_der_antwort() throws Exception {
    Antwort antwort = aufNexans.hole(pfad("48H"));

    assertThat(antwort.hatFeld("$.fehlerLive.zustand")).isTrue();
    assertThat(FehlerLiveZustand.valueOf(antwort.<String>json("$.fehlerLive.zustand")))
        .isIn((Object[]) FehlerLiveZustand.values());
  }

  /**
   * <b>Die fuenfte Mandantenkette dieser Seite, am Repository</b> (Regel M4). Durch den Endpunkt
   * zeigte sich ein Leck der Fehlerlesung nur als Zahl — und eine Zahl, die fremde Fehler
   * mitzaehlt, sieht aus wie eine richtige. Hier bekommt die Lesung das Fenster {@code 12M} am
   * Anker direkt.
   *
   * <p><b>Beide Richtungen, wie bei Block 6:</b> Keine Prozesskennung von {@code SUTTONS} steht in
   * der Lesung fuer {@code VOTG}, und jede Kennung der Lesung steht in der Prozessliste von {@code
   * VOTG} — die zweite faellt auch dann, wenn das Leck von einem dritten Mandanten kommt.
   *
   * <p><b>Die Eichung zuerst:</b> {@code VOTG} hat Fehler in diesem Fenster; ohne eigene Zeilen
   * bewiese die leere Schnittmenge nur, dass leer leer ist.
   */
  @Test
  @DisplayName("Die Fehlerlesung liefert fuer VOTG keine fremde Zeile — am Repository")
  void die_fehlerlesung_liefert_keine_fremde_zeile() throws Exception {
    List<String> fremdeProzesse = prozesseVon(aufSuttons);
    List<String> eigeneProzesse = prozesseVon(aufVotg);
    LocalDateTime von = LocalDateTime.parse("2025-01-01T00:00");
    LocalDateTime bis = LocalDateTime.parse("2026-01-01T00:00");

    List<String> gelesen =
        fehlerLiveRepository.ausDerQuelle(new MandantContext(MANDANT_A), von, bis).stream()
            .map(FehlerLiveZeile::processId)
            .toList();

    assertThat(gelesen)
        .as("Eichung: %s hat Fehler im Fenster — ohne sie bewiese die Probe nichts", MANDANT_A)
        .isNotEmpty();
    assertThat(gelesen)
        .as(
            "Die Fehlerlesung fuer %s darf keinen Prozess von %s liefern — ohne Mandantenkette im"
                + " Statement taete sie es",
            MANDANT_A, MANDANT_B)
        .doesNotContainAnyElementsOf(fremdeProzesse);
    assertThat(eigeneProzesse)
        .as("Jede Prozesskennung der Fehlerlesung gehoert %s", MANDANT_A)
        .containsAll(gelesen);
  }
}
