package de.kraftwerkone.overlord.monitor.task;

import static de.kraftwerkone.overlord.monitor.task.AuftragsPruefungTest.eintrag;
import static de.kraftwerkone.overlord.monitor.task.AuftragsPruefungTest.schluessel;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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

  /** Die Eingaben zum Beispiel „Neuanlage“: zweimal ohne SNDPRN_4908, einmal mit Lieferant. */
  static List<Map<String, String>> neuanlage() {
    return List.of(
        eintrag("werk", "72332", "abladestelle", "DDB0", "sndprn", "120764"),
        eintrag("werk", "72261", "abladestelle", "DDR0", "sndprn", "120764"),
        eintrag("werk", "010", "abladestelle", "18VZE", "sndprn", "120055", "sndprn4908", "100050"),
        eintrag(
            "lieferantennummer",
            "0101159903",
            "werk",
            "22",
            "abladestelle",
            "001N0",
            "sndprn",
            "123085"));
  }

  static List<Map<String, String>> loeschung() {
    Map<String, String> mitLieferant = schluessel("22", "001N0");
    mitLieferant.put("lieferantennummer", "0101159903");
    return List.of(schluessel("010", "18VZE"), mitLieferant);
  }

  /** Im zweiten Eintrag ist SNDPRN_4908 leer: Er wird entfernt (E‑1007). */
  static List<Map<String, String>> anpassung() {
    return List.of(
        eintrag("werk", "010", "abladestelle", "18VZE", "sndprn", "123456", "sndprn4908", "123456"),
        eintrag(
            "lieferantennummer",
            "0101159903",
            "werk",
            "22",
            "abladestelle",
            "001N0",
            "sndprn",
            "234567",
            "sndprn4908",
            ""));
  }

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
