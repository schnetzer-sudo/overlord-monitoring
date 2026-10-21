package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import de.kraftwerkone.overlord.monitor.common.auftrag.TypDefinition;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Das Lesen der Rueckgabe ({@code docs/tasks.md} §5.4). */
class RueckgabeTest {

  private static final TypDefinition.Rueckgaben WERK = Testdefinitionen.werk().rueckgaben();

  @Test
  @DisplayName("Das Beispiel der Quelle: drei Zeilen, die mittlere nicht Ok")
  void beispiel_der_quelle() {
    Rueckgabe rueckgabe = Rueckgabe.lies("Ok\nSchlüssel nicht gefunden\nOk", WERK).orElseThrow();
    assertThat(rueckgabe.zeilen())
        .containsExactly(
            new Rueckgabe.Zeile("Ok", Zeilenergebnis.OK),
            new Rueckgabe.Zeile("Schlüssel nicht gefunden", Zeilenergebnis.NICHT_OK),
            new Rueckgabe.Zeile("Ok", Zeilenergebnis.OK));
    assertThat(rueckgabe.alleOk()).isFalse();
  }

  @Test
  @DisplayName("CRLF: das CR je Zeile faellt weg, das Ergebnis ist dasselbe")
  void crlf() {
    Rueckgabe rueckgabe =
        Rueckgabe.lies("Ok\r\nSchlüssel bereits vorhanden\r\n", WERK).orElseThrow();
    assertThat(rueckgabe.zeilen())
        .containsExactly(
            new Rueckgabe.Zeile("Ok", Zeilenergebnis.OK),
            new Rueckgabe.Zeile("Schlüssel bereits vorhanden", Zeilenergebnis.NICHT_OK));
  }

  @Test
  @DisplayName("Leerraum am Rand faellt weg; ein abschliessendes LF ergibt keine leere Zeile")
  void leerraum_und_abschliessendes_lf() {
    Rueckgabe rueckgabe = Rueckgabe.lies("  Ok \n\tOk\n", WERK).orElseThrow();
    assertThat(rueckgabe.zeilen()).hasSize(2).allMatch(z -> z.ergebnis() == Zeilenergebnis.OK);
    assertThat(rueckgabe.alleOk()).isTrue();
  }

  @Test
  @DisplayName("Ein unbekannter Wert heisst UNBEKANNT, der Rohwert bleibt stehen")
  void unbekannter_wert() {
    Rueckgabe rueckgabe = Rueckgabe.lies("Ok\nOK\nGesperrt", WERK).orElseThrow();
    assertThat(rueckgabe.zeilen())
        .containsExactly(
            new Rueckgabe.Zeile("Ok", Zeilenergebnis.OK),
            new Rueckgabe.Zeile("OK", Zeilenergebnis.UNBEKANNT),
            new Rueckgabe.Zeile("Gesperrt", Zeilenergebnis.UNBEKANNT));
  }

  @Test
  @DisplayName("Eine leere Zeile mitten in der Rueckgabe zaehlt als Zeile")
  void leere_zeile_zaehlt() {
    assertThat(Rueckgabe.lies("Ok\n\nOk", WERK).orElseThrow().zeilen()).hasSize(3);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "  ", "\n", "\r\n"})
  @DisplayName("Fehlt die Rueckgabe oder ist sie nur Leerraum, gibt es keine")
  void fehlend(String roh) {
    assertThat(Rueckgabe.lies(roh, WERK)).isEmpty();
  }

  @Test
  @DisplayName("Ohne lesbare Rueckgabewerte in der Kopie ist jede Zeile UNBEKANNT")
  void ohne_rueckgabewerte() {
    Optional<Rueckgabe> rueckgabe = Rueckgabe.lies("Ok", null);
    assertThat(rueckgabe.orElseThrow().zeilen())
        .containsExactly(new Rueckgabe.Zeile("Ok", Zeilenergebnis.UNBEKANNT));
  }

  @Test
  @DisplayName("Gattung: eigene Rueckgabewerte")
  void gattung() {
    Rueckgabe rueckgabe =
        Rueckgabe.lies("JA\nNEIN\nSPAETER", Testdefinitionen.gattung().rueckgaben()).orElseThrow();
    assertThat(rueckgabe.zeilen())
        .extracting(Rueckgabe.Zeile::ergebnis)
        .containsExactly(Zeilenergebnis.OK, Zeilenergebnis.NICHT_OK, Zeilenergebnis.NICHT_OK);
  }
}
