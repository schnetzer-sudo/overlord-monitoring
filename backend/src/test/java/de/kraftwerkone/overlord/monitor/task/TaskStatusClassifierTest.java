package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Jede Zeile der Einordnungstabelle ({@code docs/tasks.md} §5.5). */
class TaskStatusClassifierTest {

  private static final TypDefinition.Rueckgaben WERK = Testdefinitionen.werk().rueckgaben();

  private static Optional<Rueckgabe> rueckgabe(String roh) {
    return Rueckgabe.lies(roh, WERK);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Ok\nOk", "Gesperrt"})
  @DisplayName("OFFEN bleibt OFFEN, gleich welche Rueckgabe")
  void offen(String roh) {
    assertThat(TaskStatusClassifier.einordnen("OFFEN", rueckgabe(roh), 2))
        .isEqualTo(Einordnung.OFFEN);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Ok\nOk", "Gesperrt"})
  @DisplayName("IN_AUSFUEHRUNG bleibt IN_AUSFUEHRUNG, gleich welche Rueckgabe")
  void in_ausfuehrung(String roh) {
    assertThat(TaskStatusClassifier.einordnen("IN_AUSFUEHRUNG", rueckgabe(roh), 2))
        .isEqualTo(Einordnung.IN_AUSFUEHRUNG);
  }

  @Test
  @DisplayName("ERLEDIGT, Anzahl passt, alle OK: ERLEDIGT")
  void erledigt_alle_ok() {
    assertThat(TaskStatusClassifier.einordnen("ERLEDIGT", rueckgabe("Ok\r\nOk\n"), 2))
        .isEqualTo(Einordnung.ERLEDIGT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Ok\nSchlüssel nicht gefunden",
        "Ok\nirgendwas",
        "Schlüssel bereits vorhanden\nNEIN"
      })
  @DisplayName("ERLEDIGT, Anzahl passt, mindestens einer nicht OK: TEILWEISE_ERLEDIGT")
  void erledigt_teilweise(String roh) {
    assertThat(TaskStatusClassifier.einordnen("ERLEDIGT", rueckgabe(roh), 2))
        .isEqualTo(Einordnung.TEILWEISE_ERLEDIGT);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "Ok", "Ok\nOk\nOk"})
  @DisplayName("ERLEDIGT, Rueckgabe fehlt oder falsche Zeilenzahl: UNSTIMMIG")
  void erledigt_unstimmig(String roh) {
    assertThat(TaskStatusClassifier.einordnen("ERLEDIGT", rueckgabe(roh), 2))
        .isEqualTo(Einordnung.UNSTIMMIG);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Ok\nOk"})
  @DisplayName("FEHLGESCHLAGEN bleibt FEHLGESCHLAGEN, gleich welche Rueckgabe")
  void fehlgeschlagen(String roh) {
    assertThat(TaskStatusClassifier.einordnen("FEHLGESCHLAGEN", rueckgabe(roh), 2))
        .isEqualTo(Einordnung.FEHLGESCHLAGEN);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"TEILWEISE_ERLEDIGT", "erledigt", "ABGEBROCHEN", ""})
  @DisplayName(
      "Jeder andere Status: UNBEKANNT — auch TEILWEISE_ERLEDIGT, das die Plattform nie setzt")
  void anderer_wert(String status) {
    assertThat(TaskStatusClassifier.einordnen(status, rueckgabe("Ok\nOk"), 2))
        .isEqualTo(Einordnung.UNBEKANNT);
  }

  @Test
  @DisplayName("Laufend heissen genau OFFEN und IN_AUSFUEHRUNG")
  void laufend() {
    assertThat(TaskStatusClassifier.LAUFEND).containsExactly("OFFEN", "IN_AUSFUEHRUNG");
  }
}
