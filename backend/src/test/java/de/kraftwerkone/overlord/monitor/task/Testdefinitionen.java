package de.kraftwerkone.overlord.monitor.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Die Definitionen, gegen die die Tests ohne Datenbank laufen.
 *
 * <p><b>{@code WERK_ABLADESTELLE}</b> kommt aus {@code src/test/resources/task}, zeichengleich mit
 * dem Auftrag zu A1 und mit der Migration {@code V100}; {@code TaskMigrationDbIT} haelt die
 * Gleichheit mit der Datenbank.
 *
 * <p><b>Die Gattung</b> besteht nur hier: andere Aktionen, andere Felder, ein anderer Trenner, eine
 * Regel {@code eindeutig} ueber ein Feld statt ueber den Schluessel, andere Rueckgabewerte.
 * Arbeiten Pruefung und Parametertext fuer sie richtig, ohne dass im Hauptcode etwas fuer sie
 * steht, ist die Bauform gemeint und nicht der erste Typ ({@code docs/tasks.md} E‑1018).
 */
final class Testdefinitionen {

  static final JsonMapper JSON = JsonMapper.builder().build();

  static final String WERK = "WERK_ABLADESTELLE";
  static final String WERK_NAME = "Anpassen der Werk-/Abladestellentabelle";

  static final String GATTUNG_STRUKTUR =
      """
      {
        "aktionen": ["Sperren", "Freigeben", "Umbenennen"],
        "maxEintraege": 3,
        "felder": [
          { "name": "bereich", "label": "Bereich", "muster": "^[A-Z]{2}$" },
          { "name": "konto", "label": "Konto", "pflicht": true, "muster": "^[0-9]{1,8}$" },
          { "name": "grund", "label": "Grund", "nurBei": ["Sperren"] },
          { "name": "neuername", "label": "Neuer Name", "pflicht": true, "nurBei": ["Umbenennen"] }
        ],
        "schluessel": { "felder": ["bereich", "konto"], "trenner": "-" },
        "regeln": [
          { "art": "eindeutig", "felder": ["konto"] },
          { "art": "mindestensEins", "felder": ["grund"], "bei": ["Sperren"] }
        ],
        "zeile": {
          "Sperren": "{schluessel};{grund}",
          "Freigeben": "{konto}",
          "Umbenennen": "{schluessel}|{neuername}"
        },
        "rueckgaben": { "ok": "JA", "weitere": ["NEIN", "SPAETER"] }
      }
      """;

  private Testdefinitionen() {}

  static String werkStruktur() {
    return ressource("/task/werk-abladestelle-struktur.json");
  }

  /** Die Beschreibung aus Anhang B, ohne das Zeilenende, das der Editor an die Datei haengt. */
  static String werkBeschreibung() {
    String text = ressource("/task/werk-abladestelle-beschreibung.txt");
    return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
  }

  /** Eine frische, veraenderbare Kopie der Struktur von {@code WERK_ABLADESTELLE}. */
  static ObjectNode werkBaum() {
    return (ObjectNode) JSON.readTree(werkStruktur());
  }

  static ObjectNode gattungBaum() {
    return (ObjectNode) JSON.readTree(GATTUNG_STRUKTUR);
  }

  static TypDefinition werk() {
    return gueltig(WERK, WERK_NAME, werkBaum());
  }

  static TypDefinition gattung() {
    return gueltig("GATTUNG", "Gattung", gattungBaum());
  }

  static DefinitionsPruefung.Ergebnis pruefe(ObjectNode struktur) {
    return DefinitionsPruefung.pruefe("PROBE", "Probe", "Beschreibung", struktur);
  }

  private static TypDefinition gueltig(String typ, String name, ObjectNode struktur) {
    DefinitionsPruefung.Ergebnis ergebnis =
        DefinitionsPruefung.pruefe(typ, name, "Beschreibung", struktur);
    assertThat(ergebnis.gruende()).as("Testdefinition %s", typ).isEmpty();
    return ergebnis.definition();
  }

  private static String ressource(String pfad) {
    try (InputStream quelle = Testdefinitionen.class.getResourceAsStream(pfad)) {
      if (quelle == null) {
        throw new IllegalStateException(pfad + " liegt nicht im Testklassenpfad");
      }
      return new String(quelle.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException fehler) {
      throw new UncheckedIOException(fehler);
    }
  }
}
