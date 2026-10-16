package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.kraftwerkone.overlord.monitor.common.auftrag.Testdefinitionen;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Im Hauptcode kennt keine Stelle einen bestimmten Typ</b> (E‑1018). Der erste Typ steht nur in
 * den Migrationen, die ihn anlegen ({@code V100}) und aendern ({@code V101}, {@code V102}), und in
 * Tests.
 *
 * <p>Durchsucht wird {@code src/main} ganz — Java, generierter Code, Ressourcen —, ausser {@code
 * src/main/resources/db/migration}. Damit der Test nicht blind gruen ist, verlangt er, dass er die
 * Klassen des Pakets {@code task} tatsaechlich gelesen und die Kennung in der Migration gefunden
 * hat.
 */
class TypUnabhaengigkeitTest {

  private static final byte[] KENNUNG = Testdefinitionen.WERK.getBytes(StandardCharsets.US_ASCII);

  private static final Path HAUPTCODE = Path.of("src", "main");
  private static final Path MIGRATIONEN =
      HAUPTCODE.resolve(Path.of("resources", "db", "migration"));

  @Test
  @DisplayName("WERK_ABLADESTELLE steht im Hauptcode nirgends ausser in der Migration")
  void kein_typ_im_hauptcode() throws IOException {
    List<Path> gelesen = new ArrayList<>();
    List<Path> treffer = new ArrayList<>();
    try (Stream<Path> dateien = Files.walk(HAUPTCODE)) {
      for (Path datei : dateien.filter(Files::isRegularFile).toList()) {
        if (datei.startsWith(MIGRATIONEN)) {
          continue;
        }
        gelesen.add(datei);
        if (enthaelt(Files.readAllBytes(datei), KENNUNG)) {
          treffer.add(datei);
        }
      }
    }

    assertThat(gelesen)
        .as("Der Test muss den Hauptcode gelesen haben, sonst prueft er nichts")
        .contains(
            HAUPTCODE.resolve(
                Path.of(
                    "java",
                    "de",
                    "kraftwerkone",
                    "overlord",
                    "monitor",
                    "task",
                    "AuftragService.java")));
    assertThat(treffer)
        .as(
            "Ein Auftragstyp ist eine Zeile in task_type, kein Code (E-1018). Was ein Typ"
                + " verlangt, gehoert in seine Definition.")
        .isEmpty();
  }

  @Test
  @DisplayName("Gegenprobe: Die Migration nennt den Typ, die Suche findet ihn also")
  void migration_nennt_den_typ() throws IOException {
    try (Stream<Path> dateien = Files.list(MIGRATIONEN)) {
      assertThat(
              dateien.filter(
                  datei -> {
                    try {
                      return enthaelt(Files.readAllBytes(datei), KENNUNG);
                    } catch (IOException fehler) {
                      throw new java.io.UncheckedIOException(fehler);
                    }
                  }))
          .extracting(datei -> datei.getFileName().toString())
          .containsExactlyInAnyOrder(
              "V100__task.sql",
              "V101__werk_abladestelle_20_eintraege.sql",
              "V102__werk_abladestelle_neu_oder_aendern.sql");
    }
  }

  private static boolean enthaelt(byte[] text, byte[] gesucht) {
    outer:
    for (int i = 0; i <= text.length - gesucht.length; i++) {
      for (int j = 0; j < gesucht.length; j++) {
        if (text[i + j] != gesucht[j]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }
}
