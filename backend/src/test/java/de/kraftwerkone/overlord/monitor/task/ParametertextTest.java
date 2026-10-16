package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.anpassung;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.eintrag;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.loeschung;
import static de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen.neuanlage;
import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
