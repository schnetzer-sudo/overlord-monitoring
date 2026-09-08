package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <b>Der Pflicht-Isolationstest von {@code GET /api/bam/suchfelder}</b> (Regel M4). Ohne ihn wird
 * nicht gemergt.
 *
 * <p>Paarung <b>{@code NEXANS} gegen {@code SUTTONS}</b>, aus demselben Grund wie in {@code
 * BamTypenIsolationDbIT}: Die BAM-Konfiguration von {@code SUTTONS} ist eine echte Teilmenge der
 * von {@code VOTG} (M48), ein Leck von {@code SUTTONS} nach {@code VOTG} bliebe unsichtbar; gegen
 * {@code NEXANS} sind die Mengen disjunkt.
 *
 * <h2>Was die Feld-Gruppe zusaetzlich verlangt — und was die Daten dazu hergeben</h2>
 *
 * <p>Die Feld-Gruppe hat <b>globale</b> Eintraege ({@code MandantID IS NULL}) und
 * <b>mandantengebundene</b>. Zu pruefen ist beides: Die globalen bleiben bei jedem Mandanten
 * erhalten, die fremden gebundenen erscheinen nicht. <b>Nach dem Stand der Testkopie vom 08.09.2026
 * sind alle sechs gebundenen Eintraege {@code NEXANS} zugeordnet (M161); {@code SUTTONS} hat
 * keinen.</b> Ein Leck von {@code SUTTONS} nach {@code NEXANS} ist damit heute nicht pruefbar —
 * nicht, weil der Test es nicht versuchte, sondern weil es nichts gibt, das lecken koennte. Der
 * Test leitet die Mengen zur Laufzeit her und prueft beide Richtungen; die zweite wird in dem
 * Moment scharf, in dem ein zweiter Mandant gebundene Eintraege bekommt. Was er verlangt, ist nur,
 * dass <i>mindestens einer</i> welche hat — sonst bewiese er, dass leer leer ist.
 *
 * <p><b>Die Gegenprobe ist die aus {@code BamTypenIsolationDbIT}:</b> kein Parameter, also kein
 * „fremd gegen erfunden", sondern der Mandantenwechsel in derselben Sitzung — und die Antwort muss
 * sich vollstaendig aendern.
 *
 * <p><b>Kein Pruefwert steht in dieser Datei</b> (Regel T2). Sollwerte kommen aus den beiden
 * Konfigurationstabellen und aus {@link Typ0Feld}.
 */
class SuchfelderIsolationDbIT extends SicherheitsTestbasis {

  private static final String NEXANS = "NEXANS";

  private static final String NUTZER_A = PRAEFIX + "suchfelder-nexans";
  private static final String NUTZER_B = PRAEFIX + "suchfelder-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  private static final String SUCHFELDER = "/api/bam/suchfelder";

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung aufNexans;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  // ─── Herleitung — aus dem Quellschema, nie aus dem Pruefling ──────────────────

  private record Konfiguriert(String name, Integer typ, String mandantId) {}

  private List<Konfiguriert> konfiguration() {
    return glassfishDsl
        .resultQuery(
            """
            select MessagePropertyName, MessagePropertyType, MandantID
            from MessagePropertySearchListEntry
            order by MessagePropertyName
            """)
        .fetch(
            (Record satz) ->
                new Konfiguriert(
                    satz.get(0, String.class),
                    satz.get(1, Integer.class),
                    satz.get(2, String.class)));
  }

  /** Die Namen, die dieser Mandant sehen darf: global oder ihm gebunden, und anbietbar. */
  private List<String> erwartetFuer(String mandant) {
    List<String> erwartet = new ArrayList<>();
    for (Konfiguriert zeile : konfiguration()) {
      boolean sichtbar = zeile.mandantId() == null || zeile.mandantId().equals(mandant);
      boolean anbietbar =
          Integer.valueOf(1).equals(zeile.typ())
              || (Integer.valueOf(0).equals(zeile.typ())
                  && Typ0Feld.fuer(zeile.name()).isPresent());
      if (sichtbar && anbietbar) {
        erwartet.add(zeile.name());
      }
    }
    return erwartet;
  }

