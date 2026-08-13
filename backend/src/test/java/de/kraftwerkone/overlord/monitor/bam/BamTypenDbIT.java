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
 * {@code GET /api/bam/typen} am laufenden Endpunkt — <b>die Auswahl neben dem Suchfeld</b>.
 *
 * <p>Zwei Fälle tragen diese Datei, und beide sind Eigenschaften der <i>Daten</i> und nicht der
 * Abzählung:
 *
 * <ol>
 *   <li><b>Ein Mandant ohne konfigurierten Typ</b> bekommt {@code 200} mit leerer Liste und keinen
 *       Fehler. {@code WOC} ist dieser Fall (M40) — und ausgerechnet er trägt 2.067 BAM-Zeilen
 *       unter einem Typ, den seine Konfiguration nicht kennt. Die Auswahl ist leer, die Suche bei
 *       ihm trotzdem nicht sinnlos: Sie läuft dort typlos.
 *   <li><b>Die Ordnung ist auch bei gleichem Sortierindex eindeutig.</b> Bei {@code VOTG} tragen
 *       zwei Typen denselben Index; ohne die Typnummer als zweiten Schlüssel entschiede dort die
 *       Reihenfolge der Speicherung.
 * </ol>
 *
 * <p><b>Kein Prüfwert steht in dieser Datei</b> (Regel G1). Die Sollwerte werden zur Laufzeit aus
 * der Konfiguration hergeleitet; ändert sich die Testkopie, wird der Test rot und sagt es.
 */
class BamTypenDbIT extends SicherheitsTestbasis {

  /** Der Mandant <b>ohne</b> konfigurierten Typ (M40). */
  private static final String OHNE_TYPEN = "WOC";

  /** Der Mandant mit dem doppelt vergebenen Sortierindex. */
  private static final String MIT_DOPPELTEM_INDEX = "VOTG";

  private static final String PASSWORT = "einLangesPasswort1";
  private static final String TYPEN = "/api/bam/typen";

  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @BeforeEach
  void mandantenPruefen() {
    // Schlaegt das fehl, hat sich die Testkopie geaendert — nicht der Code.
    assertThat(mandantRepository.existiert(OHNE_TYPEN)).isTrue();
    assertThat(mandantRepository.existiert(MIT_DOPPELTEM_INDEX)).isTrue();
  }

  private Sitzung sitzungFuer(String mandant, String kennung)
      throws IOException, InterruptedException {
    String nutzer = PRAEFIX + kennung;
    legeNutzerAn(nutzer, PASSWORT, Rolle.MANDANT, mandant);
    return anmelden(nutzer, PASSWORT);
  }

  /** Wie viele Typen für diesen Mandanten konfiguriert sind — unmittelbar aus dem Quellschema. */
  private int konfigurierteAnzahl(String mandant) {
    return glassfishDsl
        .resultQuery("select count(*) from MessageBAMMandant where MandantID = ?", mandant)
        .fetchOne(satz -> ((Number) satz.get(0)).intValue());
  }

  /**
   * <b>Kein Platzhalter und kein Fehler.</b> Drei Mandanten haben keinen konfigurierten Typ ({@code
   * EDITIONLINGERI}, {@code SYSTEM}, {@code WOC}); für sie ist die Auswahl leer, und die Oberfläche
   * bietet dann gar keine an — dieselbe Regel wie bei der leeren Spalte in {@code
   * docs/nachrichtenliste.md} §8.1.
   */
  @Test
  @DisplayName("Ein Mandant ohne konfigurierten Typ bekommt 200 mit leerer Liste")
  void mandant_ohne_typen_bekommt_eine_leere_liste() throws Exception {
    assertThat(konfigurierteAnzahl(OHNE_TYPEN))
        .as("ohne diesen Mandanten haette der Test keinen Gegenstand")
        .isZero();

    Antwort antwort = sitzungFuer(OHNE_TYPEN, "bamtypen-woc").hole(TYPEN);

    assertThat(antwort.status()).as("kein 404 — der Aufrufer stellt eine Frage").isEqualTo(200);
    assertThat(antwort.rumpf().trim()).isEqualTo("[]");
  }

  /**
   * Die vollständige Zeile: Typnummer, Beschriftung und Sortierindex — und <b>die Zahl der Einträge
   * ist die der Konfiguration</b>, nicht die der belegten Typen.
   */
  @Test
  @DisplayName("Die Auswahl entspricht der Konfiguration, mit Beschriftung und Sortierindex")
  void die_auswahl_ist_vollstaendig() throws Exception {
    Antwort antwort = sitzungFuer(MIT_DOPPELTEM_INDEX, "bamtypen-votg").hole(TYPEN);

    assertThat(antwort.status()).isEqualTo(200);

    List<Number> typen = antwort.json("$[*].typ");
    List<String> bezeichnungen = antwort.json("$[*].bezeichnung");
    List<Number> indizes = antwort.json("$[*].sortIndex");

    assertThat(typen).hasSize(konfigurierteAnzahl(MIT_DOPPELTEM_INDEX));
    assertThat(bezeichnungen)
        .as("die Beschriftung ist nie null und nie leer — notfalls die Typnummer")
        .hasSameSizeAs(typen)
        .allSatisfy(text -> assertThat(text).isNotBlank());
    assertThat(indizes).hasSameSizeAs(typen);
  }

  /**
   * <b>Die Ordnung ist eindeutig, auch wo der Sortierindex es nicht ist.</b> Bei {@code VOTG}
   * tragen zwei Typen denselben Index; die Antwort muss sie trotzdem in einer festen Reihenfolge
   * zeigen — sonst zeigten zwei Aufrufe dieselbe Auswahl verschieden.
   */
  @Test
  @DisplayName("Bei gleichem Sortierindex entscheidet die Typnummer")
  void die_ordnung_ist_auch_bei_gleichem_index_eindeutig() throws Exception {
    List<int[]> erwartet =
        glassfishDsl
            .resultQuery(
                """
                select MessageBAMTypeSortIndex, MessageBAMType
                from MessageBAMMandant
                where MandantID = ?
                order by MessageBAMTypeSortIndex, MessageBAMType
                """,
                MIT_DOPPELTEM_INDEX)
            .fetch(
                satz ->
                    new int[] {
                      ((Number) satz.get(0)).intValue(), ((Number) satz.get(1)).intValue()
                    });

    long doppelte =
        erwartet.size() - erwartet.stream().mapToInt(paar -> paar[0]).distinct().count();
    assertThat(doppelte)
        .as(
            "Ohne einen doppelt vergebenen Sortierindex prueft dieser Test nichts. Ist er"
                + " verschwunden, gehoert ein anderer Mandant hierher — oder der zweite"
                + " Sortierschluessel ist entbehrlich geworden")
        .isPositive();

    Antwort antwort = sitzungFuer(MIT_DOPPELTEM_INDEX, "bamtypen-votg-ordnung").hole(TYPEN);

    List<Number> typen = antwort.json("$[*].typ");
    assertThat(typen.stream().map(Number::intValue).toList())
        .isEqualTo(erwartet.stream().map(paar -> paar[1]).toList());
  }
}
