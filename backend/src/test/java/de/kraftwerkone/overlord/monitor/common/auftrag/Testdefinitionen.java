package de.kraftwerkone.overlord.monitor.common.auftrag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Die Definitionen, gegen die die Tests ohne Datenbank laufen, und die Eintraege der Beispiele aus
 * dem Auftrag zu A1. Oeffentlich, weil {@code task} und {@code assistent} sie ebenfalls nutzen.
 *
 * <p><b>{@code WERK_ABLADESTELLE}</b> kommt aus {@code src/test/resources/task}, im Stand nach der
 * Migration {@code V102} (hoechstens 20 Eintraege seit {@code V101}, Beschreibung mit der
 * Rueckfrage zur Anpassung und dem Absatz zu „neu oder aendern“); {@code TaskMigrationDbIT} haelt
 * die Gleichheit mit der Datenbank.
 *
 * <p><b>Die Gattung</b> besteht nur hier: andere Aktionen, andere Felder, ein anderer Trenner, eine
 * Regel {@code eindeutig} ueber ein Feld statt ueber den Schluessel, andere Rueckgabewerte.
 * Arbeiten Pruefung und Parametertext fuer sie richtig, ohne dass im Hauptcode etwas fuer sie
 * steht, ist die Bauform gemeint und nicht der erste Typ ({@code docs/tasks.md} E‑1018).
 */
public final class Testdefinitionen {

  public static final JsonMapper JSON = JsonMapper.builder().build();

  public static final String WERK = "WERK_ABLADESTELLE";
  public static final String WERK_NAME = "Anpassen der Werk-/Abladestellentabelle";

  public static final String GATTUNG_STRUKTUR =
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

  /**
   * Die Struktur zeichengleich so, wie sie in {@code task_type} der Testkopie steht: seit {@code
   * V101} ({@code JSON_SET}) in der kompakten Form von MariaDB, ohne das Zeilenende der Datei.
   * {@code TestsatzDefinitionDbIT} haelt das fest.
   */
  public static String werkStruktur() {
    String text = ressource("/task/werk-abladestelle-struktur.json");
    return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
  }

  /** Die Beschreibung aus Anhang B, ohne das Zeilenende, das der Editor an die Datei haengt. */
  public static String werkBeschreibung() {
    String text = ressource("/task/werk-abladestelle-beschreibung.txt");
    return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
  }

  /** Eine frische, veraenderbare Kopie der Struktur von {@code WERK_ABLADESTELLE}. */
  public static ObjectNode werkBaum() {
    return (ObjectNode) JSON.readTree(werkStruktur());
  }

  public static ObjectNode gattungBaum() {
    return (ObjectNode) JSON.readTree(GATTUNG_STRUKTUR);
  }

  public static TypDefinition werk() {
    return gueltig(WERK, WERK_NAME, werkBaum());
  }

  public static TypDefinition gattung() {
    return gueltig("GATTUNG", "Gattung", gattungBaum());
  }

  public static DefinitionsPruefung.Ergebnis pruefe(ObjectNode struktur) {
    return DefinitionsPruefung.pruefe("PROBE", "Probe", "Beschreibung", struktur);
  }

  // ─── Eintraege ─────────────────────────────────────────────────────────────────────────────

  /** Ein veraenderbarer Eintrag aus Paaren Feldname, Wert; {@code null}-Werte erlaubt. */
  public static Map<String, String> eintrag(String... paare) {
    Map<String, String> eintrag = new HashMap<>();
    for (int i = 0; i < paare.length; i += 2) {
      eintrag.put(paare[i], paare[i + 1]);
    }
    return eintrag;
  }

  public static Map<String, String> schluessel(String werk, String abladestelle) {
    return eintrag("werk", werk, "abladestelle", abladestelle);
  }

  /** Die Eingaben zum Beispiel „Neuanlage“: zweimal ohne SNDPRN_4908, einmal mit Lieferant. */
  public static List<Map<String, String>> neuanlage() {
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

  public static List<Map<String, String>> loeschung() {
    Map<String, String> mitLieferant = schluessel("22", "001N0");
    mitLieferant.put("lieferantennummer", "0101159903");
    return List.of(schluessel("010", "18VZE"), mitLieferant);
  }

  /** Im zweiten Eintrag ist SNDPRN_4908 leer: Er wird entfernt (E‑1007). */
  public static List<Map<String, String>> anpassung() {
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
