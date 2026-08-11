package de.kraftwerkone.overlord.monitor.common;

import static de.kraftwerkone.overlord.monitor.common.Kettenrolle.MERGE_EINGANG;
import static de.kraftwerkone.overlord.monitor.common.Kettenrolle.MERGE_ERGEBNIS;
import static de.kraftwerkone.overlord.monitor.common.Kettenrolle.SPLIT_KIND;
import static de.kraftwerkone.overlord.monitor.common.Kettenrolle.SPLIT_WURZEL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Die Rollenlogik <b>ohne Datenbank</b> — alle vier Einzelrollen, alle sechs Paare, die leere
 * Menge.
 *
 * <p>Der Gegenpart ist {@code KettenrollenBestandTest}: Er prueft dieselbe Regel gegen die
 * Testkopie und faengt Kombinationen ab, die es hier nicht gibt, weil sie niemand aufgeschrieben
 * hat. Beide zusammen ersetzen das vollstaendige {@code switch}, das es bei einer Menge nicht geben
 * kann.
 */
class KettenrollenTest {

  private static final String KENNUNG = "eine-kennung";

  // ─── Die vier Einzelrollen ──────────────────────────────────────────────────────

  @Test
  @DisplayName("Source gesetzt heisst SPLIT_WURZEL")
  void source_flag_ist_split_wurzel() {
    assertThat(Kettenrollen.aus(true, null, null, false)).containsExactly(SPLIT_WURZEL);
  }

  @Test
  @DisplayName("SourceMessageID belegt heisst SPLIT_KIND")
  void source_id_ist_split_kind() {
    assertThat(Kettenrollen.aus(false, KENNUNG, null, false)).containsExactly(SPLIT_KIND);
  }

  @Test
  @DisplayName("TargetMessageID belegt heisst MERGE_EINGANG")
  void target_id_ist_merge_eingang() {
    assertThat(Kettenrollen.aus(false, null, KENNUNG, false)).containsExactly(MERGE_EINGANG);
  }

  @Test
  @DisplayName("Target gesetzt heisst MERGE_ERGEBNIS")
  void target_flag_ist_merge_ergebnis() {
    assertThat(Kettenrollen.aus(false, null, null, true)).containsExactly(MERGE_ERGEBNIS);
  }

  // ─── Die leere Menge ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Ohne Verkettung ist die Menge leer — und niemals null")
  void ohne_verkettung_leere_menge() {
    assertThat(Kettenrollen.aus(false, null, null, false)).isEmpty();
  }

  @Test
  @DisplayName("Auch bei lauter null ist die Menge leer statt fehlend")
  void alles_null_ist_leere_menge() {
    assertThat(Kettenrollen.aus(null, null, null, null)).isEmpty();
  }

  // ─── Die sechs Paare ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("SPLIT_WURZEL + SPLIT_KIND — 456 Zeilen ueber Fenster B (M30-4)")
  void paar_wurzel_und_kind() {
    assertThat(Kettenrollen.aus(true, KENNUNG, null, false))
        .containsExactly(SPLIT_WURZEL, SPLIT_KIND);
  }

  @Test
  @DisplayName("SPLIT_WURZEL + MERGE_ERGEBNIS — 25 Zeilen ueber Fenster B (M30-4)")
  void paar_wurzel_und_ergebnis() {
    assertThat(Kettenrollen.aus(true, null, null, true))
        .containsExactly(SPLIT_WURZEL, MERGE_ERGEBNIS);
  }

  @Test
  @DisplayName("SPLIT_KIND + MERGE_ERGEBNIS — 33 Zeilen ueber Fenster B (M28-1c)")
  void paar_kind_und_ergebnis() {
    assertThat(Kettenrollen.aus(false, KENNUNG, null, true))
        .containsExactly(SPLIT_KIND, MERGE_ERGEBNIS);
  }

  /**
   * Das <b>sechste</b> Paar. Es kommt in der Testkopie nicht vor — null ueber beide Bezugsfenster
   * (M28‑1c fuer Fenster B, M30‑4 fuer Fenster A) —, und genau deshalb steht es hier: „kommt nicht
   * vor" ist keine Aussage darueber, was die Rechenregel liefern soll, wenn es doch vorkommt.
   */
  @Test
  @DisplayName("SPLIT_WURZEL + MERGE_EINGANG — gemessen null, trotzdem formuliert")
  void paar_wurzel_und_eingang() {
    assertThat(Kettenrollen.aus(true, null, KENNUNG, false))
        .containsExactly(SPLIT_WURZEL, MERGE_EINGANG);
  }

  @Test
  @DisplayName("SPLIT_KIND + MERGE_EINGANG — durch M28-1 ausgeschlossen, trotzdem formuliert")
  void paar_kind_und_eingang() {
    assertThat(Kettenrollen.aus(false, KENNUNG, KENNUNG, false))
        .containsExactly(SPLIT_KIND, MERGE_EINGANG);
  }

  @Test
  @DisplayName("MERGE_EINGANG + MERGE_ERGEBNIS")
  void paar_eingang_und_ergebnis() {
    assertThat(Kettenrollen.aus(false, null, KENNUNG, true))
        .containsExactly(MERGE_EINGANG, MERGE_ERGEBNIS);
  }

  // ─── „Belegt" heisst: nicht null und nicht leer ─────────────────────────────────

