package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.security.Rolle;
import de.kraftwerkone.overlord.monitor.security.SicherheitsTestbasis;
import java.io.IOException;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <b>Der Pflicht-Isolationstest von {@code GET /api/bam/typen}</b> (Regel M4). Ohne ihn wird nicht
 * gemergt.
 *
 * <h2>Warum {@code NEXANS} gegen {@code SUTTONS} und nicht das Paar der Vorlage</h2>
 *
 * <p>{@code SicherheitsTestbasis} führt {@code VOTG} gegen {@code SUTTONS}, und für die
 * Pfad-Endpunkte ist das die richtige Paarung. <b>Hier trüge sie nur die halbe Aussage:</b> Die
 * Konfiguration von {@code SUTTONS} ({@code 2000}, {@code 2001}) ist eine <i>echte Teilmenge</i>
 * der von {@code VOTG} ({@code 2000} bis {@code 2011}) — gemessen am 13.08.2026. Ein Leck von
 * {@code SUTTONS} nach {@code VOTG} wäre damit unsichtbar, weil dort ohnehin dieselben Typen
 * stehen.
 *
 * <p><b>{@code NEXANS} gegen {@code SUTTONS} ist dagegen disjunkt</b> — 9xxx gegen 2xxx —, und die
 * Trennung ist damit in <b>beide</b> Richtungen prüfbar. Es ist zugleich die Paarung von {@code
 * NachrichtenIsolationDbIT} und {@code BamSucheIsolationDbIT}: zwei Mandanten aus verschiedenen
 * Häusern.
 *
 * <p><b>Die Disjunktheit wird zur Laufzeit hergeleitet und nicht angenommen.</b> Ändert sich die
 * Konfiguration der Testkopie, wird der erste Test rot und sagt es — statt dass die übrigen lautlos
 * nichts mehr beweisen.
 *
 * <h2>Die Gegenprobe sieht hier wieder anders aus</h2>
 *
 * <p>Es gibt <b>kein {@code 404}</b>: Der Endpunkt nimmt keine Kennung entgegen, er hat überhaupt
 * keinen Parameter. Die Ununterscheidbarkeit von „fremd" und „erfunden" ist damit gegenstandslos —
 * <b>an ihre Stelle tritt die Frage, woher die Antwort ihren Mandanten nimmt</b>. Geprüft wird
 * deshalb der Mandantenwechsel: Dieselbe Sitzung, derselbe Aufruf, ein anderer aktiver Mandant —
 * und die Antwort muss sich vollständig ändern.
 */
class BamTypenIsolationDbIT extends SicherheitsTestbasis {

  /** Mandant A — 40 konfigurierte Typen, alle im 9xxx-Bereich. */
  private static final String NEXANS = "NEXANS";

  private static final String NUTZER_A = PRAEFIX + "bamtypen-nexans";
  private static final String NUTZER_B = PRAEFIX + "bamtypen-suttons";
  private static final String PASSWORT = "einLangesPasswort1";

  private static final String TYPEN = "/api/bam/typen";

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  private Sitzung aufNexans;
  private Sitzung aufSuttons;

  @BeforeEach
  void nutzerAnlegenUndAnmelden() throws IOException, InterruptedException {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(NEXANS)).isTrue();
    assertThat(mandantRepository.existiert(MANDANT_B)).isTrue();

