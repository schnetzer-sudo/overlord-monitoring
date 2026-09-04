package de.kraftwerkone.overlord.monitor.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

  @Test
  @DisplayName("Auch die Richtungssicht traegt nichts Fremdes")
  void richtungssicht_traegt_nichts_fremdes() throws Exception {
    List<String> vonSuttons = prozesseVon(aufSuttons);

    ohneFremdeProzesse(aufVotg.hole("/api/dashboard?zeitraum=12M&verteilung=RICHTUNG"), vonSuttons);
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
    assertThat(antwort.<List<String>>json("$.verteilung.zeilen[*].art"))
        .as("Auch hier sagt der Katalog etwas: alles nicht zugeordnet, naemlich null")
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
        .contains("\"stand\"");
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

  @Test
  @DisplayName("Eine unbekannte Verteilungssicht ist 400")
  void unbekannte_verteilung() throws Exception {
    Antwort antwort = aufVotg.hole("/api/dashboard?verteilung=BELEGART");

    assertThat(antwort.status()).isEqualTo(400);
    assertThat(antwort.<String>json("$.type")).endsWith("/verteilung-unbekannt");
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
}