  /**
   * §5.9 hat gemessen, dass unbelegte Verkettung ausnahmslos {@code NULL} ist — kein einziger
   * leerer String ueber 220.579 gepruefte Zeilen. Die Pruefung bleibt trotzdem: Die Produktion muss
   * sich nicht daran halten, was die Testkopie zufaellig enthaelt.
   */
  @ParameterizedTest(name = "Kennung \"{0}\" zaehlt nicht als belegt")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "   ", "\t"})
  void leere_kennung_ist_keine_rolle(String kennung) {
    assertThat(Kettenrollen.aus(false, kennung, kennung, false)).isEmpty();
  }

  @Test
  @DisplayName("Ein null-Flag ist keine Behauptung, es gebe Kinder")
  void null_flag_ist_nicht_gesetzt() {
    assertThat(Kettenrollen.aus(null, null, null, null)).isEmpty();
    assertThat(Kettenrollen.aus(false, null, null, null)).isEmpty();
  }

  // ─── Reihenfolge und Vollstaendigkeit ───────────────────────────────────────────

  /**
   * Die Reihenfolge in der Antwort haengt nicht daran, in welcher Reihenfolge die Spalten geprueft
   * werden — {@link EnumSet} laeuft in Deklarationsreihenfolge.
   */
  @Test
  @DisplayName("Alle vier Rollen kommen in Deklarationsreihenfolge")
  void reihenfolge_ist_die_deklarationsreihenfolge() {
    assertThat(Kettenrollen.aus(true, KENNUNG, KENNUNG, true))
        .containsExactly(SPLIT_WURZEL, SPLIT_KIND, MERGE_EINGANG, MERGE_ERGEBNIS);
  }

  @Test
  @DisplayName("Das Ergebnis ist je Aufruf frisch und veraendert kein voriges")
  void ergebnis_ist_je_aufruf_frisch() {
    EnumSet<Kettenrolle> erste = Kettenrollen.aus(true, null, null, false);
    EnumSet<Kettenrolle> zweite = Kettenrollen.aus(false, KENNUNG, null, false);

    erste.add(MERGE_ERGEBNIS);

    assertThat(zweite).containsExactly(SPLIT_KIND);
  }

  /**
   * <b>Die hinterlegte Liste enthaelt sechs Paare, nicht fuenf.</b> M28 hat vier erhoben, M28‑1c
   * das fuenfte ({@code SPLIT_KIND} + {@code MERGE_ERGEBNIS}) — und die beiden mit {@code
   * MERGE_EINGANG} sind gemessen null. Sie gehoeren trotzdem hinein: Die Liste sagt, worueber sich
   * etwas sagen laesst, nicht, was vorgekommen ist.
   */
  @Test
  @DisplayName("Die hinterlegte Liste ist vollstaendig: leer, vier Einzelrollen, sechs Paare")
  void hinterlegte_liste_ist_vollstaendig() {
    assertThat(Kettenrollen.FORMULIERTE_KOMBINATIONEN).hasSize(11);
    assertThat(Kettenrollen.FORMULIERTE_KOMBINATIONEN)
        .as("die leere Menge und die vier Einzelrollen")
        .contains(
            Set.of(),
            Set.of(SPLIT_WURZEL),
            Set.of(SPLIT_KIND),
            Set.of(MERGE_EINGANG),
            Set.of(MERGE_ERGEBNIS));
    assertThat(Kettenrollen.FORMULIERTE_KOMBINATIONEN)
        .as("alle sechs Paare, auch die drei gemessen leeren (alle drei mit MERGE_EINGANG)")
        .contains(
            Set.of(SPLIT_WURZEL, SPLIT_KIND),
            Set.of(SPLIT_WURZEL, MERGE_EINGANG),
            Set.of(SPLIT_WURZEL, MERGE_ERGEBNIS),
            Set.of(SPLIT_KIND, MERGE_EINGANG),
            Set.of(SPLIT_KIND, MERGE_ERGEBNIS),
            Set.of(MERGE_EINGANG, MERGE_ERGEBNIS));
  }

  @Test
  @DisplayName("Keine Kombination mit drei oder vier Rollen steht in der Liste")
  void keine_kombination_mit_drei_oder_vier_rollen() {
    assertThat(Kettenrollen.FORMULIERTE_KOMBINATIONEN)
        .as("nie mehr als zwei Rollen je Zeile (M28-1c) — eine dritte haette keinen Beleg")
        .allSatisfy(
            kombination ->
                assertThat(kombination.size())
                    .isLessThanOrEqualTo(Kettenrollen.HOECHSTENS_ROLLEN_JE_ZEILE));
  }

  /**
   * Der Nachweis, dass die Liste und die Rechenregel dasselbe meinen: <b>Jede</b> der sechzehn
   * moeglichen Spaltenbelegungen mit hoechstens zwei Rollen ergibt eine Kombination, die in der
   * Liste steht.
   */
  @Test
  @DisplayName("Jede erreichbare Belegung mit bis zu zwei Rollen steht in der Liste")
  void jede_erreichbare_belegung_ist_formuliert() {
    List<Set<Kettenrolle>> erreicht = new ArrayList<>();
    for (boolean source : new boolean[] {false, true}) {
      for (boolean kind : new boolean[] {false, true}) {
        for (boolean eingang : new boolean[] {false, true}) {
          for (boolean target : new boolean[] {false, true}) {
            Set<Kettenrolle> rollen =
                Kettenrollen.aus(source, kind ? KENNUNG : null, eingang ? KENNUNG : null, target);
            if (rollen.size() <= Kettenrollen.HOECHSTENS_ROLLEN_JE_ZEILE) {
              erreicht.add(rollen);
            }
          }
        }
      }
    }
    assertThat(erreicht).isNotEmpty();
    assertThat(Kettenrollen.FORMULIERTE_KOMBINATIONEN).containsAll(erreicht);
  }
}