    legeNutzerAn(NUTZER_A, PASSWORT, Rolle.MANDANT, NEXANS);
    legeNutzerAn(NUTZER_B, PASSWORT, Rolle.MANDANT, MANDANT_B);
    aufNexans = anmelden(NUTZER_A, PASSWORT);
    aufSuttons = anmelden(NUTZER_B, PASSWORT);
  }

  // ─── Herleitung ───────────────────────────────────────────────────────────────

  /**
   * Die konfigurierten Typnummern eines Mandanten, unmittelbar aus dem Quellschema — <b>der
   * Sollwert, gegen den die Antwort des Endpunkts gehalten wird</b>.
   *
   * <p>Bewusst nicht über den Endpunkt selbst geholt: Ein Test, der seine Erwartung aus dem
   * Prüfling zieht, bestätigt nur, dass der Prüfling sich selbst gleicht.
   */
  private List<Integer> konfiguriertFuer(String mandant) {
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

  private static List<Integer> typenAus(Antwort antwort) {
    List<Number> roh = antwort.json("$[*].typ");
    return roh.stream().map(Number::intValue).toList();
  }

  // ─── Die Tests ────────────────────────────────────────────────────────────────

  /**
   * <b>Die Voraussetzung, ohne die alles Folgende wertlos wäre.</b> Beide Mandanten haben eine
   * Konfiguration, und die beiden Mengen sind <b>disjunkt</b> — nur dann sagt „A sieht keinen Typ
   * von B" überhaupt etwas.
   */
  @Test
  @DisplayName("Beide Mandanten sind konfiguriert, und ihre Typmengen sind disjunkt")
  void beide_mandanten_haben_eine_disjunkte_konfiguration() {
    List<Integer> vonNexans = konfiguriertFuer(NEXANS);
    List<Integer> vonSuttons = konfiguriertFuer(MANDANT_B);

    assertThat(vonNexans)
        .as("ohne Konfiguration bewiese der Test nur, dass leer leer ist")
        .isNotEmpty();
    assertThat(vonSuttons).isNotEmpty();
    assertThat(vonNexans)
        .as(
            "Die Paarung ist gerade deshalb gewaehlt: Waere die eine Menge Teilmenge der anderen,"
                + " bliebe ein Leck in einer Richtung unsichtbar")
        .doesNotContainAnyElementsOf(vonSuttons);
  }

  /**
   * Jeder Mandant bekommt <b>genau</b> seine eigene Konfiguration — nicht mehr und nicht weniger.
   */
  @Test
  @DisplayName("Jeder Mandant sieht genau seine eigene Auswahl")
  void jeder_sieht_genau_seine_eigene_auswahl() throws Exception {
    Antwort nexans = aufNexans.hole(TYPEN);
    Antwort suttons = aufSuttons.hole(TYPEN);

    assertThat(nexans.status()).isEqualTo(200);
    assertThat(suttons.status()).isEqualTo(200);
    assertThat(typenAus(nexans)).isEqualTo(konfiguriertFuer(NEXANS));
    assertThat(typenAus(suttons)).isEqualTo(konfiguriertFuer(MANDANT_B));
  }

  /**
   * <b>Der eigentliche Nachweis, und er gilt in beide Richtungen.</b> Kein Typ des einen Mandanten
   * taucht beim anderen auf — und weil die Mengen disjunkt sind, ist das eine Aussage und keine
   * Tautologie.
   */
  @Test
  @DisplayName("Kein Mandant sieht einen Typ des anderen — in beide Richtungen")
  void trennung_gilt_in_beide_richtungen() throws Exception {
    List<Integer> vonNexans = konfiguriertFuer(NEXANS);
    List<Integer> vonSuttons = konfiguriertFuer(MANDANT_B);

    assertThat(typenAus(aufNexans.hole(TYPEN)))
        .as("SUTTONS-Typen haben in der Auswahl von NEXANS nichts zu suchen")
        .doesNotContainAnyElementsOf(vonSuttons);
    assertThat(typenAus(aufSuttons.hole(TYPEN)))
        .as("sonst bewiese der Test nur, dass NEXANS nichts Fremdes sieht")
        .doesNotContainAnyElementsOf(vonNexans);
  }

  /**
   * <b>Die Gegenprobe: Die Antwort hängt an der Sitzung und an nichts sonst.</b>
   *
   * <p>Dieselbe Sitzung, derselbe Aufruf, dazwischen ein Mandantenwechsel — und die Antwort ist
   * eine vollständig andere. Das schließt die Fehlerklasse aus, gegen die ein Vergleich zweier
   * <i>getrennter</i> Sitzungen blind wäre: eine Auswahl, die einmal ermittelt und danach für alle
   * gehalten wird.
   */
  @Test
  @DisplayName("Der Mandantenwechsel wechselt die Auswahl vollstaendig")
  void gegenprobe_ueber_den_mandantenwechsel() throws Exception {
    String admin = PRAEFIX + "bamtypen-admin";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    assertThat(alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + NEXANS + "\"}").status())
        .isEqualTo(200);
    List<Integer> beiNexans = typenAus(alsAdmin.hole(TYPEN));

    assertThat(
            alsAdmin.sende("/api/auth/mandant", "{\"mandantId\":\"" + MANDANT_B + "\"}").status())
        .isEqualTo(200);
    List<Integer> beiSuttons = typenAus(alsAdmin.hole(TYPEN));

    assertThat(beiNexans).isEqualTo(konfiguriertFuer(NEXANS));
    assertThat(beiSuttons).isEqualTo(konfiguriertFuer(MANDANT_B));
    assertThat(beiNexans)
        .as("eine Auswahl, die den Wechsel ueberlebt, waere die Auswahl des falschen Mandanten")
        .doesNotContainAnyElementsOf(beiSuttons);
  }

  /**
   * Der Rumpf nennt den Mandanten nirgends — weder den eigenen noch einen fremden. Er beschreibt
   * Belegarten und nicht, für wen sie gelten.
   */
  @Test
  @DisplayName("Der Rumpf traegt keine Mandantenkennung")
  void der_rumpf_nennt_keinen_mandanten() throws Exception {
    Antwort antwort = aufNexans.hole(TYPEN);

    assertThat(antwort.rumpf())
        .doesNotContain(MANDANT_B)
        .doesNotContain(NEXANS)
        .doesNotContain("MandantID")
        .doesNotContain("mandant");
  }

  /** Ohne aktiven Mandanten gibt es keine Auswahl — auch nicht für ADMIN (Regeln M1/M2). */
  @Test
  @DisplayName("Ohne aktiven Mandanten antwortet der Endpunkt 403")
  void ohne_aktiven_mandanten_ist_403() throws Exception {
    String admin = PRAEFIX + "bamtypen-admin-ohne";
    legeNutzerAn(admin, PASSWORT, Rolle.ADMIN);
    Sitzung alsAdmin = anmelden(admin, PASSWORT);

    Antwort antwort = alsAdmin.hole(TYPEN);

    assertThat(antwort.status()).isEqualTo(403);
    assertThat(antwort.<String>json("$.type")).endsWith("/kein-mandant-gewaehlt");
  }
}
