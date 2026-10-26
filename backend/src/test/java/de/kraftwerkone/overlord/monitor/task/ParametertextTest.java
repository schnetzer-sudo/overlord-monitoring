package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.anpassung;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.eintrag;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.loeschung;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.neuanlage;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.DefinitionsPruefung;
import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * Der Parametertext, zeichengenau ({@code docs/tasks.md} §5.2). Die drei Texte stehen so in der
 * Taskdefinition des Auftraggebers vom 15.10.2026 (mit der Lesart „Anpassung“, E‑1017) und im
 * Plattformvertrag.
 */
class ParametertextTest {

  private static final TypDefinition WERK = Testdefinitionen.werk();
  private static final TypDefinition GATTUNG = Testdefinitionen.gattung();

  @Test
  @DisplayName("Neuanlage — exakt der Text der Quelle")
  void neuanlage_exakt() {
    assertThat(Parametertext.baue(WERK, "Neuanlage", neuanlage()))
        .isEqualTo(
            "Aktion=Neuanlage\n"
                + "72332/DDB0,120764,\n"
                + "72261/DDR0,120764,\n"
                + "010/18VZE,120055,100050\n"
                + "0101159903/22/001N0,123085,");
  }

  @Test
  @DisplayName("Loeschung — exakt der Text der Quelle")
  void loeschung_exakt() {
    assertThat(Parametertext.baue(WERK, "Löschung", loeschung()))
        .isEqualTo("Aktion=Löschung\n010/18VZE\n0101159903/22/001N0");
  }

  @Test
  @DisplayName("Anpassung — exakt der Text der Quelle, mit der Lesart „Anpassung“")
  void anpassung_exakt() {
    assertThat(Parametertext.baue(WERK, "Anpassung", anpassung()))
        .isEqualTo("Aktion=Anpassung\n010/18VZE,123456,123456\n0101159903/22/001N0,234567,");
  }

  @Test
  @DisplayName("LF als Zeilenende, kein abschliessendes LF, kein CR")
  void zeilenende() {
    String text = Parametertext.baue(WERK, "Löschung", loeschung());
    assertThat(text).doesNotEndWith("\n").doesNotContain("\r");
    assertThat(text.split("\n", -1)).hasSize(3);
  }

  @Test
  @DisplayName("Gattung: anderer Trenner, andere Vorlagen, optionaler erster Schluesselteil")
  void gattung() {
    assertThat(
            Parametertext.baue(
                GATTUNG,
                "Sperren",
                List.of(
                    eintrag("bereich", "NO", "konto", "17", "grund", "Mahnung offen"),
                    eintrag("konto", "42", "grund", "Prüfung"))))
        .isEqualTo("Aktion=Sperren\nNO-17;Mahnung offen\n42;Prüfung");
    assertThat(
            Parametertext.baue(
                GATTUNG, "Freigeben", List.of(eintrag("bereich", "NO", "konto", "17"))))
        .isEqualTo("Aktion=Freigeben\n17");
  }

  @Test
  @DisplayName("Die vier Schluesselformen der Plattform woertlich: L/W/A, W/A, L/W/, W/ (E‑1172)")
  void vier_schluesselformen() {
    List<Map<String, String>> eintraege =
        List.of(
            eintrag("lieferantennummer", "0101159903", "werk", "22", "abladestelle", "001N0"),
            eintrag("werk", "72332", "abladestelle", "DDB0"),
            eintrag("lieferantennummer", "0101159903", "werk", "40NP"),
            eintrag("werk", "72332"));
    assertThat(Parametertext.baue(WERK, "Löschung", eintraege))
        .isEqualTo("Aktion=Löschung\n0101159903/22/001N0\n72332/DDB0\n0101159903/40NP/\n72332/");
  }

  @Test
  @DisplayName("Ohne letzten Teil: er bleibt als leerer Text nach dem letzten Trenner")
  void ohne_letzten_teil() {
    assertThat(
            Parametertext.baue(
                WERK,
                "Neuanlage",
                List.of(
                    eintrag("lieferantennummer", "0101159903", "werk", "40NP", "sndprn", "120764"),
                    eintrag("werk", "72332", "sndprn", "120764", "sndprn4908", "100050"))))
        .isEqualTo("Aktion=Neuanlage\n0101159903/40NP/,120764,\n72332/,120764,100050");
  }

  @Test
  @DisplayName("Ohne mittleren Teil: leerer Text zwischen zwei Trennern (a//c)")
  void ohne_mittleren_teil() {
    assertThat(
            Parametertext.baue(
                mitte(),
                "Neuanlage",
                List.of(
                    eintrag(
                        "lieferantennummer",
                        "0101159903",
                        "abladestelle",
                        "001N0",
                        "sndprn",
                        "123085"),
                    eintrag(
                        "lieferantennummer",
                        "0101159903",
                        "werk",
                        "22",
                        "abladestelle",
                        "001N0",
                        "sndprn",
                        "123085"))))
        .isEqualTo("Aktion=Neuanlage\n0101159903//001N0,123085,\n0101159903/22/001N0,123085,");
  }

  @Test
  @DisplayName("Fuehrende fehlende Teile entfallen weiter samt Trenner, auch zwei hintereinander")
  void fuehrende_fehlende_teile_entfallen() {
    assertThat(
            Parametertext.baue(
                mitte(),
                "Löschung",
                List.of(
                    eintrag("abladestelle", "001N0"),
                    eintrag("werk", "22", "abladestelle", "001N0"))))
        .isEqualTo("Aktion=Löschung\n001N0\n22/001N0");
  }

  /**
   * Eine Variante, in der das Werk optional ist und die Abladestelle Pflicht: Nur so steht ein
   * optionaler Teil zwischen zwei anderen. Fachlich gibt es sie nicht.
   */
  private static TypDefinition mitte() {
    ObjectNode struktur = Testdefinitionen.werkBaum();
    ((ObjectNode) struktur.get("felder").get(1)).remove("pflicht");
    ((ObjectNode) struktur.get("felder").get(2)).put("pflicht", true);
    DefinitionsPruefung.Ergebnis mitte = DefinitionsPruefung.pruefe("MITTE", "Mitte", "", struktur);
    assertThat(mitte.gruende()).isEmpty();
    return mitte.definition();
  }

  @Test
  @DisplayName("Ersetzt wird in einem Durchgang: ein Wert wie ein Platzhalter bleibt stehen")
  void ein_durchgang() {
    assertThat(
            Parametertext.baue(
                GATTUNG,
                "Umbenennen",
                List.of(eintrag("konto", "5", "neuername", "Neu {konto} $1 \\"))))
        .isEqualTo("Aktion=Umbenennen\n5|Neu {konto} $1 \\");
  }
}
