package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Der Vertrag fuer die Plattform steht an zwei Stellen — in {@code docs/task-schnittstelle.md} fuer
 * das Plattform-Team und in {@link Plattformvertrag} fuer die Tests, die ihn ausfuehren. Dieser
 * Test haelt beide zeichengleich: Was die Plattform liest, ist das, was {@code AuftraegeDbIT}
 * belegt.
 */
class PlattformvertragTest {

  private static final Path VERTRAG = Path.of("..", "docs", "task-schnittstelle.md");

  @Test
  @DisplayName("Die vier Statements der Datei sind woertlich die des Tests")
  void datei_und_test_stimmen_ueberein() throws IOException {
    String datei = Files.readString(VERTRAG, StandardCharsets.UTF_8);
    int anfang = datei.indexOf("```sql\n");
    assertThat(anfang).as("kein ```sql-Block in %s", VERTRAG).isNotNegative();
    int ende = datei.indexOf("\n```", anfang);
    String block = datei.substring(anfang + "```sql\n".length(), ende);

    List<String> statements =
        Arrays.stream(block.split(";")).map(String::strip).filter(s -> !s.isEmpty()).toList();

    assertThat(statements).containsExactlyElementsOf(Plattformvertrag.ALLE);
    assertThat(block.strip()).endsWith(";");
  }
}