  /** Die Namen, die einem <b>anderen</b> Mandanten gebunden sind — sie duerfen nie erscheinen. */
  private List<String> fremdGebundenFuer(String mandant) {
    return konfiguration().stream()
        .filter(zeile -> zeile.mandantId() != null && !zeile.mandantId().equals(mandant))
        .map(Konfiguriert::name)
        .toList();
  }

  private List<String> gebundenAn(String mandant) {
    return konfiguration().stream()
        .filter(zeile -> mandant.equals(zeile.mandantId()))
        .map(Konfiguriert::name)
        .toList();
  }

  private List<String> global() {
    return konfiguration().stream()
        .filter(zeile -> zeile.mandantId() == null)
        .map(Konfiguriert::name)
        .toList();
  }

  private List<Integer> bamKonfiguriertFuer(String mandant) {
    return glassfishDsl
        .resultQuery(
            """
            select MessageBAMType
            from MessageBAMMandant
            where MandantID = ?
            order by MessageBAMTypeSortIndex, MessageBAMType
            """,
            mandant)
        .fetch(satz -> ((Number) satz.get(0)).intValue());
  }

  private static List<String> feldnamen(Antwort antwort) {
    return antwort.json("$.felder[*].name");
  }

  private static List<Integer> bamTypen(Antwort antwort) {
    List<Number> roh = antwort.json("$.bam[*].typ");
    return roh.stream().map(Number::intValue).toList();
  }

  // ─── Die Voraussetzung ────────────────────────────────────────────────────────

  /**
   * <b>Ohne diese Zusicherung waere alles Folgende wertlos.</b> Es gibt globale Eintraege, und
   * mindestens einer der beiden Mandanten hat gebundene — sonst bewiese „A sieht nichts von B" nur,
   * dass es nichts zu sehen gibt.
   */
  @Test
  @DisplayName("Es gibt globale Eintraege, und mindestens ein Mandant hat gebundene")
  void es_gibt_globale_und_gebundene_eintraege() {
    assertThat(global()).as("ohne globale Eintraege prueft die Erhaltung nichts").isNotEmpty();
    assertThat(gebundenAn(NEXANS).size() + gebundenAn(MANDANT_B).size())
        .as("ohne einen einzigen gebundenen Eintrag bewiese der Test nur, dass leer leer ist")
        .isPositive();
    assertThat(gebundenAn(NEXANS)).doesNotContainAnyElementsOf(gebundenAn(MANDANT_B));
    assertThat(bamKonfiguriertFuer(NEXANS))
        .isNotEmpty()
        .doesNotContainAnyElementsOf(bamKonfiguriertFuer(MANDANT_B));
  }

  // ─── Der Kern ─────────────────────────────────────────────────────────────────

  /** Jeder Mandant sieht <b>genau</b> seine Auswahl, in beiden Gruppen. */
  @Test
  @DisplayName("Jeder Mandant sieht genau seine Auswahl — beide Gruppen")
  void jeder_sieht_genau_seine_auswahl() throws Exception {
    Antwort nexans = aufNexans.hole(SUCHFELDER);
    Antwort suttons = aufSuttons.hole(SUCHFELDER);

    assertThat(nexans.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(feldnamen(nexans)).isEqualTo(erwartetFuer(NEXANS));
    assertThat(feldnamen(suttons)).isEqualTo(erwartetFuer(MANDANT_B));
    assertThat(bamTypen(nexans)).isEqualTo(bamKonfiguriertFuer(NEXANS));
    assertThat(bamTypen(suttons)).isEqualTo(bamKonfiguriertFuer(MANDANT_B));
  }

  /**
   * <b>Die globalen Eintraege bleiben bei jedem Mandanten erhalten.</b> Ein {@code WHERE MandantID
   * = ?} ohne den {@code NULL}-Zweig loeschte sie — acht von vierzehn Eintraegen (M161).
   */
  @Test
  @DisplayName("Die globalen Eintraege stehen bei beiden Mandanten")
  void globale_eintraege_bleiben_erhalten() throws Exception {
    List<String> globalAnbietbar =
        erwartetFuer(NEXANS).stream().filter(global()::contains).toList();
    assertThat(globalAnbietbar).isNotEmpty();

    assertThat(feldnamen(aufNexans.hole(SUCHFELDER))).containsAll(globalAnbietbar);
    assertThat(feldnamen(aufSuttons.hole(SUCHFELDER))).containsAll(globalAnbietbar);
  }

  /**
   * <b>Kein Mandant sieht einen Eintrag, der einem anderen gebunden ist — in beide Richtungen.</b>
   * Die Richtung {@code SUTTONS} → {@code NEXANS} ist nach dem Stand vom 08.09.2026 leer und wird
   * scharf, sobald {@code SUTTONS} gebundene Eintraege bekommt.
   */
  @Test
  @DisplayName("Fremde gebundene Eintraege erscheinen nicht — in beide Richtungen")
  void fremde_gebundene_erscheinen_nicht() throws Exception {
    Antwort nexans = aufNexans.hole(SUCHFELDER);
    Antwort suttons = aufSuttons.hole(SUCHFELDER);

    assertThat(feldnamen(nexans)).doesNotContainAnyElementsOf(fremdGebundenFuer(NEXANS));
    assertThat(feldnamen(suttons)).doesNotContainAnyElementsOf(fremdGebundenFuer(MANDANT_B));
    assertThat(feldnamen(suttons))
        .as("die gebundenen Eintraege von NEXANS gehoeren nicht zu SUTTONS")
        .doesNotContainAnyElementsOf(gebundenAn(NEXANS));
    assertThat(feldnamen(nexans)).doesNotContainAnyElementsOf(gebundenAn(MANDANT_B));
    assertThat(bamTypen(nexans)).doesNotContainAnyElementsOf(bamKonfiguriertFuer(MANDANT_B));
    assertThat(bamTypen(suttons)).doesNotContainAnyElementsOf(bamKonfiguriertFuer(NEXANS));
  }

  /**
   * <b>Die Gegenprobe: Die Antwort haengt an der Sitzung und an nichts sonst.</b> Dieselbe Sitzung,
   * derselbe Aufruf, dazwischen ein Mandantenwechsel — und beide Gruppen aendern sich.
   */
  @Test
  @DisplayName("Der Mandantenwechsel wechselt das Angebot vollstaendig")
  void gegenprobe_ueber_den_mandantenwechsel() throws Exception {
    String admin = PRAEFIX + "suchfelder-admin";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    assertThat(alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}").status())
        .isEqualTo(200);
    Antwort beiNexans = alsAdmin.hole(SUCHFELDER);

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_B + "\"}").status())
        .isEqualTo(200);
    Antwort beiSuttons = alsAdmin.hole(SUCHFELDER);

    assertThat(feldnamen(beiNexans)).isEqualTo(erwartetFuer(NEXANS));
    assertThat(feldnamen(beiSuttons)).isEqualTo(erwartetFuer(MANDANT_B));
    assertThat(bamTypen(beiNexans)).isEqualTo(bamKonfiguriertFuer(NEXANS));
    assertThat(bamTypen(beiSuttons)).isEqualTo(bamKonfiguriertFuer(MANDANT_B));
    assertThat(feldnamen(beiSuttons))
        .as("ein Angebot, das den Wechsel ueberlebt, waere das Angebot des falschen Mandanten")
        .doesNotContainAnyElementsOf(gebundenAn(NEXANS));
  }

  /** Der Rumpf nennt keinen Mandanten — er beschreibt Felder und nicht, fuer wen sie gelten. */
  @Test
  @DisplayName("Der Rumpf traegt keine Mandantenkennung")
  void der_rumpf_nennt_keinen_mandanten() throws Exception {
    Antwort antwort = aufNexans.hole(SUCHFELDER);

    assertThat(antwort.rumpf())
        .doesNotContain(MANDANT_B)
        .doesNotContain(NEXANS)
        .doesNotContain("MandantID")
        .doesNotContain("mandant");
  }

  /** Ohne aktiven Mandanten gibt es kein Angebot — auch nicht fuer ADMIN (Regeln M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antwortet der Endpunkt 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "suchfelder-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort antwort = alsAdmin.hole(SUCHFELDER);

    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
  }
}
